/*
 * Copyright 2018 The Data-Portability Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.gateway.reference;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import com.google.common.net.HttpHeaders;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.HttpCookie;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.dataportabilityproject.gateway.ApiSettings;
import org.dataportabilityproject.gateway.reference.ReferenceApiUtils.HttpMethods;
import org.dataportabilityproject.security.EncrypterFactory;
import org.dataportabilityproject.security.SymmetricKeyGenerator;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.JobAuthorization;
import org.dataportabilityproject.spi.cloud.types.PortabilityJob;
import org.dataportabilityproject.api.launcher.TypeManager;
import org.dataportabilityproject.spi.gateway.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProviderRegistry;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProviderRegistry.AuthMode;
import org.dataportabilityproject.spi.gateway.types.AuthFlowConfiguration;
import org.dataportabilityproject.types.client.transfer.DataTransferResponse;
import org.dataportabilityproject.types.client.transfer.DataTransferResponse.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common logic for job setup handlers. This handler is meant to retrieve the current status via a
 * DataTransferResponse Directs the frontend to: - The destination services authorization page (in
 * case of IMPORT mode) - The startCopy page (in case of COPY mode)
 */
abstract class SetupHandler implements HttpHandler {

  private static final Logger logger = LoggerFactory.getLogger(SetupHandler.class);
  private final ObjectMapper objectMapper;
  private final JobStore store;
  private final SymmetricKeyGenerator symmetricKeyGenerator;
  private final AuthServiceProviderRegistry registry;
  private final ApiSettings apiSettings;
  private final Mode mode;
  private final String handlerUrlPath;
  private final TokenManager tokenManager;

  protected SetupHandler(
      AuthServiceProviderRegistry registry,
      ApiSettings apiSettings,
      JobStore store,
      SymmetricKeyGenerator symmetricKeyGenerator,
      Mode mode,
      String handlerUrlPath,
      TokenManager tokenManager,
      TypeManager typeManager) {
    this.store = store;
    this.symmetricKeyGenerator = symmetricKeyGenerator;
    this.registry = registry;
    this.apiSettings = apiSettings;
    this.mode = mode;
    this.handlerUrlPath = handlerUrlPath;
    this.objectMapper = typeManager.getMapper();
    this.tokenManager = tokenManager;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    try {
      logger.debug("Entering setup handler, exchange: {}", exchange);
      Preconditions.checkArgument(
          ReferenceApiUtils.validateRequest(exchange, HttpMethods.GET, handlerUrlPath));

      String encodedIdCookie =
          ReferenceApiUtils.getCookie(exchange.getRequestHeaders(), JsonKeys.ID_COOKIE_KEY);
      Preconditions.checkArgument(
          !Strings.isNullOrEmpty(encodedIdCookie), "Encoded Id cookie required");

      // Valid job must be present
      UUID jobId = ReferenceApiUtils.decodeJobId(encodedIdCookie);
      PortabilityJob job = store.findJob(jobId);
      Preconditions.checkNotNull(job, "existing job not found for jobId: %s", jobId);

      // This page is only valid after the oauth of the export service - export data should exist
      // for all setup Modes.
      String exportService = job.exportService();
      Preconditions.checkState(!Strings.isNullOrEmpty(exportService), "Export service is invalid");

      String importService = job.importService();
      Preconditions.checkState(!Strings.isNullOrEmpty(importService), "Import service is invalid");

      DataTransferResponse response;
      if (mode == Mode.IMPORT) {
        response = handleImportSetup(exchange.getRequestHeaders(), job, jobId);
      } else {
        response = handleCopySetup(exchange.getRequestHeaders(), job, jobId);
        // Valid job is present, generate an XSRF token to pass back via cookie
        String tokenStr = tokenManager.createNewToken(jobId);
        HttpCookie token = new HttpCookie(JsonKeys.XSRF_TOKEN, tokenStr);
        exchange.getResponseHeaders().add(HttpHeaders.SET_COOKIE, token.toString() + ReferenceApiUtils.COOKIE_ATTRIBUTES);
      }

      // Mark the response as type Json and send
      exchange
          .getResponseHeaders()
          .set(CONTENT_TYPE, "application/json; charset=" + StandardCharsets.UTF_8.name());
      exchange.sendResponseHeaders(200, 0);

      objectMapper.writeValue(exchange.getResponseBody(), response);
    } catch (Exception e) {
      logger.error("Error handling request", e);
      throw e;
    }
  }

  private DataTransferResponse handleImportSetup(
      Headers headers, PortabilityJob job, UUID jobId) throws IOException {
    String exportAuthCookie =
        ReferenceApiUtils.getCookie(headers, JsonKeys.EXPORT_AUTH_DATA_COOKIE_KEY);
      Preconditions.checkArgument(
          !Strings.isNullOrEmpty(exportAuthCookie), "Export auth cookie required");

    // Initial auth flow url
    AuthDataGenerator generator =
        registry.getAuthDataGenerator(
            job.importService(), job.transferDataType(), AuthMode.IMPORT);
    Preconditions.checkNotNull(
        generator,
        "Generator not found for type: %s, service: %s",
        job.transferDataType(),
        job.importService());

    String encodedJobId = ReferenceApiUtils.encodeJobId(jobId);
    AuthFlowConfiguration authFlowConfiguration =
        generator.generateConfiguration(apiSettings.getBaseApiUrl(), encodedJobId);
    Preconditions.checkNotNull(
        authFlowConfiguration,
        "AuthFlowConfiguration not found for type: %s, service: %s",
        job.transferDataType(),
        job.importService());

    // If present, store initial auth data for export services, e.g. used for oauth1
    if (authFlowConfiguration.getInitialAuthData() != null) {

      // Retrieve and parse the session key from the job
      String sessionKey = job.jobAuthorization().encodedSessionKey();
      SecretKey key = symmetricKeyGenerator.parse(BaseEncoding.base64Url().decode(sessionKey));

      // Ensure intial auth data for import has not already been set
      Preconditions.checkState(
          Strings.isNullOrEmpty(job.jobAuthorization().encryptedInitialImportAuthData()));

      // Serialize and encrypt the initial auth data
      String serialized =
          objectMapper.writeValueAsString(authFlowConfiguration.getInitialAuthData());
      String encryptedInitialAuthData = EncrypterFactory.create(key).encrypt(serialized);

      // Add the serialized and encrypted initial auth data to the job authorization
      JobAuthorization updatedJobAuthorization =
          job.jobAuthorization()
              .toBuilder()
              .setEncryptedInitialImportAuthData(encryptedInitialAuthData)
              .build();

      // Persist the updated PortabilityJob with the updated JobAuthorization
      PortabilityJob updatedPortabilityJob =
          job.toBuilder().setAndValidateJobAuthorization(updatedJobAuthorization).build();

      store.updateJob(jobId, updatedPortabilityJob);
    }

    return new DataTransferResponse(
        job.exportService(),
        job.importService(),
        job.transferDataType(),
        Status.INPROCESS,
        authFlowConfiguration.getUrl()); // Redirect to auth page of import service
  }

  private DataTransferResponse handleCopySetup(
      Headers requestHeaders, PortabilityJob job, UUID jobId) {
    // Make sure the data exists in the cookies before rendering copy page
      String exportAuthCookie =
          ReferenceApiUtils.getCookie(requestHeaders, JsonKeys.EXPORT_AUTH_DATA_COOKIE_KEY);
      Preconditions.checkArgument(
          !Strings.isNullOrEmpty(exportAuthCookie), "Export auth cookie required");

      String importAuthCookie =
          ReferenceApiUtils.getCookie(requestHeaders, JsonKeys.IMPORT_AUTH_DATA_COOKIE_KEY);
      Preconditions.checkArgument(
          !Strings.isNullOrEmpty(importAuthCookie), "Import auth cookie required");


    return new DataTransferResponse(
        job.exportService(),
        job.importService(),
        job.transferDataType(),
        Status.INPROCESS,
        StartCopyHandler.PATH); // frontend  should redirect to startCopy handler
  }

  // Which Setup flow to configure.
  // IMPORT mode sets up the import authorization flow.
  // COPY mode sets up the copy setup flow.
  public enum Mode {
    IMPORT,
    COPY
  }
}
