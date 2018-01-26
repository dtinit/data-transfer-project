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
import static org.apache.axis.transport.http.HTTPConstants.HEADER_SET_COOKIE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.HttpCookie;
import java.nio.charset.StandardCharsets;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.job.JobDao;
import org.dataportabilityproject.job.JobUtils;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.job.PortabilityJobFactory;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.ServiceMode;
import org.dataportabilityproject.shared.auth.AuthFlowInitiator;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;
import org.dataportabilityproject.shared.settings.CommonSettings;
import org.dataportabilityproject.types.client.transfer.DataTransferRequest;
import org.dataportabilityproject.types.client.transfer.DataTransferResponse;
import org.dataportabilityproject.types.client.transfer.DataTransferResponse.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HttpHandler for the DataTransfer service Takes a DataTransferRequest and creates a new job entry
 * according to the request. Redirects to the authorization flow for source Service.
 */
final class DataTransferHandler implements HttpHandler {

  public static final String PATH = "/_/DataTransfer";

  private static final Logger logger = LoggerFactory.getLogger(DataTransferHandler.class);
  private final ServiceProviderRegistry serviceProviderRegistry;
  private final JobDao jobDao;
  private final PortabilityJobFactory jobFactory;
  private final CommonSettings commonSettings;
  private final ObjectMapper objectMapper;

  @Inject
  DataTransferHandler(ServiceProviderRegistry serviceProviderRegistry,
      JobDao jobDao,
      PortabilityJobFactory jobFactory,
      CommonSettings commonSettings) {
    this.serviceProviderRegistry = serviceProviderRegistry;
    this.jobDao = jobDao;
    this.jobFactory = jobFactory;
    this.commonSettings = commonSettings;
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Given a set of job configuration parameters, this will create a new job and kick off auth flows
   * for the specified configuration. TODO: Determine what to do if previous job exists in the
   * session instead of creating a new job every time. TODO: Preconditions doesn't return an error
   * code or page. So if a page is requested with invalid params or incorrect method, no error is
   * present and the response is empty.
   */
  public void handle(HttpExchange exchange) throws IOException {
    Preconditions.checkArgument(
        PortabilityApiUtils.validateRequest(exchange, HttpMethods.POST, PATH),
        PATH + " only supports POST.");
    logger.debug("received request: {}", exchange.getRequestURI());

    DataTransferResponse dataTransferResponse = handleExchange(exchange);
    logger.debug("redirecting to: {}", dataTransferResponse.getNextUrl());

    // Mark the response as type Json and send
    exchange.getResponseHeaders()
        .set(HEADER_CONTENT_TYPE, "application/json; charset=" + StandardCharsets.UTF_8.name());
    exchange.sendResponseHeaders(200, 0);
    objectMapper.writeValue(exchange.getResponseBody(), dataTransferResponse);
  }


  DataTransferResponse handleExchange(HttpExchange exchange) throws IOException {
    String redirect = "/error";
    DataTransferRequest request = objectMapper
        .readValue(exchange.getRequestBody(), DataTransferRequest.class);

    try {
      String dataTypeStr = request.getTransferDataType();
      Preconditions.checkArgument(!Strings.isNullOrEmpty(dataTypeStr),
          "Missing valid dataTypeParam: %s", dataTypeStr);

      PortableDataType dataType = JobUtils.getDataType(dataTypeStr);

      String exportService = request.getSource();
      Preconditions.checkArgument(JobUtils.isValidService(exportService, ServiceMode.EXPORT),
          "Missing valid exportService: %s", exportService);

      String importService = request.getDestination();
      Preconditions.checkArgument(JobUtils.isValidService(importService, ServiceMode.IMPORT),
          "Missing valid importService: %s", importService);

      // Create a new job and persist
      PortabilityJob newJob = createJob(dataType, exportService, importService);

      // Set new cookie
      HttpCookie cookie = new HttpCookie(JsonKeys.ID_COOKIE_KEY, JobUtils.encodeId(newJob));
      exchange.getResponseHeaders()
          .add(HEADER_SET_COOKIE, cookie.toString() + PortabilityApiUtils.COOKIE_ATTRIBUTES);

      // Lookup job, even if just recently created
      PortabilityJob job;
      if (commonSettings.getEncryptedFlow()) {
        job = PortabilityApiUtils.lookupJobPendingAuthData(newJob.id(), jobDao);
      } else {
        job = PortabilityApiUtils.lookupJob(newJob.id(), jobDao);
      }
      Preconditions.checkState(job != null, "Job required");

      // TODO: Validate job before going further

      // Obtain the OnlineAuthDataGenerator for export service
      OnlineAuthDataGenerator generator = serviceProviderRegistry
          .getOnlineAuth(job.exportService(), dataType, ServiceMode.EXPORT);
      Preconditions.checkNotNull(generator, "Generator not found for type: %s, service: %s",
          dataType, job.exportService());

      // Auth authUrl
      AuthFlowInitiator authFlowInitiator = generator
          .generateAuthUrl(PortabilityApiFlags.baseApiUrl(), JobUtils.encodeId(newJob));
      Preconditions
          .checkNotNull(authFlowInitiator, "AuthFlowInitiator not found for type: %s, service: %s",
              dataType, job.exportService());

      // Store initial auth data for export services. Any initial auth data for import
      // is done in the SetupHandler in IMPORT mode
      if (authFlowInitiator.initialAuthData() != null) {
        PortabilityJob updatedJob = JobUtils
            .setInitialAuthData(job, authFlowInitiator.initialAuthData(), ServiceMode.EXPORT);
        if (commonSettings.getEncryptedFlow()) {
          jobDao.updatePendingAuthDataJob(updatedJob);
        } else {
          jobDao.updateJob(updatedJob);
        }
      }

      // Send the authUrl for the client to redirect to export service authorization
      redirect = authFlowInitiator.authUrl();
    } catch (Exception e) {
      logger.error("Error handling request", e);
      throw e;
    }

    return new DataTransferResponse(request.getSource(), request.getDestination(),
        request.getTransferDataType(),
        Status.INPROCESS, redirect); // to the auth url for the export service
  }

  /**
   * Create the initial job in initial state and persist in storage.
   */
  private PortabilityJob createJob(PortableDataType dataType, String exportService,
      String importService)
      throws IOException {
    PortabilityJob job = jobFactory.create(dataType, exportService, importService);
    if (commonSettings.getEncryptedFlow()) {
      // This is the initial population of the row in storage
      jobDao.insertJobInPendingAuthDataState(job);
    } else {
      jobDao.insertJob(job);
    }
    return job;
  }
}
