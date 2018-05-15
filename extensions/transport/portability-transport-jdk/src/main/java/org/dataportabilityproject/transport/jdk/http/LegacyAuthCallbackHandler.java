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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import com.google.common.net.HttpHeaders;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.dataportabilityproject.api.launcher.TypeManager;
import org.dataportabilityproject.security.EncrypterFactory;
import org.dataportabilityproject.security.SymmetricKeyGenerator;
import org.dataportabilityproject.spi.api.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.api.auth.AuthServiceProviderRegistry;
import org.dataportabilityproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.PortabilityJob;
import org.dataportabilityproject.transport.jdk.http.ReferenceApiUtils.FrontendConstantUrls;
import org.dataportabilityproject.transport.jdk.http.ReferenceApiUtils.HttpMethods;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/* Auth Callback Handler for legacy "frob" auth methods - for an example see RememberTheMilk */
final class LegacyAuthCallbackHandler implements HttpHandler {
  public static final String PATH = "/legacyauthcallback/";

  // Convention for token query param in request.
  private static final String FROB = "frob";

  private final Logger logger = LoggerFactory.getLogger(LegacyAuthCallbackHandler.class);
  private final JobStore jobStore;
  private final AuthServiceProviderRegistry registry;
  private final ObjectMapper objectMapper;
  private final SymmetricKeyGenerator symmetricKeyGenerator;
  private final String baseUrl;

  @Inject
  LegacyAuthCallbackHandler(
      JobStore jobStore,
      AuthServiceProviderRegistry registry,
      TypeManager typeManager,
      SymmetricKeyGenerator symmetricKeyGenerator,
      @Named("baseUrl") String baseUrl) {
    this.jobStore = jobStore;
    this.registry = registry;
    this.symmetricKeyGenerator = symmetricKeyGenerator;
    this.objectMapper = typeManager.getMapper();
    this.baseUrl = baseUrl;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    // Add .* to resource path as this path will be of the form /authcallback/SERVICEPROVIDER
    Preconditions.checkArgument(
        ReferenceApiUtils.validateRequest(exchange, HttpMethods.GET, PATH + ".*"));
    logger.debug("received request: {}", exchange.getRequestURI());

    String redirect = baseUrl + "/error";
    try {
      redirect = handleExchange(exchange);
    } catch (Exception e) {
      logger.warn("Error handling request: {}", e.getMessage());
    }

    logger.debug("redirecting to: {}", redirect);

    exchange.getResponseHeaders().set(HttpHeaders.LOCATION, redirect);
    exchange.sendResponseHeaders(303, -1);
  }

  private String handleExchange(HttpExchange exchange) throws JsonProcessingException {
    Headers requestHeaders = exchange.getRequestHeaders();
    Map<String, String> requestParams = ReferenceApiUtils.getRequestParams(exchange);

    // Verify request corresponds to a valid job
    String encodedIdCookie = ReferenceApiUtils.getCookie(requestHeaders, JsonKeys.ID_COOKIE_KEY);
    Preconditions.checkArgument(!Strings.isNullOrEmpty(encodedIdCookie), "Missing encodedIdCookie");
    UUID jobId = ReferenceApiUtils.decodeJobId(encodedIdCookie);
    PortabilityJob job = jobStore.findJob(jobId);
    Preconditions.checkNotNull(job, "existing job not found for jobId: %s", jobId);

    // Retrieve frob param from request
    String frob = requestParams.get(FROB);
    Preconditions.checkArgument(!Strings.isNullOrEmpty(frob), "Missing frob");

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

    // Generate AuthData
    // TODO: use UUID instead of UUID.toString()
    AuthData authData = generator.generateAuthData(null, frob, jobId.toString(), null, null);
    Preconditions.checkNotNull(authData, "AuthData should not be NULL");

    // Obtain the session key for this job
    String encodedSessionKey = job.jobAuthorization().sessionSecretKey();
    SecretKey key = symmetricKeyGenerator.parse(BaseEncoding.base64Url().decode(encodedSessionKey));
    // Serialize and encrypt the auth data
    String serialized = objectMapper.writeValueAsString(authData);
    String encryptedAuthData = EncrypterFactory.create(key).encrypt(serialized);
    // Set new cookie
    ReferenceApiUtils.setCookie(exchange.getResponseHeaders(), encryptedAuthData, authMode);

    return baseUrl
        + ((authMode == AuthMode.EXPORT)
            ? FrontendConstantUrls.URL_NEXT_PAGE
            : FrontendConstantUrls.URL_COPY_PAGE);
  }
}
