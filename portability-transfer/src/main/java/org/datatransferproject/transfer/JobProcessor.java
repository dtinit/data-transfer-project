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
package org.datatransferproject.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.RSADecrypter;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.types.JobAuthorization;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.auth.AuthDataPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.util.UUID;

/**
 * Process a job in two steps: <br>
 * (1) Decrypt the stored credentials, which have been encrypted with this transfer worker's public
 * key<br>
 * (2)Run the copy job
 */
final class JobProcessor {

  private static final Logger logger = LoggerFactory.getLogger(JobProcessor.class);

  private final JobStore store;
  private final ObjectMapper objectMapper;
  private final InMemoryDataCopier copier;

  @Inject
  JobProcessor(JobStore store, ObjectMapper objectMapper, InMemoryDataCopier copier) {
    this.store = store;
    this.objectMapper = objectMapper;
    this.copier = copier;
  }

  /** Process our job, whose metadata is available via {@link JobMetadata}. */
  void processJob() {
    UUID jobId = JobMetadata.getJobId();
    logger.debug("Begin processing jobId: {}", jobId);

    PortabilityJob job = store.findJob(jobId);
    JobAuthorization jobAuthorization = job.jobAuthorization();
    Preconditions.checkState(jobAuthorization.state() == JobAuthorization.State.CREDS_STORED);

    try {
      logger.debug(
          "Starting copy job, id: {}, source: {}, destination: {}",
          jobId,
          job.exportService(),
          job.importService());

      // TODO refactor to a service extension
      // Decrypt the data
      RSADecrypter decrypter = new RSADecrypter(JobMetadata.getKeyPair().getPrivate());
      JWEObject object = JWEObject.parse(jobAuthorization.encryptedAuthData());
      object.decrypt(decrypter);
      AuthDataPair pair =
          objectMapper.readValue(object.getPayload().toString(), AuthDataPair.class);

      AuthData exportAuthData = objectMapper.readValue(pair.getExportAuthData(), AuthData.class);
      AuthData importAuthData = objectMapper.readValue(pair.getImportAuthData(), AuthData.class);

      // Copy the data
      copier.copy(exportAuthData, importAuthData, jobId);
      logger.debug("Finished copy for jobId: " + jobId);

    } catch (IOException | ParseException | JOSEException e) {
      logger.error("Error processing jobId: " + jobId, e);
    } finally {
      try {
        store.remove(jobId);
        JobMetadata.reset();
      } catch (IOException e) {
        logger.error("Error removing jobId: " + jobId, e);
      }
    }
  }
}
