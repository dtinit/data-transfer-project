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
package org.dataportabilityproject.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.io.BaseEncoding;
import com.google.inject.Inject;

import java.io.IOException;
import java.util.UUID;

import javax.crypto.SecretKey;
import org.dataportabilityproject.security.Decrypter;
import org.dataportabilityproject.security.DecrypterFactory;
import org.dataportabilityproject.security.SymmetricKeyGenerator;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.JobAuthorization;
import org.dataportabilityproject.spi.cloud.types.PortabilityJob;
import org.dataportabilityproject.spi.transfer.InMemoryDataCopier;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process a job in two steps: <br>
 * (1) Decrypt the stored credentials, which have been encrypted with this worker's public key<br>
 * (2)Run the copy job
 */
final class JobProcessor {
  private static final Logger logger = LoggerFactory.getLogger(JobProcessor.class);

  private final JobStore store;
  private final ObjectMapper objectMapper;
  private final InMemoryDataCopier copier;
  private final SymmetricKeyGenerator symmetricKeyGenerator;

  @Inject
  JobProcessor(JobStore store, ObjectMapper objectMapper, InMemoryDataCopier copier, SymmetricKeyGenerator symmetricKeyGenerator) {
    this.store = store;
    this.objectMapper = objectMapper;
    this.copier = copier;
    this.symmetricKeyGenerator = symmetricKeyGenerator;
  }

  /** Process our job, whose metadata is available via {@link JobMetadata}. */
  void processJob() {
    UUID jobId = JobMetadata.getJobId();
    logger.debug("Begin processing jobId: {}", jobId);

    PortabilityJob job = store.findJob(jobId);
    JobAuthorization jobAuthorization = job.jobAuthorization();
    Preconditions.checkState(
        jobAuthorization.state() == JobAuthorization.State.CREDS_ENCRYPTED);

    try {
      logger.debug(
          "Starting copy job, id: {}, source: {}, destination: {}",
          jobId,
          JobMetadata.getExportService(),
          JobMetadata.getImportService());

      // Decrypt export and import auth data, which have been encrypted with session key and then
      // with our (worker) public key
      Decrypter workerKeyDecrypter = DecrypterFactory.create(JobMetadata.getKeyPair().getPrivate());
      String encodedSessionKey = jobAuthorization.encodedSessionKey();
      SecretKey sessionKey =
          symmetricKeyGenerator.parse(BaseEncoding.base64Url().decode(encodedSessionKey));
      Decrypter sessionKeyDecrypter = DecrypterFactory.create(sessionKey);

      // Export auth data
      String serializedExportAuthData =
          workerKeyDecrypter.decrypt(jobAuthorization.encryptedExportAuthData());
      serializedExportAuthData = sessionKeyDecrypter.decrypt(serializedExportAuthData);
      AuthData exportAuthData = deSerialize(serializedExportAuthData);

      // Import auth data
      String serializedImportAuthData =
          workerKeyDecrypter.decrypt(jobAuthorization.encryptedImportAuthData());
      serializedImportAuthData = sessionKeyDecrypter.decrypt(serializedImportAuthData);
      AuthData importAuthData = deSerialize(serializedImportAuthData);

      // Copy the data
      copier.copy(exportAuthData, importAuthData, jobId);
    } catch (IOException e) {
      logger.error("Error processing jobId: {}" + jobId, e);
    } finally {
      store.removeData(jobId);
    }
  }

  private AuthData deSerialize(String serialized) throws IOException {
    try {
      return objectMapper.readValue(serialized, AuthData.class);
    } catch (IOException e) {
      throw new IOException("Unable to deserialize AuthData", e);
    }
  }
}
