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

import static java.lang.String.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.io.IOException;
import java.security.PrivateKey;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.types.JobAuthorization;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.spi.cloud.types.PortabilityJob.State;
import org.datatransferproject.spi.transfer.hooks.JobHooks;
import org.datatransferproject.spi.transfer.security.AuthDataDecryptService;
import org.datatransferproject.spi.transfer.security.SecurityException;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.auth.AuthDataPair;
/**
 * Process a job in two steps: <br>
 * (1) Decrypt the stored credentials, which have been encrypted with this transfer worker's public
 * key<br>
 * (2)Run the copy job
 */
final class JobProcessor {

  private final JobStore store;
  private final JobHooks hooks;
  private final ObjectMapper objectMapper;
  private final InMemoryDataCopier copier;
  private final Set<AuthDataDecryptService> decryptServices;
  private final Monitor monitor;

  @Inject
  JobProcessor(
      JobStore store,
      JobHooks hooks,
      ObjectMapper objectMapper,
      InMemoryDataCopier copier,
      Set<AuthDataDecryptService> decryptServices,
      Monitor monitor) {
    this.store = store;
    this.hooks = hooks;
    this.objectMapper = objectMapper;
    this.copier = copier;
    this.decryptServices = decryptServices;
    this.monitor = monitor;
  }

  /** Process our job, whose metadata is available via {@link JobMetadata}. */
  void processJob() {
    boolean success = false;
    UUID jobId = JobMetadata.getJobId();
    monitor.debug(() -> format("Begin processing jobId: %s", jobId));
    markJobStarted(jobId);
    hooks.jobStarted(jobId);

    PortabilityJob job = store.findJob(jobId);
    JobAuthorization jobAuthorization = job.jobAuthorization();

    try {
      monitor.debug(
          () ->
              format(
                  "Starting copy job, id: %s, source: %s, destination: %s",
                  jobId, job.exportService(), job.importService()));

      String scheme = jobAuthorization.encryptionScheme();
      AuthDataDecryptService decryptService = getAuthDecryptService(scheme);
      if (decryptService == null) {
        monitor.severe(
            () ->
                format(
                    "No auth decrypter found for scheme %s while processing job: %s",
                    scheme, jobId));
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
      monitor.debug(() -> "Finished copy for jobId: " + jobId);
      success = true;
    } catch (IOException | SecurityException | CopyException e) {
      monitor.severe(() -> "Error processing jobId: " + jobId, e);
    } finally {
      monitor.debug(() -> "Finished processing jobId: " + jobId);
      markJobFinished(jobId, success);
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

  private void markJobFinished(UUID jobId, boolean success) {
    State state = success ? State.COMPLETE : State.ERROR;
    updateJobState(jobId, state, State.IN_PROGRESS, JobAuthorization.State.CREDS_STORED);
  }

  private void markJobStarted(UUID jobId) {
    updateJobState(jobId, State.IN_PROGRESS, State.NEW, JobAuthorization.State.CREDS_STORED);
  }

  private void updateJobState(UUID jobId, State state, State prevState,
      JobAuthorization.State prevAuthState) {
    PortabilityJob existingJob = store.findJob(jobId);
    PortabilityJob updatedJob = existingJob.toBuilder().setState(state).build();

    try {
      store.updateJob(jobId, updatedJob,
          ((previous, updated) -> {
            Preconditions.checkState(previous.state() == prevState);
            Preconditions.checkState(previous.jobAuthorization().state() == prevAuthState);
          }));
    } catch (IOException e) {
      monitor.debug(() -> format("Could not mark job %s as %s, %s", jobId, state, e));
    }
  }
}
