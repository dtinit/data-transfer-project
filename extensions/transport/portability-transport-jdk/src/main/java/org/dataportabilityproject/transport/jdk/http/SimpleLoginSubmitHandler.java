/*
 * Copyright 2018 The Data Transfer Project Authors.
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
package org.dataportabilityproject.transport.jdk.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.http.HttpHeaders;
import org.dataportabilityproject.transport.jdk.http.HandlerUtils.FrontendConstantUrls;
import org.dataportabilityproject.transport.jdk.http.HandlerUtils.HttpMethods;
import org.dataportabilityproject.api.launcher.TypeManager;
import org.dataportabilityproject.security.EncrypterFactory;
import org.dataportabilityproject.security.SymmetricKeyGenerator;
import org.dataportabilityproject.spi.api.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.api.auth.AuthServiceProviderRegistry;
import org.dataportabilityproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.PortabilityJob;
import org.dataportabilityproject.types.client.transfer.CreateJobResponse;
import org.dataportabilityproject.types.client.transfer.CreateJobResponse.Status;
import org.dataportabilityproject.types.client.transfer.SimpleLoginRequest;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.dataportabilityproject.api.action.ActionUtils.decodeJobId;

/**
 * HttpHandler for SimpleLoginSubmit authorization flow. Redirects client request to: - the next
 * authorization (if this is after the source service auth) or - the copy page (if this is after the
 * destination service auth)
 */
final class SimpleLoginSubmitHandler implements HttpHandler {

  public static final String PATH = "/_/simpleLoginSubmit";
  private static final Logger logger = LoggerFactory.getLogger(SimpleLoginSubmitHandler.class);

  private final AuthServiceProviderRegistry registry;
  private final JobStore store;
  private final SymmetricKeyGenerator symmetricKeyGenerator;
  private final ObjectMapper objectMapper;
  private final String baseUrl;
  private final String baseApiUrl;

  @Inject
  SimpleLoginSubmitHandler(
      JobStore store,
      AuthServiceProviderRegistry registry,
      SymmetricKeyGenerator symmetricKeyGenerator,
      TypeManager typeManager,
      @Named("baseUrl") String baseUrl,
      @Named("baseApiUrl") String baseApiUrl) {
    this.registry = registry;
    this.store = store;
    this.symmetricKeyGenerator = symmetricKeyGenerator;
    this.objectMapper = typeManager.getMapper();
    this.baseUrl = baseUrl;
    this.baseApiUrl = baseApiUrl;
  }

  public void handle(HttpExchange exchange) throws IOException {
    Preconditions.checkArgument(HandlerUtils.validateRequest(exchange, HttpMethods.POST, PATH));
    logger.debug("received request: {}", exchange.getRequestURI());

    CreateJobResponse response = handleExchange(exchange);

    logger.debug("redirecting to: {}", response.getNextUrl());
    // Mark the response as type Json and send
    exchange
        .getResponseHeaders()
        .set(
            HttpHeaders.CONTENT_TYPE, "application/json; charset=" + StandardCharsets.UTF_8.name());
    exchange.sendResponseHeaders(200, 0);

    objectMapper.writeValue(exchange.getResponseBody(), response);
  }

  CreateJobResponse handleExchange(HttpExchange exchange) throws IOException {

    Headers requestHeaders = exchange.getRequestHeaders();
    try {
      SimpleLoginRequest request =
          objectMapper.readValue(exchange.getRequestBody(), SimpleLoginRequest.class);

      String encodedIdCookie = HandlerUtils.getCookie(requestHeaders, JsonKeys.ID_COOKIE_KEY);
      Preconditions.checkArgument(
          !Strings.isNullOrEmpty(encodedIdCookie), "Missing encodedIdCookie");
      // Valid job must be present
      Preconditions.checkArgument(
          !Strings.isNullOrEmpty(encodedIdCookie), "Encoded Id cookie required");
      UUID jobId = decodeJobId(encodedIdCookie);

      PortabilityJob job = store.findJob(jobId);
      Preconditions.checkNotNull(job, "existing job not found for jobId: %s", jobId);

      // TODO: Determine service from job or from authUrl path?
      AuthMode authMode = HandlerUtils.getAuthMode(exchange.getRequestHeaders());

      // TODO: Determine service from job or from authUrl path?
      String service = (authMode == AuthMode.EXPORT) ? job.exportService() : job.importService();
      Preconditions.checkState(
          !Strings.isNullOrEmpty(service),
          "service not found, service: %s authMode: %s, jobId: %s",
          service,
          authMode,
          jobId);

      Preconditions.checkArgument(
          !Strings.isNullOrEmpty(request.getUsername()), "Missing valid username");
      Preconditions.checkArgument(
          !Strings.isNullOrEmpty(request.getPassword()), "Missing password");

      AuthDataGenerator generator =
          registry.getAuthDataGenerator(service, job.transferDataType(), AuthMode.EXPORT);
      Preconditions.checkNotNull(
          generator,
          "Generator not found for type: %s, service: %s",
          job.transferDataType(),
          service);

      // TODO: change signature to pass UUID

      // Generate and store auth data
      AuthData authData =
          generator.generateAuthData(
              baseApiUrl, request.getUsername(), jobId.toString(), null, request.getPassword());
      Preconditions.checkNotNull(authData, "Auth data should not be null");

      // Obtain the session key for this job
      String encodedSessionKey = job.jobAuthorization().sessionSecretKey();
      SecretKey key =
          symmetricKeyGenerator.parse(BaseEncoding.base64Url().decode(encodedSessionKey));
      // Serialize and encrypt the auth data
      String serialized = objectMapper.writeValueAsString(authData);
      String encryptedAuthData = EncrypterFactory.create(key).encrypt(serialized);
      // Set new cookie
      HandlerUtils.setCookie(exchange.getResponseHeaders(), encryptedAuthData, authMode);

      return new CreateJobResponse(
          job.exportService(),
          job.importService(),
          job.transferDataType(),
          Status.INPROCESS,
          baseUrl
              + (authMode == AuthMode.EXPORT
                  ? FrontendConstantUrls.URL_NEXT_PAGE
                  : FrontendConstantUrls.URL_COPY_PAGE));

    } catch (Exception e) {
      logger.debug("Exception occurred while trying to handle request: {}", e);
      throw e;
    }
  }
}
