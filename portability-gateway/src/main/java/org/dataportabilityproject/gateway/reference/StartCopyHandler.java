/*
 * Copyright 2018 The Data-Portability Project Authors.
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
package org.dataportabilityproject.gateway.reference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Sleeper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import com.google.inject.Inject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.UUID;
import org.apache.http.HttpHeaders;
import org.dataportabilityproject.gateway.reference.ReferenceApiUtils.FrontendConstantUrls;
import org.dataportabilityproject.gateway.reference.ReferenceApiUtils.HttpMethods;
import org.dataportabilityproject.security.AsymmetricKeyGenerator;
import org.dataportabilityproject.security.Encrypter;
import org.dataportabilityproject.security.EncrypterFactory;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.JobAuthorization;
import org.dataportabilityproject.spi.cloud.types.PortabilityJob;
import org.dataportabilityproject.spi.cloud.types.TypeManager;
import org.dataportabilityproject.types.client.transfer.DataTransferResponse;
import org.dataportabilityproject.types.client.transfer.DataTransferResponse.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class StartCopyHandler implements HttpHandler {

  public static final String PATH = "/_/startCopy";
  private final Logger logger = LoggerFactory.getLogger(StartCopyHandler.class);

  private final JobStore store;
  private final TokenManager tokenManager;
  private final AsymmetricKeyGenerator asymmetricKeyGenerator;
  private final ObjectMapper objectMapper;

  @Inject
  StartCopyHandler(
      JobStore store,
      TokenManager tokenManager,
      AsymmetricKeyGenerator asymmetricKeyGenerator,
      TypeManager typeManager) {
    this.store = store;
    this.tokenManager = tokenManager;
    this.asymmetricKeyGenerator = asymmetricKeyGenerator;
    this.objectMapper = typeManager.getMapper();
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    Preconditions.checkArgument(
        ReferenceApiUtils.validateRequest(exchange, HttpMethods.POST, PATH));

    UUID jobId = ReferenceApiUtils.validateJobId(exchange.getRequestHeaders(), tokenManager);

    // Encrypted job initiation flow
    //   - Validate auth data is present in cookies
    //   - Set Job to state pending assignment
    //   - Wait for a worker to be assigned
    //   - Once worker assigned, grab worker key to encrypt auth data from cookies
    //   - Update job with auth data

    // Lookup job
    PortabilityJob job = store.findJob(jobId);
    Preconditions.checkNotNull(job, "existing job not found for jobId: %s", jobId);
    // Validate job
    String exportService = job.exportService();
    Preconditions.checkState(!Strings.isNullOrEmpty(exportService), "Export service is invalid");
    String importService = job.importService();
    Preconditions.checkState(!Strings.isNullOrEmpty(importService), "Import service is invalid");

    //  Validate auth data is present in cookies
    String exportAuthCookieValue =
        ReferenceApiUtils.getCookie(
            exchange.getRequestHeaders(), JsonKeys.EXPORT_AUTH_DATA_COOKIE_KEY);
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(exportAuthCookieValue), "Export auth cookie required");

    String importAuthCookieValue =
        ReferenceApiUtils.getCookie(
            exchange.getRequestHeaders(), JsonKeys.IMPORT_AUTH_DATA_COOKIE_KEY);
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(importAuthCookieValue), "Import auth cookie required");

    // We have the data, now update to 'pending worker assignment' so a worker may be assigned

    // Job auth data
    JobAuthorization jobAuthorization =
        job.jobAuthorization().toBuilder().setState(JobAuthorization.State.CREDS_AVAILABLE).build();

    job = job.toBuilder().setAndValidateJobAuthorization(jobAuthorization).build();
    store.updateJob(jobId, job);
    logger.debug("Updated job {} to CREDS_AVAILABLE", jobId);

    // Loop until the worker updates it to assigned without auth data state, e.g. at that point
    // the worker instance key will be populated
    // TODO: start new thread
    // TODO: implement timeout condition
    // TODO: Handle case where API dies while waiting
    job = store.findJob(jobId);
    while (job == null || job.jobAuthorization().state() != JobAuthorization.State.CREDS_ENCRYPTION_KEY_GENERATED) {
      logger.debug("Waiting for job {} to enter state CREDS_ENCRYPTION_KEY_GENERATED", jobId);
      try {
        Sleeper.DEFAULT.sleep(10000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      job = store.findJob(jobId);
    }

    logger.debug("Got job {} in state CREDS_ENCRYPTION_KEY_GENERATED", jobId);

    Preconditions.checkNotNull(
        job.jobAuthorization().encodedPublicKey(),
        "Expected job "
            + jobId
            + " to have a worker instance's public key after being assigned "
            + "(state CREDS_ENCRYPTION_KEY_GENERATED)");
    Preconditions.checkState(
        job.jobAuthorization().encryptedExportAuthData() == null,
        "Didn't expect job "
            + jobId
            + " to have encrypted export auth data yet in state "
            + "CREDS_ENCRYPTION_KEY_GENERATED");
    Preconditions.checkState(
        job.jobAuthorization().encryptedImportAuthData() == null,
        "Didn't expect job "
            + jobId
            + " to have encrypted import auth data yet in state "
            + "CREDS_ENCRYPTION_KEY_GENERATED");

    // Populate job with auth data from cookies encrypted with worker key
    logger.debug("About to parse worker instance public key: {}", job.jobAuthorization().encodedPublicKey());
    PublicKey publicKey =
        asymmetricKeyGenerator.parse(
            BaseEncoding.base64Url().decode(job.jobAuthorization().encodedPublicKey()));
    logger.debug("Parsed publicKey, has length: {}", publicKey.getEncoded().length);

    // Encrypt the data with the assigned workers PublicKey and persist
    Encrypter crypter = EncrypterFactory.create(publicKey);
    String encryptedExportAuthData = crypter.encrypt(exportAuthCookieValue);
    logger.debug("Encrypted export auth data, has length: {}", encryptedExportAuthData.length());
    String encryptedImportAuthData = crypter.encrypt(importAuthCookieValue);
    logger.debug("Encrypted import auth data, has length: {}", encryptedImportAuthData.length());
    // Populate job with encrypted auth data

    JobAuthorization updatedJobAuthorization = job.jobAuthorization().toBuilder()
        .setEncryptedExportAuthData(encryptedExportAuthData)
        .setEncryptedImportAuthData(encryptedImportAuthData)
        .setState(JobAuthorization.State.CREDS_ENCRYPTED)
        .build();

    job = job.toBuilder().setAndValidateJobAuthorization(updatedJobAuthorization).build();

    logger.debug("Updating job {} from CREDS_ENCRYPTION_KEY_GENERATED to CREDS_ENCRYPTED", jobId);
    store.updateJob(jobId, job);

    DataTransferResponse response =
            new DataTransferResponse(
                job.exportService(),
                job.importService(),
                job.transferDataType(),
                Status.INPROCESS,
                FrontendConstantUrls.URL_COPY_PAGE);
    writeResponse(exchange, response);
  }

  /** Write a response with status to the client. */
  private void writeResponse(HttpExchange exchange, DataTransferResponse response) throws IOException {

    logger.debug("StartCopyHandler, redirecting to: {}", response.getNextUrl());
    // Mark the response as type Json and send
    exchange
        .getResponseHeaders()
        .set(
            HttpHeaders.CONTENT_TYPE, "application/json; charset=" + StandardCharsets.UTF_8.name());
    exchange.sendResponseHeaders(200, 0);

    objectMapper.writeValue(exchange.getResponseBody(), response);
  }
}
