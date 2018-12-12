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
import org.datatransferproject.spi.transfer.hooks.JobHooks;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.types.JobAuthorization;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.spi.transfer.security.AuthDataDecryptService;
import org.datatransferproject.spi.transfer.security.SecurityException;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.auth.AuthDataPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.security.PrivateKey;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.lang.String.format;

/**
 * Process a job in two steps: <br>
 * (1) Decrypt the stored credentials, which have been encrypted with this transfer worker's public
 * key<br>
 * (2)Run the copy job
 */
final class JobProcessor {

  private static final Logger logger = LoggerFactory.getLogger(JobProcessor.class);

  private final JobStore store;
  private final JobHooks hooks;
  private final ObjectMapper objectMapper;
  private final InMemoryDataCopier copier;
  private final Set<AuthDataDecryptService> decryptServices;

  @Inject
  JobProcessor(
      JobStore store,
      JobHooks hooks,
      ObjectMapper objectMapper,
      InMemoryDataCopier copier,
      Set<AuthDataDecryptService> decryptServices) {
    this.store = store;
    this.hooks = hooks;
    this.objectMapper = objectMapper;
    this.copier = copier;
    this.decryptServices = decryptServices;
  }

  /** Process our job, whose metadata is available via {@link JobMetadata}. */
  void processJob() {
    boolean success = false;
    UUID jobId = JobMetadata.getJobId();
    hooks.jobStarted(jobId);

    PortabilityJob job = store.findJob(jobId);
    JobAuthorization jobAuthorization = job.jobAuthorization();
    Preconditions.checkState(jobAuthorization.state() == JobAuthorization.State.CREDS_STORED);

    try {
      logger.debug(
          "Starting copy job, id: {}, source: {}, destination: {}",
          jobId,
          job.exportService(),
          job.importService());

      String scheme = jobAuthorization.encryptionScheme();
      AuthDataDecryptService decryptService = getAuthDecryptService(scheme);
      if (decryptService == null) {
        logger.error(
            format(
                "No auth decrypter found for scheme %s while processing job: %s", scheme, jobId));
        return;
      }

      String encrypted = jobAuthorization.encryptedAuthData();
      PrivateKey privateKey = JobMetadata.getKeyPair().getPrivate();
      AuthDataPair pair = decryptService.decrypt(encrypted, privateKey);
      AuthData exportAuthData = objectMapper.readValue(pair.getExportAuthData(), AuthData.class);
      AuthData importAuthData = objectMapper.readValue(pair.getImportAuthData(), AuthData.class);

      Optional<ExportInformation> exportInfo = Optional.ofNullable(job.exportInformation());

      // Copy the data
      copier.copy(exportAuthData, importAuthData, jobId, exportInfo);
      logger.debug("Finished copy for jobId: " + jobId);
      success = true;
    } catch (IOException | SecurityException | CopyException e) {
      logger.error("Error processing jobId: " + jobId, e);
    } finally {
      hooks.jobFinished(jobId, success);
      JobMetadata.reset();
    }
  }

  @Nullable
  private AuthDataDecryptService getAuthDecryptService(String scheme) {
    for (AuthDataDecryptService decryptService : decryptServices) {
      if (decryptService.canHandle(scheme)) {
        return decryptService;
      }
    }
    return null;
  }
}
