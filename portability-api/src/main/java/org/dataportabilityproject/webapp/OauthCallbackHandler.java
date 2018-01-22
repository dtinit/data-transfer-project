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
package org.dataportabilityproject.webapp;

import static org.apache.axis.transport.http.HTTPConstants.HEADER_HOST;
import static org.apache.axis.transport.http.HTTPConstants.HEADER_LOCATION;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Map;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.job.JobDao;
import org.dataportabilityproject.job.JobUtils;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.shared.Config.Environment;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.ServiceMode;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;
import org.dataportabilityproject.shared.settings.CommonSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HttpHandler for callbacks from Oauth1 authorization flow.
 */
final class OauthCallbackHandler implements HttpHandler {

  private final Logger logger = LoggerFactory.getLogger(OauthCallbackHandler.class);

  private final ServiceProviderRegistry serviceProviderRegistry;
  private final JobDao jobDao;
  private final CryptoHelper cryptoHelper;
  private final CommonSettings commonSettings;

  @Inject
  OauthCallbackHandler(ServiceProviderRegistry serviceProviderRegistry,
      JobDao jobDao,
      CryptoHelper cryptoHelper,
      CommonSettings commonSettings) {
    this.serviceProviderRegistry = serviceProviderRegistry;
    this.jobDao = jobDao;
    this.cryptoHelper = cryptoHelper;
    this.commonSettings = commonSettings;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    PortabilityApiUtils.validateRequest(exchange, HttpMethods.GET, "/callback1/.*");
    logger.debug("received request: {}", exchange.getRequestURI());

    String redirect = handleExchange(exchange);
    logger.debug("redirecting to {}", redirect);
    exchange.getResponseHeaders().set(HEADER_LOCATION, redirect);
    exchange.sendResponseHeaders(303, -1);
  }

  private String handleExchange(HttpExchange exchange) throws IOException {
    String redirect = "/error";

    try {
      Headers requestHeaders = exchange.getRequestHeaders();

      // Get the URL for the request - needed for the authorization.

      String requestURL = PortabilityApiUtils
          .createURL(
              requestHeaders.getFirst(HEADER_HOST),
              exchange.getRequestURI().toString(),
              commonSettings.getEnv() != Environment.LOCAL);

      Map<String, String> requestParams = PortabilityApiUtils.getRequestParams(exchange);

      String encodedIdCookie = PortabilityApiUtils
          .getCookie(requestHeaders, JsonKeys.ID_COOKIE_KEY);
      Preconditions
          .checkArgument(!Strings.isNullOrEmpty(encodedIdCookie), "Missing encodedIdCookie");

      String oauthToken = requestParams.get("oauth_token");
      Preconditions.checkArgument(!Strings.isNullOrEmpty(oauthToken), "Missing oauth_token");

      String oauthVerifier = requestParams.get("oauth_verifier");
      Preconditions.checkArgument(!Strings.isNullOrEmpty(oauthVerifier), "Missing oauth_verifier");

      // Valid job must be present
      Preconditions
          .checkArgument(!Strings.isNullOrEmpty(encodedIdCookie), "Encoded Id cookie required");
      String jobId = JobUtils.decodeId(encodedIdCookie);

      PortabilityJob job;
      if (commonSettings.getEncryptedFlow()) {
        job = PortabilityApiUtils.lookupJobPendingAuthData(jobId, jobDao);
      } else {
        job = PortabilityApiUtils.lookupJob(jobId, jobDao);
      }
      PortableDataType dataType = JobUtils.getDataType(job.dataType());

      ServiceMode serviceMode = PortabilityApiUtils.getServiceMode(
          job,
          exchange.getRequestHeaders(),
          commonSettings.getEncryptedFlow());

      // TODO: Determine service from job or from authUrl path?
      String service =
          serviceMode == ServiceMode.EXPORT ? job.exportService() : job.importService();
      Preconditions.checkState(!Strings.isNullOrEmpty(service),
          "service not found, service: %s serviceMode: %s, job id: %s", service, serviceMode,
          jobId);

      // Obtain the ServiceProvider from the registry
      OnlineAuthDataGenerator generator = serviceProviderRegistry.getOnlineAuth(service, dataType, serviceMode);

      // Retrieve initial auth data, if it existed
      AuthData initialAuthData = JobUtils.getInitialAuthData(job, serviceMode);
      Preconditions
          .checkNotNull(initialAuthData, "Initial AuthData expected during Oauth 1.0 flow");

      // Generate and store auth data
      AuthData authData = generator
          .generateAuthData(PortabilityApiFlags.baseApiUrl(), oauthVerifier, jobId, initialAuthData,
              null);

      if (!commonSettings.getEncryptedFlow()) {
        // Update the job
        PortabilityJob updatedJob = JobUtils.setAuthData(job, authData, serviceMode);
        jobDao.updateJob(updatedJob);
      } else {
        // Set new cookie
        cryptoHelper
            .encryptAndSetCookie(exchange.getResponseHeaders(), job.id(), serviceMode, authData);
      }

      redirect =
          PortabilityApiFlags.baseUrl() + ((serviceMode == ServiceMode.EXPORT) ? "/next" : "/copy");
    } catch (Exception e) {
      logger.error("Error handling request", e);
      throw e;
    }

    return redirect;
  }
}
