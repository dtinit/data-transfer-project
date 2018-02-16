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
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import org.dataportabilityproject.PortabilityCopier;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.job.Crypter;
import org.dataportabilityproject.job.CrypterFactory;
import org.dataportabilityproject.job.JobUtils;
import org.dataportabilityproject.job.PublicPrivateKeyPairGenerator;
import org.dataportabilityproject.job.TokenManager;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.settings.CommonSettings;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.JobAuthorization;
import org.dataportabilityproject.spi.cloud.types.LegacyPortabilityJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class StartCopyHandler implements HttpHandler {

  public final static String PATH = "/_/startCopy";
  private final Logger logger = LoggerFactory.getLogger(StartCopyHandler.class);

  private final ServiceProviderRegistry serviceProviderRegistry;
  private final JobStore store;
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
    this.store = cloudFactory.getJobStore();
    this.tokenManager = tokenManager;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    Preconditions.checkArgument(
        PortabilityApiUtils.validateRequest(exchange, HttpMethods.POST, PATH));

    UUID jobId = PortabilityApiUtils.validateJobId(exchange.getRequestHeaders(), tokenManager);

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
  private void handleWorkerAssignmentFlow(HttpExchange exchange, UUID jobId) throws IOException {

    // Encrypted job initiation flow
    //   - Validate auth data is present in cookies
    //   - Set Job to state pending assignment
    //   - Wait for a worker to be assigned
    //   - Once worker assigned, grab worker key to encrypt auth data from cookies
    //   - Update job with auth data

    // Lookup job
    LegacyPortabilityJob job = commonSettings.getEncryptedFlow()
        ? store.find(jobId, JobAuthorization.State.INITIAL) : store.find(jobId);
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
    job = job.toBuilder().setJobState(JobAuthorization.State.CREDS_AVAILABLE).build();
    store.update(jobId, job, JobAuthorization.State.INITIAL);
    logger.debug("Updated job {} to CREDS_AVAILABLE", jobId);

    // Loop until the worker updates it to assigned without auth data state, e.g. at that point
    // the worker instance key will be populated
    // TODO: start new thread
    // TODO: implement timeout condition
    // TODO: Handle case where API dies while waiting
    job = store.find(jobId);
    while (job == null || job.jobState() != JobAuthorization.State.CREDS_ENCRYPTION_KEY_GENERATED) {
      logger.debug("Waiting for job {} to enter state CREDS_ENCRYPTION_KEY_GENERATED", jobId);
      try {
        Sleeper.DEFAULT.sleep(10000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      job = store.find(jobId);
    }

    logger.debug("Got job {} in state CREDS_ENCRYPTION_KEY_GENERATED", jobId);

    Preconditions.checkNotNull(job.workerInstancePublicKey(),
        "Expected job " + jobId + " to have a worker instance's public key after being assigned "
            + "(state CREDS_ENCRYPTION_KEY_GENERATED)");
    Preconditions.checkState(job.encryptedExportAuthData() == null,
        "Didn't expect job " + jobId + " to have encrypted export auth data yet in state "
            + "CREDS_ENCRYPTION_KEY_GENERATED");
    Preconditions.checkState(job.encryptedImportAuthData() == null,
        "Didn't expect job " + jobId + " to have encrypted import auth data yet in state "
            + "CREDS_ENCRYPTION_KEY_GENERATED");

    // Populate job with auth data from cookies encrypted with worker key
    logger.debug("About to parse worker instance public key: {}", job.workerInstancePublicKey());
    PublicKey publicKey = PublicPrivateKeyPairGenerator
        .parsePublicKey(job.workerInstancePublicKey());
    logger.debug("Parsed publicKey, has length: {}", publicKey.getEncoded().length);

    // Encrypt the data with the assigned workers PublicKey and persist
    Crypter crypter = CrypterFactory.create(publicKey);
    String encryptedExportAuthData = crypter.encrypt(exportAuthCookieValue);
    logger.debug("Encrypted export auth data, has length: {}", encryptedExportAuthData.length());
    String encryptedImportAuthData = crypter.encrypt(importAuthCookieValue);
    logger.debug("Encrypted import auth data, has length: {}", encryptedImportAuthData.length());
    // Populate job with encrypted auth data
    job = job.toBuilder()
        .setEncryptedExportAuthData(encryptedExportAuthData)
        .setEncryptedImportAuthData(encryptedImportAuthData)
        .setJobState(JobAuthorization.State.CREDS_ENCRYPTED)
        .build();

    logger.debug("Updating job {} from CREDS_ENCRYPTION_KEY_GENERATED to CREDS_ENCRYPTED",
        jobId);
    store.update(jobId, job, JobAuthorization.State.CREDS_ENCRYPTION_KEY_GENERATED);

    writeResponse(exchange);
  }

  /**
   * Validates job information, starts the copy job inline, and returns status to the client.
   */
  private void handleStartCopyInApi(HttpExchange exchange, UUID jobId) throws IOException {
    // Lookup job
    LegacyPortabilityJob job = store.find(jobId);
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
              job.exportAuthData(), importService, job.importAuthData(), jobId);
        } catch (IOException e) {
          logger.error("copyDataType failed", e);
          e.printStackTrace();
        } finally {
          cloudFactory.clearJobData(jobId);
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
