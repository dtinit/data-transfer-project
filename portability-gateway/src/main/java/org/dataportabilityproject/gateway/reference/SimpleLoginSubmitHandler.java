/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.gateway.reference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import com.google.inject.Inject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.apache.http.HttpHeaders;
import org.dataportabilityproject.gateway.ApiFlags;
import org.dataportabilityproject.gateway.crypto.EncrypterFactory;
import org.dataportabilityproject.gateway.crypto.SymmetricKeyGenerator;
import org.dataportabilityproject.gateway.reference.ReferenceApiUtils.FrontendConstantUrls;
import org.dataportabilityproject.gateway.reference.ReferenceApiUtils.HttpMethods;
import org.dataportabilityproject.gateway.reference.ReferenceApiUtils.TransferMode;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.PortabilityJob;
import org.dataportabilityproject.spi.cloud.types.TypeManager;
import org.dataportabilityproject.spi.gateway.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProvider;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProviderRegistry;
import org.dataportabilityproject.types.client.transfer.DataTransferResponse;
import org.dataportabilityproject.types.client.transfer.DataTransferResponse.Status;
import org.dataportabilityproject.types.client.transfer.SimpleLoginRequest;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HttpHandler for SimpleLoginSubmit authorization flow. Redirects client request to:
 *   - the next authorization (if this is after the source service auth) or
 *   - the copy page (if this is after the destination service auth)
 */
final class SimpleLoginSubmitHandler implements HttpHandler {

  public static final String PATH = "/_/simpleLoginSubmit";
  private static final Logger logger = LoggerFactory.getLogger(SimpleLoginSubmitHandler.class);

  private final AuthServiceProviderRegistry registry;
  private final JobStore store;
  private final SymmetricKeyGenerator symmetricKeyGenerator;
  private final ObjectMapper objectMapper;

  @Inject
  SimpleLoginSubmitHandler(
      JobStore store,
      AuthServiceProviderRegistry registry,
      SymmetricKeyGenerator symmetricKeyGenerator,
      TypeManager typeManager) {
    this.registry = registry;
    this.store = store;
    this.symmetricKeyGenerator = symmetricKeyGenerator;
    this.objectMapper = typeManager.getMapper();
  }

  public void handle(HttpExchange exchange) throws IOException {
    Preconditions.checkArgument(
        ReferenceApiUtils.validateRequest(exchange, HttpMethods.POST, PATH));
    logger.debug("received request: {}", exchange.getRequestURI());

    DataTransferResponse response = handleExchange(exchange);

    logger.debug("simpleLoginSubmit, redirecting to: {}", response.getNextUrl());
    // Mark the response as type Json and send
    exchange.getResponseHeaders()
        .set(HttpHeaders.CONTENT_TYPE, "application/json; charset=" + StandardCharsets.UTF_8.name());
    exchange.sendResponseHeaders(200, 0);

    objectMapper.writeValue(exchange.getResponseBody(), response);
  }

  DataTransferResponse handleExchange(HttpExchange exchange) throws IOException {
    DataTransferResponse response;

    Headers requestHeaders = exchange.getRequestHeaders();
    try {
      SimpleLoginRequest request = objectMapper
          .readValue(exchange.getRequestBody(), SimpleLoginRequest.class);

      String encodedIdCookie = ReferenceApiUtils
          .getCookie(requestHeaders, JsonKeys.ID_COOKIE_KEY);
      Preconditions
          .checkArgument(!Strings.isNullOrEmpty(encodedIdCookie), "Missing encodedIdCookie");
      // Valid job must be present
      Preconditions
          .checkArgument(!Strings.isNullOrEmpty(encodedIdCookie), "Encoded Id cookie required");
      String jobId = ReferenceApiUtils.decodeId(encodedIdCookie);

      PortabilityJob job = store.findJob(jobId);
      Preconditions.checkNotNull(job, "existing job not found for jobId: %s", jobId);

      // TODO: Determine service from job or from authUrl path?
      TransferMode transferMode = ReferenceApiUtils.getTransferMode(exchange.getRequestHeaders());

      // TODO: Determine service from job or from authUrl path?
      String service =
          (transferMode == TransferMode.EXPORT) ? job.getExportService() : job.getImportService();
      Preconditions.checkState(!Strings.isNullOrEmpty(service),
          "service not found, service: %s transferMode: %s, jobId: %s", service, transferMode,
          jobId);

      Preconditions
          .checkArgument(!Strings.isNullOrEmpty(request.getUsername()), "Missing valid username");
      Preconditions
          .checkArgument(!Strings.isNullOrEmpty(request.getPassword()), "Missing password");

      AuthServiceProvider provider = registry.getServiceProvider(service);
      Preconditions.checkNotNull(provider, "Provider not found for type: %s, service: %s",
          job.getTransferDataType(), service);

      AuthDataGenerator generator = provider.getAuthDataGenerator(job.getTransferDataType());
      Preconditions.checkNotNull(generator, "Generator not found for type: %s, service: %s",
          job.getTransferDataType(), service);

      // Generate and store auth data
      AuthData authData = generator
          .generateAuthData(ApiFlags.baseApiUrl(), request.getUsername(), jobId, null,
              request.getPassword());
      Preconditions.checkNotNull(authData, "Auth data should not be null");

      // Obtain the session key for this job
      String encodedSessionKey = job.getJobAuthorization().getEncodedSessionKey();
      SecretKey key = symmetricKeyGenerator
          .parse(BaseEncoding.base64Url().decode(encodedSessionKey));
      // Serialize and encrypt the auth data
      String serialized = objectMapper.writeValueAsString(authData);
      String encryptedAuthData = EncrypterFactory.create(key).encrypt(serialized);
      // Set new cookie
      ReferenceApiUtils.setCookie(exchange.getResponseHeaders(), encryptedAuthData, transferMode);

      response = new DataTransferResponse(job.getExportService(),
          job.getImportService(), job.getTransferDataType(), Status.INPROCESS,
          ApiFlags.baseUrl() + (transferMode == TransferMode.EXPORT
              ? FrontendConstantUrls.URL_NEXT_PAGE : FrontendConstantUrls.URL_COPY_PAGE));

    } catch (Exception e) {
      logger.debug("Exception occurred while trying to handle request: {}", e);
      throw e;
    }

    return response;
  }
}
