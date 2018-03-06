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
import com.google.inject.Inject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.HttpCookie;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.dataportabilityproject.gateway.ApiSettings;
import org.dataportabilityproject.gateway.action.createjob.CreateJobAction;
import org.dataportabilityproject.gateway.action.createjob.CreateJobActionRequest;
import org.dataportabilityproject.gateway.action.createjob.CreateJobActionResponse;
import org.dataportabilityproject.gateway.reference.ReferenceApiUtils.HttpMethods;
import org.dataportabilityproject.security.EncrypterFactory;
import org.dataportabilityproject.security.SymmetricKeyGenerator;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.JobAuthorization;
import org.dataportabilityproject.spi.cloud.types.PortabilityJob;
import org.dataportabilityproject.spi.cloud.types.TypeManager;
import org.dataportabilityproject.spi.gateway.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProviderRegistry;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProviderRegistry.AuthMode;
import org.dataportabilityproject.spi.gateway.types.AuthFlowConfiguration;
import org.dataportabilityproject.types.client.transfer.DataTransferRequest;
import org.dataportabilityproject.types.client.transfer.DataTransferResponse;
import org.dataportabilityproject.types.client.transfer.DataTransferResponse.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HttpHandler for the {@link CreateJobAction}. TODO: rename to CreateJobHandler as well as client
 * code as well
 */
final class DataTransferHandler implements HttpHandler {

  // TODO: rename to CreateJob as well as client code as well
  public static final String PATH = "/_/DataTransfer";
  public static final String ERROR_PATH = "/error";
  private static final Logger logger = LoggerFactory.getLogger(DataTransferHandler.class);
  private final CreateJobAction createJobAction;
  private final AuthServiceProviderRegistry registry;
  private final ApiSettings apiSettings;
  private final JobStore store;
  private final SymmetricKeyGenerator symmetricKeyGenerator;
  private final ObjectMapper objectMapper;

  @Inject
  DataTransferHandler(
      CreateJobAction createJobAction,
      AuthServiceProviderRegistry registry,
      ApiSettings apiSettings,
      JobStore store,
      TypeManager typeManager,
      SymmetricKeyGenerator symmetricKeyGenerator) {
    this.createJobAction = createJobAction;
    this.registry = registry;
    this.apiSettings = apiSettings;
    this.store = store;
    this.symmetricKeyGenerator = symmetricKeyGenerator;
    this.objectMapper = typeManager.getMapper();
  }

  /** Services the {@link CreateJobAction} via the {@link HttpExchange}. */
  @Override
  public void handle(HttpExchange exchange) throws IOException {
    Preconditions.checkArgument(
        ReferenceApiUtils.validateRequest(exchange, HttpMethods.POST, PATH),
        PATH + " only supports POST.");
    logger.debug("received request: {}", exchange.getRequestURI());
    DataTransferRequest request =
        objectMapper.readValue(exchange.getRequestBody(), DataTransferRequest.class);
    CreateJobActionRequest actionRequest =
        new CreateJobActionRequest(
            request.getSource(), request.getDestination(), request.getTransferDataType());
    CreateJobActionResponse actionResponse = createJobAction.handle(actionRequest);

    DataTransferResponse dataTransferResponse;
    if (actionResponse.getErrorMsg() != null) {
      logger.warn("Error during action: {}", actionResponse.getErrorMsg());
      handleError(exchange, request);
      return;
    }

    // Set new cookie
    String encodedJobId = ReferenceApiUtils.encodeId(actionResponse.getId());
    HttpCookie cookie = new HttpCookie(JsonKeys.ID_COOKIE_KEY, encodedJobId);
    exchange
        .getResponseHeaders()
        .add(HttpHeaders.SET_COOKIE, cookie.toString() + ReferenceApiUtils.COOKIE_ATTRIBUTES);

    // Initial auth flow url
    AuthDataGenerator generator =
        registry.getAuthDataGenerator(
            request.getSource(), request.getTransferDataType(), AuthMode.EXPORT);
    Preconditions.checkNotNull(
        generator,
        "Generator not found for type: %s, service: %s",
        request.getTransferDataType(),
        request.getSource());

    AuthFlowConfiguration authFlowConfiguration =
        generator.generateConfiguration(apiSettings.getBaseUrl(), encodedJobId);
    Preconditions.checkNotNull(
        authFlowConfiguration,
        "AuthFlowConfiguration not found for type: %s, service: %s",
        request.getTransferDataType(),
        request.getSource());

    PortabilityJob job = store.findJob(actionResponse.getId());

    // If present, store initial auth data for export services, e.g. used for oauth1
    if (authFlowConfiguration.getInitialAuthData() != null) {

      // Retrieve and parse the session key from the job
      String sessionKey = job.jobAuthorization().encryptedSessionKey();
      SecretKey key = symmetricKeyGenerator.parse(BaseEncoding.base64Url().decode(sessionKey));

      // Ensure intial auth data for export has not already been set
      Preconditions.checkState(
          Strings.isNullOrEmpty(job.jobAuthorization().encryptedInitialExportAuthData()));

      // Serialize and encrypt the initial auth data
      String serialized =
          objectMapper.writeValueAsString(authFlowConfiguration.getInitialAuthData());
      String encryptedInitialAuthData = EncrypterFactory.create(key).encrypt(serialized);

      // Add the serialized and encrypted initial auth data to the job authorization
      JobAuthorization updatedJobAuthorization =
          job.jobAuthorization()
              .toBuilder()
              .setEncryptedInitialExportAuthData(encryptedInitialAuthData)
              .build();

      // Persist the updated PortabilityJob with the updated JobAuthorization
      PortabilityJob updatedPortabilityJob =
          job.toBuilder().setAndValidateJobAuthorization(updatedJobAuthorization).build();

      store.updateJob(actionResponse.getId(), updatedPortabilityJob);
    }

    dataTransferResponse =
        new DataTransferResponse(
            request.getSource(),
            request.getDestination(),
            request.getTransferDataType(),
            Status.INPROCESS,
            authFlowConfiguration.getUrl());

    logger.debug("redirecting to: {}", authFlowConfiguration.getUrl());
    // Mark the response as type Json and send
    exchange
        .getResponseHeaders()
        .set(CONTENT_TYPE, "application/json; charset=" + StandardCharsets.UTF_8.name());
    exchange.sendResponseHeaders(200, 0);
    objectMapper.writeValue(exchange.getResponseBody(), dataTransferResponse);
  }

  /** Handles error response. TODO: Determine whether to return user facing error message here. */
  public void handleError(HttpExchange exchange, DataTransferRequest request) throws IOException {
    DataTransferResponse dataTransferResponse =
        new DataTransferResponse(
            request.getSource(),
            request.getDestination(),
            request.getTransferDataType(),
            Status.ERROR,
            ERROR_PATH);
    // Mark the response as type Json and send
    exchange
        .getResponseHeaders()
        .set(CONTENT_TYPE, "application/json; charset=" + StandardCharsets.UTF_8.name());
    exchange.sendResponseHeaders(200, 0);
    objectMapper.writeValue(exchange.getResponseBody(), dataTransferResponse);
  }
}
