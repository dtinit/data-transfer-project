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
package org.datatransferproject.transport.jdk.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import com.google.common.net.HttpHeaders;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.datatransferproject.api.launcher.TypeManager;
import org.datatransferproject.security.DecrypterFactory;
import org.datatransferproject.security.EncrypterFactory;
import org.datatransferproject.security.SymmetricKeyGenerator;
import org.datatransferproject.spi.api.auth.AuthDataGenerator;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.HttpCookie;
import java.util.Map;
import java.util.UUID;

import static org.datatransferproject.api.action.ActionUtils.decodeJobId;

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
  private final String baseUrl;
  private final String baseApiUrl;

  @Inject
  Oauth2CallbackHandler(
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

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    // Add .* to resource path as this path will be of the form /callback/SERVICEPROVIDER
    Preconditions.checkArgument(
        HandlerUtils.validateRequest(exchange, HandlerUtils.HttpMethods.GET, PATH + ".*"));
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
          HandlerUtils.createURL(
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
      Map<String, HttpCookie> httpCookies = HandlerUtils.getCookies(requestHeaders);
      HttpCookie encodedIdCookie = httpCookies.get(JsonKeys.ID_COOKIE_KEY);
      Preconditions.checkArgument(
          encodedIdCookie != null && !Strings.isNullOrEmpty(encodedIdCookie.getValue()),
          "Encoded Id cookie required");

      UUID jobId = decodeJobId(encodedIdCookie.getValue());

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
      AuthMode authMode = HandlerUtils.getAuthMode(exchange.getRequestHeaders());
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
              baseApiUrl,
              authResponse.getCode(),
              jobId.toString(),
              initialAuthData,
              null);
      Preconditions.checkNotNull(authData, "Auth data should not be null");

      // Serialize and encrypt the auth data
      String serialized = objectMapper.writeValueAsString(authData);
      String encryptedAuthData = EncrypterFactory.create(key).encrypt(serialized);
      // Set new cookie
      HandlerUtils.setCookie(exchange.getResponseHeaders(), encryptedAuthData, authMode);

      redirect = baseUrl
              + ((authMode == AuthMode.EXPORT)
                  ? HandlerUtils.FrontendConstantUrls.URL_NEXT_PAGE
                  : HandlerUtils.FrontendConstantUrls.URL_COPY_PAGE);
    } catch (Exception e) {
      logger.error("Error handling request: {}", e);
      throw e;
    }

    return redirect;
  }
}
