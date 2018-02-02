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

import static org.apache.axis.transport.http.HTTPConstants.HEADER_CONTENT_TYPE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.job.JobUtils;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.ServiceMode;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.OldPortabilityJob;
import org.dataportabilityproject.spi.cloud.types.OldPortabilityJob.JobState;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;
import org.dataportabilityproject.shared.settings.CommonSettings;
import org.dataportabilityproject.types.client.transfer.DataTransferResponse;
import org.dataportabilityproject.types.client.transfer.DataTransferResponse.Status;
import org.dataportabilityproject.types.client.transfer.SimpleLoginRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HttpHandler for SimpleLoginSubmit authorization flow. Redirects client request to:
 *   - the next authorization (if this is after the source service auth) or
 *   - the copy page (if this is after the destination service auth)
 */
final class SimpleLoginSubmitHandler implements HttpHandler {

  public static final String PATH = "/_/simpleLoginSubmit";
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final Logger logger = LoggerFactory.getLogger(SimpleLoginSubmitHandler.class);

  private final ServiceProviderRegistry serviceProviderRegistry;
  private final JobStore store;
  private final CryptoHelper cryptoHelper;
  private final CommonSettings commonSettings;

  @Inject
  SimpleLoginSubmitHandler(
      ServiceProviderRegistry serviceProviderRegistry,
      CloudFactory cloudFactory,
      CryptoHelper cryptoHelper,
      CommonSettings commonSettings) {
    this.serviceProviderRegistry = serviceProviderRegistry;
    this.store = cloudFactory.getJobStore();
    this.cryptoHelper = cryptoHelper;
    this.commonSettings = commonSettings;
  }

  public void handle(HttpExchange exchange) throws IOException {
    PortabilityApiUtils.validateRequest(exchange, HttpMethods.POST, PATH);

    DataTransferResponse response = handleExchange(exchange);

    logger.debug("simpleLoginSubmit, redirecting to: {}", response.getNextUrl());
    // Mark the response as type Json and send
    exchange.getResponseHeaders()
        .set(HEADER_CONTENT_TYPE, "application/json; charset=" + StandardCharsets.UTF_8.name());
    exchange.sendResponseHeaders(200, 0);

    objectMapper.writeValue(exchange.getResponseBody(), response);
  }

  DataTransferResponse handleExchange(HttpExchange exchange) throws IOException {
    DataTransferResponse response;

    try {
      SimpleLoginRequest request = objectMapper
          .readValue(exchange.getRequestBody(), SimpleLoginRequest.class);

      String encodedIdCookie = PortabilityApiUtils
          .getCookie(exchange.getRequestHeaders(), JsonKeys.ID_COOKIE_KEY);
      Preconditions
          .checkArgument(!Strings.isNullOrEmpty(encodedIdCookie), "Encoded Id Cookie required");
      String jobId = JobUtils.decodeId(encodedIdCookie);

      OldPortabilityJob job = commonSettings.getEncryptedFlow()
          ? store.find(jobId, JobState.PENDING_AUTH_DATA) : store.find(jobId);
      Preconditions.checkNotNull(job, "existing job not found for jobId: %s", jobId);

      ServiceMode serviceMode = PortabilityApiUtils.getServiceMode(
          job, exchange.getRequestHeaders(), commonSettings.getEncryptedFlow());

      String service =
          (serviceMode == ServiceMode.EXPORT) ? job.exportService() : job.importService();
      Preconditions.checkState(!Strings.isNullOrEmpty(service),
          "service not found, service: %s serviceMode: %s, job id: %s", service, serviceMode,
          jobId);

      PortableDataType dataType = JobUtils.getDataType(job.dataType());

      Preconditions
          .checkArgument(!Strings.isNullOrEmpty(request.getUsername()), "Missing valid username");
      Preconditions
          .checkArgument(!Strings.isNullOrEmpty(request.getPassword()), "Missing password");

      OnlineAuthDataGenerator generator = serviceProviderRegistry
          .getOnlineAuth(job.exportService(), dataType, serviceMode);
      Preconditions.checkNotNull(generator, "Generator not found for type: %s, service: %s",
          dataType, job.exportService());

      // Generate and store auth data
      AuthData authData = generator
          .generateAuthData(PortabilityApiFlags.baseApiUrl(), request.getUsername(), jobId, null,
              request.getPassword());
      Preconditions.checkNotNull(authData, "Auth data should not be null");

      if (!commonSettings.getEncryptedFlow()) {
        // Update the job
        OldPortabilityJob updatedJob = JobUtils.setAuthData(job, authData, serviceMode);
        store.update(updatedJob, null);
      }

      if (commonSettings.getEncryptedFlow()) {
        cryptoHelper.encryptAndSetCookie(exchange.getResponseHeaders(), job.id(), serviceMode, authData);
      }

      response = new DataTransferResponse(job.exportService(), job.importService(), job.dataType(), Status.INPROCESS,
          PortabilityApiFlags.baseUrl() + (serviceMode == ServiceMode.EXPORT
              ? FrontendConstantUrls.next : FrontendConstantUrls.copy));

    } catch (Exception e) {
      logger.debug("Exception occurred while trying to handle request: {}", e);
      throw e;
    }

    return response;
  }
}
