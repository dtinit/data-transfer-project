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
package org.dataportabilityproject.gateway.reference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import com.google.common.net.HttpHeaders;
import com.google.inject.Inject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.HttpCookie;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.dataportabilityproject.gateway.ApiSettings;
import org.dataportabilityproject.gateway.reference.ReferenceApiUtils.FrontendConstantUrls;
import org.dataportabilityproject.gateway.reference.ReferenceApiUtils.HttpMethods;
import org.dataportabilityproject.security.DecrypterFactory;
import org.dataportabilityproject.security.EncrypterFactory;
import org.dataportabilityproject.security.SymmetricKeyGenerator;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.PortabilityJob;
import org.dataportabilityproject.api.launcher.TypeManager;
import org.dataportabilityproject.spi.gateway.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProviderRegistry;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProviderRegistry.AuthMode;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HttpHandler for callbacks from Oauth2 authorization flow. Redirects client request to: - the next
 * authorization (if this is after the source service auth) or - the copy page (if this is after the
 * destination service auth)
 */
final class Oauth2CallbackHandler implements HttpHandler {

  public static final String PATH = "/callback/";
  // TODO: obtain from flags
  private static final boolean IS_LOCAL = true;
  private final Logger logger = LoggerFactory.getLogger(Oauth2CallbackHandler.class);
  private final AuthServiceProviderRegistry registry;
  private final JobStore store;
  private final SymmetricKeyGenerator symmetricKeyGenerator;
  private final ObjectMapper objectMapper;
  private final ApiSettings apiSettings;

  @Inject
  Oauth2CallbackHandler(
      JobStore store,
      AuthServiceProviderRegistry registry,
      SymmetricKeyGenerator symmetricKeyGenerator,
      TypeManager typeManager,
      ApiSettings apiSettings) {
    this.registry = registry;
    this.store = store;
    this.symmetricKeyGenerator = symmetricKeyGenerator;
    this.objectMapper = typeManager.getMapper();
    this.apiSettings = apiSettings;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    // Add .* to resource path as this path will be of the form /callback/SERVICEPROVIDER
    Preconditions.checkArgument(
        ReferenceApiUtils.validateRequest(exchange, HttpMethods.GET, PATH + ".*"));
    logger.debug("received request: {}", exchange.getRequestURI());

    String redirect = handleExchange(exchange);
    logger.debug("redirecting to {}", redirect);
    exchange.getResponseHeaders().set(HttpHeaders.LOCATION, redirect);
    exchange.sendResponseHeaders(303, -1);
  }

  private String handleExchange(HttpExchange exchange) throws IOException {
    String redirect = "/error";

    try {
      Headers requestHeaders = exchange.getRequestHeaders();

      String requestURL =
          ReferenceApiUtils.createURL(
              requestHeaders.getFirst(HttpHeaders.HOST),
              exchange.getRequestURI().toString(),
              IS_LOCAL);

      AuthorizationCodeResponseUrl authResponse = new AuthorizationCodeResponseUrl(requestURL);

      // check for user-denied error
      if (authResponse.getError() != null) {
        logger.warn("Authorization DENIED: {} Redirecting to /error", authResponse.getError());
        return redirect;
      }

      // retrieve cookie from exchange
      Map<String, HttpCookie> httpCookies = ReferenceApiUtils.getCookies(requestHeaders);
      HttpCookie encodedIdCookie = httpCookies.get(JsonKeys.ID_COOKIE_KEY);
      Preconditions.checkArgument(
          encodedIdCookie != null && !Strings.isNullOrEmpty(encodedIdCookie.getValue()),
          "Encoded Id cookie required");

      UUID jobId = ReferenceApiUtils.decodeJobId(encodedIdCookie.getValue());

      logger.debug("State token: {}", authResponse.getState());
      // TODO(#258): Check job ID in state token, was broken during local demo
      // UUID jobIdFromState = ReferenceApiUtils.decodeJobId(authResponse.getState());

      // // TODO: Remove sanity check
      // Preconditions.checkState(
      //     jobIdFromState.equals(jobId),
      //     "Job id in cookie [%s] and request [%s] should match",
      //     jobId,
      //     jobIdFromState);

      PortabilityJob job = store.findJob(jobId);
      Preconditions.checkNotNull(job, "existing job not found for jobId: %s", jobId);

      // TODO: Determine service from job or from authUrl path?
      AuthMode authMode = ReferenceApiUtils.getAuthMode(exchange.getRequestHeaders());
      String service = (authMode == AuthMode.EXPORT) ? job.exportService() : job.importService();
      Preconditions.checkState(
          !Strings.isNullOrEmpty(service),
          "service not found, service: %s authMode: %s, jobId: %s",
          service,
          authMode,
          jobId.toString());

      AuthDataGenerator generator =
          registry.getAuthDataGenerator(service, job.transferDataType(), authMode);
      Preconditions.checkNotNull(
          generator,
          "Generator not found for type: %s, service: %s",
          job.transferDataType(),
          service);

      // Obtain the session key for this job
      String encodedSessionKey = job.jobAuthorization().sessionSecretKey();
      SecretKey key =
          symmetricKeyGenerator.parse(BaseEncoding.base64Url().decode(encodedSessionKey));

      // Retrieve initial auth data, if it existed
      AuthData initialAuthData = null;
      String encryptedInitialAuthData =
          (authMode == AuthMode.EXPORT)
              ? job.jobAuthorization().encryptedInitialExportAuthData()
              : job.jobAuthorization().encryptedInitialImportAuthData();
      if (encryptedInitialAuthData != null) {
        // Retrieve and parse the session key from the job
        // Decrypt and deserialize the object
        String serialized = DecrypterFactory.create(key).decrypt(encryptedInitialAuthData);
        initialAuthData = objectMapper.readValue(serialized, AuthData.class);
      }

      // TODO: Use UUID instead of UUID.toString()
      // Generate auth data
      AuthData authData =
          generator.generateAuthData(
              apiSettings.getBaseApiUrl(),
              authResponse.getCode(),
              jobId.toString(),
              initialAuthData,
              null);
      Preconditions.checkNotNull(authData, "Auth data should not be null");

      // Serialize and encrypt the auth data
      String serialized = objectMapper.writeValueAsString(authData);
      String encryptedAuthData = EncrypterFactory.create(key).encrypt(serialized);
      // Set new cookie
      ReferenceApiUtils.setCookie(exchange.getResponseHeaders(), encryptedAuthData, authMode);

      redirect =
          apiSettings.getBaseUrl()
              + ((authMode == AuthMode.EXPORT)
                  ? FrontendConstantUrls.URL_NEXT_PAGE
                  : FrontendConstantUrls.URL_COPY_PAGE);
    } catch (Exception e) {
      logger.error("Error handling request: {}", e);
      throw e;
    }

    return redirect;
  }
}
