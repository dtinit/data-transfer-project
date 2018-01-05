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

import static org.apache.axis.transport.http.HTTPConstants.HEADER_LOCATION;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Map;
import org.dataportabilityproject.PortabilityFlags;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.job.JobDao;
import org.dataportabilityproject.job.JobUtils;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HttpHandler for SimpleLoginSubmit Controller.
 */
public class SimpleLoginSubmitHandler implements HttpHandler {
  private final Logger logger = LoggerFactory.getLogger(SimpleLoginSubmitHandler.class);

  private final ServiceProviderRegistry serviceProviderRegistry;
  private final JobDao jobDao;
  private final CryptoHelper cryptoHelper;

  public SimpleLoginSubmitHandler(ServiceProviderRegistry serviceProviderRegistry, JobDao jobDao,
      CryptoHelper cryptoHelper) {
    this.serviceProviderRegistry = serviceProviderRegistry;
    this.jobDao = jobDao;
    this.cryptoHelper = cryptoHelper;
  }

  public void handle(HttpExchange exchange) throws IOException {
    PortabilityApiUtils.validateRequest(exchange, HttpMethods.POST, "/simpleLoginSubmit");

    String encodedIdCookie = PortabilityApiUtils
        .getCookie(exchange.getRequestHeaders(), JsonKeys.ID_COOKIE_KEY);
    Preconditions
        .checkArgument(!Strings.isNullOrEmpty(encodedIdCookie), "Encoded Id Cookie required");
    String jobId = JobUtils.decodeId(encodedIdCookie);

    PortabilityJob job;
    if (PortabilityFlags.encryptedFlow()) {
      job = PortabilityApiUtils.lookupJobPendingAuthData(jobId, jobDao);
    }  else {
      job = PortabilityApiUtils.lookupJob(jobId, jobDao);
    }
    Preconditions.checkState(null != job, "existingJob not found for job id: %s", jobId);

    // TODO: Determine import vs export mode
    // Hack! For now, if we don't have export auth data, assume it's for export.
    boolean isExport = (null == job.exportAuthData());
    String service = isExport ? job.exportService() : job.importService();
    Preconditions.checkState(!Strings.isNullOrEmpty(service),
        "service not found, service: %s isExport: %b, job id: %s", service, isExport, jobId);

    PortableDataType dataType = JobUtils.getDataType(job.dataType());

    Map<String, String> requestParams = PortabilityApiUtils.getRequestParams(exchange);
    requestParams.putAll(PortabilityApiUtils.getPostParams(exchange));

    String username = requestParams.get("username");
    String password = requestParams.get("password");
    Preconditions
        .checkArgument(!Strings.isNullOrEmpty(username), "Missing valid username: %s", username);
    Preconditions.checkArgument(!Strings.isNullOrEmpty(password), "Password is empty");

    OnlineAuthDataGenerator generator = serviceProviderRegistry
        .getOnlineAuth(job.exportService(), dataType);
    Preconditions.checkNotNull(generator, "Generator not found for type: %s, service: %s",
        dataType, job.exportService());

    // Generate and store auth data
    AuthData authData = generator
        .generateAuthData(PortabilityApiFlags.baseApiUrl(), username, jobId, null, password);
    Preconditions.checkNotNull(authData, "Auth data should not be null");

    if (!PortabilityFlags.encryptedFlow()) {
      // Update the job
      PortabilityJob updatedJob = JobUtils.setAuthData(job, authData, isExport);
      jobDao.updateJob(updatedJob);
    }

    String redirect = PortabilityApiFlags.baseUrl() + (isExport ? "/next" : "/copy");

    // Set new cookie and redirect to the next page
    logger.debug("simpleLoginSubmit, redirecting to: {}", redirect);
    Headers responseHeaders = exchange.getResponseHeaders();

    if(PortabilityFlags.encryptedFlow()) {
      cryptoHelper.encryptAndSetCookie(responseHeaders, job.id(), isExport, authData);
    }
    responseHeaders.set(HEADER_LOCATION, redirect);
    exchange.sendResponseHeaders(303, -1);
  }
}
