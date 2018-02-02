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
import org.dataportabilityproject.cloud.interfaces.PersistentKeyValueStore;
import org.dataportabilityproject.job.Crypter;
import org.dataportabilityproject.job.CrypterFactory;
import org.dataportabilityproject.job.JobUtils;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.job.PortabilityJob.JobState;
import org.dataportabilityproject.job.PublicPrivateKeyPairGenerator;
import org.dataportabilityproject.job.TokenManager;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.settings.CommonSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class StartCopyHandler implements HttpHandler {

  public final static String PATH = "/_/startCopy";
  private final Logger logger = LoggerFactory.getLogger(StartCopyHandler.class);

  private final ServiceProviderRegistry serviceProviderRegistry;
  private final PersistentKeyValueStore store;
  private final CloudFactory cloudFactory;
  private final CommonSettings commonSettings;
  private final TokenManager tokenManager;

  @Inject
  StartCopyHandler(
      ServiceProviderRegistry serviceProviderRegistry,
      CloudFactory cloudFactory,
      CommonSettings commonSettings,
      TokenManager tokenManager) {
    this.serviceProviderRegistry = serviceProviderRegistry;
    this.cloudFactory = cloudFactory;
    this.commonSettings = commonSettings;
    this.store = cloudFactory.getPersistentKeyValueStore();
    this.tokenManager = tokenManager;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    Preconditions.checkArgument(
        PortabilityApiUtils.validateRequest(exchange, HttpMethods.POST, PATH));

    String jobId = PortabilityApiUtils.validateJobId(exchange.getRequestHeaders(), tokenManager);

    if (commonSettings.getEncryptedFlow()) {
      handleWorkerAssignmentFlow(exchange, jobId);
    } else {
      handleStartCopyInApi(exchange, jobId);
    }
  }

  /**
   * Handles flow for assigning a worker instance, encrypting data with the assigned worker key, and
   * persisting the auth data, which will result in the worker starting the copy.
   */
  private void handleWorkerAssignmentFlow(HttpExchange exchange, String jobId) throws IOException {

    // Encrypted job initiation flow
    //   - Validate auth data is present in cookies
    //   - Set Job to state pending assignment
    //   - Wait for a worker to be assigned
    //   - Once worker assigned, grab worker key to encrypt auth data from cookies
    //   - Update job with auth data

    // Lookup job
    PortabilityJob job = commonSettings.getEncryptedFlow()
        ? store.find(jobId, JobState.PENDING_AUTH_DATA) : store.find(jobId);
    Preconditions.checkNotNull(job, "existing job not found for jobId: %s", jobId);
    // Validate job
    String exportService = job.exportService();
    Preconditions.checkState(!Strings.isNullOrEmpty(exportService), "Export service is invalid");
    String importService = job.importService();
    Preconditions.checkState(!Strings.isNullOrEmpty(importService), "Import service is invalid");

    //  Validate auth data is present in cookies
    String exportAuthCookieValue = PortabilityApiUtils
        .getCookie(exchange.getRequestHeaders(), JsonKeys.EXPORT_AUTH_DATA_COOKIE_KEY);
    Preconditions
        .checkArgument(!Strings.isNullOrEmpty(exportAuthCookieValue),
            "Export auth cookie required");

    String importAuthCookieValue = PortabilityApiUtils
        .getCookie(exchange.getRequestHeaders(), JsonKeys.IMPORT_AUTH_DATA_COOKIE_KEY);
    Preconditions
        .checkArgument(!Strings.isNullOrEmpty(importAuthCookieValue),
            "Import auth cookie required");

    // We have the data, now update to 'pending worker assignment' so a worker may be assigned
    job = job.toBuilder().setJobState(JobState.PENDING_WORKER_ASSIGNMENT).build();
    store.update(job, JobState.PENDING_AUTH_DATA);
    logger.debug("Updated job {} to PENDING_WORKER_ASSIGNMENT", jobId);

    // Loop until the worker updates it to assigned without auth data state, e.g. at that point
    // the worker instance key will be populated
    // TODO: start new thread
    // TODO: implement timeout condition
    // TODO: Handle case where API dies while waiting
    job = store.find(jobId);
    while (job == null || job.jobState() != JobState.ASSIGNED_WITHOUT_AUTH_DATA) {
      logger.debug("Waiting for job {} to enter state ASSIGNED_WITHOUT_AUTH_DATA", jobId);
      try {
        Sleeper.DEFAULT.sleep(10000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      job = store.find(jobId);
    }

    logger.debug("Found job after while loop, lookupAssignedWithoutAuthDataJob, id: {}", jobId);

    // Ensure job is assigned and has worker key
    job = store.find(jobId, JobState.ASSIGNED_WITHOUT_AUTH_DATA);

    logger.debug("Found job after lookupAssignedWithoutAuthDataJob, id: {}", jobId);
    Preconditions.checkNotNull(job.workerInstancePublicKey() != null);
    // Populate job with auth data from cookies encrypted with worker key
    logger.debug("About to parse: {}", job.workerInstancePublicKey());
    PublicKey publicKey = PublicPrivateKeyPairGenerator
        .parsePublicKey(job.workerInstancePublicKey());
    logger.debug("Found publicKey: {}", publicKey.getEncoded().length);

    // Encrypt the data with the assigned workers PublicKey and persist
    Crypter crypter = CrypterFactory.create(publicKey);
    String encryptedExportAuthData = crypter.encrypt(exportAuthCookieValue);
    logger.debug("Created encryptedExportAuthData: {}", encryptedExportAuthData.length());
    String encryptedImportAuthData = crypter.encrypt(importAuthCookieValue);
    logger.debug("Created encryptedImportAuthData: {}", encryptedImportAuthData.length());
    Preconditions.checkNotNull(job, "Attempting to update a non-existent job");
    Preconditions.checkState(job.encryptedExportAuthData() == null);
    Preconditions.checkState(job.encryptedImportAuthData() == null);
    // Populate job with encrypted auth data
    job = job.toBuilder()
        .setEncryptedExportAuthData(encryptedExportAuthData)
        .setEncryptedImportAuthData(encryptedImportAuthData)
        .setJobState(JobState.ASSIGNED_WITH_AUTH_DATA)
        .build();
    store.update(job, JobState.ASSIGNED_WITHOUT_AUTH_DATA);

    writeResponse(exchange);
  }

  /**
   * Validates job information, starts the copy job inline, and returns status to the client.
   */
  private void handleStartCopyInApi(HttpExchange exchange, String jobId) throws IOException {
    // Lookup job
    PortabilityJob job = store.find(jobId);
    Preconditions.checkState(null != job, "existing job not found for id: %s", jobId);
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
