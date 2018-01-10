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

import com.google.api.client.util.Sleeper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import org.dataportabilityproject.PortabilityCopier;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.job.JobDao;
import org.dataportabilityproject.job.JobUtils;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.job.PublicPrivateKeyUtils;
import org.dataportabilityproject.job.TokenManager;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.settings.CommonSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class StartCopyHandler implements HttpHandler {

  private final Logger logger = LoggerFactory.getLogger(StartCopyHandler.class);

  private final ServiceProviderRegistry serviceProviderRegistry;
  private final JobDao jobDao;
  private final CloudFactory cloudFactory;
  private final CryptoHelper cryptoHelper;
  private final CommonSettings commonSettings;
  private final TokenManager tokenManager;

  @Inject
  StartCopyHandler(ServiceProviderRegistry serviceProviderRegistry, JobDao jobDao,
      CloudFactory cloudFactory,
      CryptoHelper cryptoHelper,
      CommonSettings commonSettings,
      TokenManager tokenManager) {
    this.serviceProviderRegistry = serviceProviderRegistry;
    this.jobDao = jobDao;
    this.cloudFactory = cloudFactory;
    this.cryptoHelper = cryptoHelper;
    this.commonSettings = commonSettings;
    this.tokenManager = tokenManager;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    Preconditions.checkArgument(
        PortabilityApiUtils.validateRequest(exchange, HttpMethods.POST, "/_/startCopy"));

    String jobId = validateJobId(exchange.getRequestHeaders());

    if (commonSettings.getEncryptedFlow()) {
      handleWorkerAssignmentFlow(exchange, jobId);
    } else {
      handleStartCopyInApi(exchange, jobId);
    }
  }

  /**
   * Validates that the request coming is for a valid job and contains the correct xsrf header
   */
  private String validateJobId(Headers requestHeaders) {
    String encodedIdCookie = PortabilityApiUtils
        .getCookie(requestHeaders, JsonKeys.ID_COOKIE_KEY);
    Preconditions
        .checkArgument(!Strings.isNullOrEmpty(encodedIdCookie), "Encoded Id cookie required");

    // Valid job must be present
    String jobId = JobUtils.decodeId(encodedIdCookie);

    // Validate XSRF token is present in request header. The cookie value might be surrounded by
    // double quotes which causes the angular cli to also surround the header with double quotes.
    // Since the value itself may not contain quotes or whitespace, trim off the double quotes by
    // converting them to whitespace.
    String tokenHeader = requestHeaders.getFirst(JsonKeys.XSRF_HEADER)
        .replace("\"", " ")
        .trim();
    String tokenCookie = PortabilityApiUtils
        .getCookie(requestHeaders, JsonKeys.XSRF_TOKEN);

    // Both header and token should be present
    Preconditions.checkArgument(!Strings.isNullOrEmpty(tokenHeader));
    Preconditions.checkArgument(!Strings.isNullOrEmpty(tokenCookie));

    // The token present in the header should be the same as the token present in the cookie.
    Preconditions.checkArgument(tokenCookie.equals(tokenHeader));

    // Verify that the token is actually valid in the tokenManager
    Preconditions.checkArgument(tokenManager.verifyToken(tokenHeader));

    // finally make sure the jobId present in the token is also equal to the jobId present in the cookie
    String jobIdFromToken = tokenManager.getData(tokenHeader);
    Preconditions.checkArgument(jobId.equals(jobIdFromToken));
    return jobId;
  }

  /**
   * Handles flow for assigning a worker instance, encrypting data with the assigned worker key, and
   * persisting the auth data, which will result in the worker starting the copy.
   */
  private void handleWorkerAssignmentFlow(HttpExchange exchange, String id)
      throws IOException {

    // Encrypted job initiation flow
    //   - Validate auth data is present in cookies
    //   - Set Job to state pending assignment
    //   - Wait for a worker to be assigned
    //   - Once worker assigned, grab worker key to encrypt auth data from cookies
    //   - Update job with auth data

    // Lookup job
    PortabilityJob job = PortabilityApiUtils.lookupJobPendingAuthData(id, jobDao);
    Preconditions.checkState(null != job, "existing job not found for id: %s", id);
    // Validate job
    String exportService = job.exportService();
    Preconditions.checkState(!Strings.isNullOrEmpty(exportService), "Export service is invalid");
    String importService = job.importService();
    Preconditions.checkState(!Strings.isNullOrEmpty(importService), "Import service is invalid");
    PortableDataType type = JobUtils.getDataType(job.dataType());

    //  Validate auth data is present in cookies
    String exportAuthCookie = PortabilityApiUtils
        .getCookie(exchange.getRequestHeaders(), JsonKeys.EXPORT_AUTH_DATA_COOKIE_KEY);
    Preconditions
        .checkArgument(!Strings.isNullOrEmpty(exportAuthCookie), "Export auth cookie required");

    String importAuthCookie = PortabilityApiUtils
        .getCookie(exchange.getRequestHeaders(), JsonKeys.IMPORT_AUTH_DATA_COOKIE_KEY);
    Preconditions
        .checkArgument(!Strings.isNullOrEmpty(importAuthCookie), "Import auth cookie required");

    // We have the data, now update it unassigned so it can be assigned a worker
    // Set Job to state to pending worker assignment
    jobDao.updateJobStateToPendingWorkerAssignment(job.id()); // Now that job is complete unassiged

    // Loop until the worker updates it to assigned without auth data state, e.g. at that point
    // the worker instance key will be populated
    // TODO: start new thread
    // TODO: implement timeout condition
    // TODO: Handle case where API dies while waiting
    while (jobDao.lookupAssignedWithoutAuthDataJob(job.id()) == null) {
      try {
        Sleeper.DEFAULT.sleep(5000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    // Ensure job is assigned and has worker key
    PortabilityJob assignedJob = jobDao.lookupAssignedWithoutAuthDataJob(job.id());
    Preconditions.checkNotNull(assignedJob.workerInstancePublicKey() != null);
    // Populate job with auth data from cookies encrypted with worker key
    PublicKey publicKey = PublicPrivateKeyUtils
        .parsePublicKey(assignedJob.workerInstancePublicKey());
    jobDao.updateJobStateToAssigneWithAuthData(assignedJob.id(),
        cryptoHelper.encryptAuthData(publicKey, exportAuthCookie),
        cryptoHelper.encryptAuthData(publicKey, importAuthCookie));

    writeResponse(exchange);
  }

  /**
   * Validates job information, starts the copy job inline, and returns status to the client.
   */
  private void handleStartCopyInApi(HttpExchange exchange, String id) throws IOException {
    // Lookup job
    PortabilityJob job = PortabilityApiUtils.lookupJob(id, jobDao);
    Preconditions.checkState(null != job, "existing job not found for id: %s", id);
    // Validate job
    String exportService = job.exportService();
    Preconditions.checkState(!Strings.isNullOrEmpty(exportService), "Export service is invalid");
    String importService = job.importService();
    Preconditions.checkState(!Strings.isNullOrEmpty(importService), "Import service is invalid");
    PortableDataType type = JobUtils.getDataType(job.dataType());
    // Validate for data is present in job for non-worker based flow
    Preconditions.checkState(job.exportAuthData() != null, "Export AuthData is required");
    Preconditions.checkState(job.importAuthData() != null, "Import AuthData is required");
    // TODO: Design better threading for new copy tasks with exception handling
    Runnable r = new Runnable() {
      public void run() {
        try {
          PortabilityCopier.copyDataType(serviceProviderRegistry, type, exportService,
              job.exportAuthData(), importService, job.importAuthData(), job.id());
        } catch (IOException e) {
          logger.error("copyDataType failed", e);
          e.printStackTrace();
        } finally {
          cloudFactory.clearJobData(job.id());
        }
      }
    };

    ExecutorService executor = Executors.newCachedThreadPool();
    executor.submit(r);

    writeResponse(exchange);
  }

  /**
   * Write a response with status to the client.
   */
  private void writeResponse(HttpExchange exchange) throws IOException {
    JsonObject response = Json.createObjectBuilder().add("status", "started").build();

    // Mark response as Json and send
    exchange.getResponseHeaders()
        .set(HEADER_CONTENT_TYPE, "application/json; charset=" + StandardCharsets.UTF_8.name());
    exchange.sendResponseHeaders(200, 0);
    JsonWriter writer = Json.createWriter(exchange.getResponseBody());
    writer.write(response);
    writer.close();
  }
}
