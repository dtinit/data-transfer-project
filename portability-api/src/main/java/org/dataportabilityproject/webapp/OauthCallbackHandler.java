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
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Map;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.job.JobDao;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.shared.LogUtils;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.Secrets;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;

/**
 * HttpHandler for callbacks from Oauth1 authorization flow.
 */
public class OauthCallbackHandler implements HttpHandler {

  private final ServiceProviderRegistry serviceProviderRegistry;
  private final JobDao jobDao;
  private final CryptoHelper cryptoHelper;

  public OauthCallbackHandler(ServiceProviderRegistry serviceProviderRegistry,
      JobDao jobDao, CryptoHelper cryptoHelper) {
    this.serviceProviderRegistry = serviceProviderRegistry;
    this.jobDao = jobDao;
    this.cryptoHelper = cryptoHelper;
  }

  public void handle(HttpExchange exchange) throws IOException {
    PortabilityServerUtils.validateRequest(exchange, HttpMethods.GET, "/callback1/.*");
    String redirect = handleExchange(exchange);

    LogUtils.log("OauthCallbackHandler, redirecting to %s", redirect);
    exchange.getResponseHeaders().set(HEADER_LOCATION, redirect);
    exchange.sendResponseHeaders(303, -1);
  }

  private String handleExchange(HttpExchange exchange) throws IOException {
    String redirect = "";

    try {
      Headers requestHeaders = exchange.getRequestHeaders();

      // Get the URL for the request - needed for the authorization.
      String requestURL = PortabilityServerUtils
          .createURL(exchange.getProtocol(), requestHeaders.getFirst(HEADER_HOST),
              exchange.getRequestURI().toString());
      LogUtils.log("OauthCallbackHandler, Request URL: %s", requestURL);

      Map<String, String> requestParams = PortabilityServerUtils.getRequestParams(exchange);

      String encodedIdCookie = PortabilityServerUtils
          .getCookie(requestHeaders, JsonKeys.ID_COOKIE_KEY);
      Preconditions
          .checkArgument(!Strings.isNullOrEmpty(encodedIdCookie), "Missing encodedIdCookie");
      LogUtils.log("OauthCallbackHandler, encodedCookieId: %s", encodedIdCookie);

      String oauthToken = requestParams.get("oauth_token");
      Preconditions.checkArgument(!Strings.isNullOrEmpty(oauthToken), "Missing oauth_token");
      LogUtils.log("OauthCallbackHandler, oauthToken: %s", oauthToken);

      String oauthVerifier = requestParams.get("oauth_verifier");
      Preconditions.checkArgument(!Strings.isNullOrEmpty(oauthVerifier), "Missing oauth_verifier");
      LogUtils.log("OauthCallbackHandler, oauthVerifier: %s", oauthVerifier);

      // Valid job must be present
      Preconditions
          .checkArgument(!Strings.isNullOrEmpty(encodedIdCookie), "Encoded Id cookie required");
      String jobId = JobUtils.decodeId(encodedIdCookie);

      PortabilityJob job = PortabilityServerUtils.lookupJob(jobId, jobDao);

      PortableDataType dataType = JobUtils.getDataType(job.dataType());

      // TODO: Support import and export service
      // Hack! For now, if we don't have export auth data, assume it's for export.
      boolean isExport = (null == job.exportAuthData());

      // TODO: Determine service from job or from authUrl path?
      String service = isExport ? job.exportService() : job.importService();
      Preconditions.checkState(!Strings.isNullOrEmpty(service),
          "service not found, service: %s isExport: %b, job id: %s", service, isExport, jobId);
      LogUtils.log("service: %s, isExport: %b", service, isExport);

      // Obtain the ServiceProvider from the registry
      OnlineAuthDataGenerator generator = serviceProviderRegistry.getOnlineAuth(service, dataType);

      // Retrieve initial auth data, if it existed
      AuthData initialAuthData = JobUtils.getInitialAuthData(job, isExport);
      Preconditions
          .checkNotNull(initialAuthData, "Initial AuthData expected during Oauth 1.0 flow");

      // Generate and store auth data
      AuthData authData = generator.generateAuthData(oauthVerifier, jobId, initialAuthData, null);

      // Update the job
      // TODO: Remove persistence of auth data in storage at this point. The data will be passed
      // thru to the client via the cookie.
      PortabilityJob updatedJob = JobUtils.setAuthData(job, authData, isExport);
      jobDao.updateJob(updatedJob);

      // Get responseHeaders to set redirect and new cookie
      Headers responseHeaders = exchange.getResponseHeaders();
      cryptoHelper.encryptAndSetCookie(responseHeaders, isExport, authData);
      redirect = Secrets.getInstance().baseUrl() + (isExport ? "/next" : "/copy");
    } catch (Exception e) {
      LogUtils.log("Error while handling request: %s", e);
      LogUtils.log("StackTrace: %s", e.getStackTrace());
      throw e;
    }

    return redirect;
  }
}
