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
package org.dataportabilityproject.transfer;

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
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private final SymmetricKeyGenerator symmetricKeyGenerator;

  @Inject
  JobProcessor(
      JobStore store,
      ObjectMapper objectMapper,
      InMemoryDataCopier copier,
      SymmetricKeyGenerator symmetricKeyGenerator) {
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
    Preconditions.checkState(jobAuthorization.state() == JobAuthorization.State.CREDS_ENCRYPTED);

    try {
      logger.debug(
          "Starting copy job, id: {}, source: {}, destination: {}",
          jobId,
          job.exportService(),
          job.importService());

      // Decrypt the encrypted outer symmetric key, which has been encrypted with our public key
      Decrypter workerKeyDecrypter = DecrypterFactory.create(JobMetadata.getKeyPair().getPrivate());
      byte[] encodedOuterSymmetricKey =
          BaseEncoding.base64Url()
              .decode(workerKeyDecrypter.decrypt(jobAuthorization.authSecretKey()));
      SecretKey outerSymmetricKey = symmetricKeyGenerator.parse(encodedOuterSymmetricKey);

      // Decrypt the doubly encrypted export and import credentials, which have been doubly
      // encrypted with two symmetric keys

      // First decrypt with the outer (secondary) encryption key
      Decrypter outerAuthDataDecrypter = DecrypterFactory.create(outerSymmetricKey);
      String singlyEncryptedExportAuthData =
          outerAuthDataDecrypter.decrypt(jobAuthorization.encryptedExportAuthData());
      String singlyEncryptedImportAuthData =
          outerAuthDataDecrypter.decrypt(jobAuthorization.encryptedImportAuthData());

      // Parse the inner (initial) symmetric encryption key that is stored encoded with the
      // jobAuthorization
      byte[] keyBytes = BaseEncoding.base64Url().decode(jobAuthorization.sessionSecretKey());
      SecretKey innerSymmetricKey = symmetricKeyGenerator.parse(keyBytes);

      // Decrypt one more time
      Decrypter innerAuthDataDecrypter = DecrypterFactory.create(innerSymmetricKey);

      String serializedExportAuthData =
          innerAuthDataDecrypter.decrypt(singlyEncryptedExportAuthData);
      AuthData exportAuthData = deSerialize(serializedExportAuthData);

      String serializedImportAuthData =
          innerAuthDataDecrypter.decrypt(singlyEncryptedImportAuthData);
      AuthData importAuthData = deSerialize(serializedImportAuthData);

      // Copy the data
      copier.copy(exportAuthData, importAuthData, jobId);
      logger.debug("Finished copy for jobId: " + jobId);
    } catch (IOException e) {
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

  private AuthData deSerialize(String serialized) throws IOException {
    try {
      return objectMapper.readValue(serialized, AuthData.class);
    } catch (IOException e) {
      throw new IOException("Unable to deserialize AuthData", e);
    }
  }
}
