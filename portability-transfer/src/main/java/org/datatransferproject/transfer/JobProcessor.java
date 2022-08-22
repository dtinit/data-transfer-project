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
import com.google.common.base.Strings;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import org.datatransferproject.api.launcher.DtpInternalMetricRecorder;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.launcher.monitor.events.EventCode;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.types.JobAuthorization;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.spi.cloud.types.PortabilityJob.State;
import org.datatransferproject.spi.transfer.hooks.JobHooks;
import org.datatransferproject.spi.transfer.security.AuthDataDecryptService;
import org.datatransferproject.spi.transfer.types.CopyException;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.spi.transfer.types.FailureReasons;
import org.datatransferproject.transfer.copier.InMemoryDataCopier;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.auth.AuthDataPair;
import org.datatransferproject.types.transfer.errors.ErrorDetail;

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
  private final AuthDataDecryptService decryptService;
  private final Monitor monitor;
  private final DtpInternalMetricRecorder dtpInternalMetricRecorder;

  @Inject
  JobProcessor(
      JobStore store,
      JobHooks hooks,
      ObjectMapper objectMapper,
      InMemoryDataCopier copier,
      AuthDataDecryptService decryptService,
      Monitor monitor,
      DtpInternalMetricRecorder dtpInternalMetricRecorder) {
    this.store = store;
    this.hooks = hooks;
    this.objectMapper = objectMapper;
    this.copier = copier;
    this.decryptService = decryptService;
    this.monitor = monitor;
    this.dtpInternalMetricRecorder = dtpInternalMetricRecorder;
  }

  /** Process our job, whose metadata is available via {@link JobMetadata}. */
  void processJob() {
    boolean success = false;
    UUID jobId = JobMetadata.getJobId();
    monitor.debug(() -> format("Begin processing jobId: %s", jobId), EventCode.WORKER_JOB_STARTED);

    Collection<ErrorDetail> errors = null;

    try {
      markJobStarted(jobId);
      hooks.jobStarted(jobId);

      PortabilityJob job = store.findJob(jobId);
      JobAuthorization jobAuthorization = job.jobAuthorization();

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
      byte[] encodedPrivateKey = JobMetadata.getPrivateKey();
      AuthDataPair pair = decryptService.decrypt(encrypted, encodedPrivateKey);
      AuthData exportAuthData = objectMapper.readValue(pair.getExportAuthData(), AuthData.class);
      AuthData importAuthData = objectMapper.readValue(pair.getImportAuthData(), AuthData.class);

      String exportInfoStr = job.exportInformation();
      Optional<ExportInformation> exportInfo = Optional.empty();
      if (!Strings.isNullOrEmpty(exportInfoStr)) {
        exportInfo = Optional.ofNullable(objectMapper.readValue(exportInfoStr, ExportInformation.class));
      }

      // Copy the data
      dtpInternalMetricRecorder.startedJob(
          JobMetadata.getDataType(),
          JobMetadata.getExportService(),
          JobMetadata.getImportService());
      JobMetadata.getStopWatch().start();
      errors = copier.copy(exportAuthData, importAuthData, jobId, exportInfo);
      final int numErrors = errors.size();
      monitor.debug(
          () -> format("Finished copy for jobId: %s with %d error(s).", jobId, numErrors));
      success = errors.isEmpty();
    } catch (CopyExceptionWithFailureReason e) {
      String failureReason = e.getFailureReason();
      if (failureReason.contains(FailureReasons.DESTINATION_FULL.toString())) {
        monitor.info(() -> "The remaining storage in the user's account is not enough to perform this operation.", e);
      } else if (failureReason.contains(FailureReasons.INVALID_TOKEN.toString())  ||
              failureReason.contains(FailureReasons.SESSION_INVALIDATED.toString())  ||
              failureReason.contains(FailureReasons.UNCONFIRMED_USER.toString())  ||
              failureReason.contains(FailureReasons.USER_CHECKPOINTED.toString())) {
        monitor.info(() -> "Got token error", e);
      } else {
        monitor.severe(
                () ->
                        format(
                                "Error with failure code '%s' while processing jobId: %s",
                                failureReason, jobId),
                e,
                EventCode.WORKER_JOB_ERRORED);
      }
      addFailureReasonToJob(jobId, failureReason);
    } catch (IOException | CopyException | RuntimeException e) {
      monitor.severe(() -> "Error processing jobId: " + jobId, e, EventCode.WORKER_JOB_ERRORED);
    } finally {
      monitor.debug(() -> "Finished processing jobId: " + jobId, EventCode.WORKER_JOB_FINISHED);
      addErrorsAndMarkJobFinished(jobId, success, errors);
      hooks.jobFinished(jobId, success);
      dtpInternalMetricRecorder.finishedJob(
          JobMetadata.getDataType(),
          JobMetadata.getExportService(),
          JobMetadata.getImportService(),
          success,
          JobMetadata.getStopWatch().elapsed());
      monitor.flushLogs();
      JobMetadata.reset();
    }
  }

  @Nullable
  private AuthDataDecryptService getAuthDecryptService(String scheme) {
    if (decryptService.canHandle(scheme)) {
      return decryptService;
    }
    return null;
  }

  private void addErrorsAndMarkJobFinished(
      UUID jobId, boolean success, Collection<ErrorDetail> errors) {
    try {
      store.addErrorsToJob(jobId, errors);
    } catch (IOException | RuntimeException e) {
      success = false;
      monitor.severe(() -> "Problem adding errors to JobStore", e);
    }
    try {
      store.markJobAsFinished(jobId, success ? State.COMPLETE : State.ERROR);
    } catch (IOException e) {
      monitor.severe(() -> format("Could not mark job %s as finished.", jobId));
    }
  }

  private void addFailureReasonToJob(UUID jobId, String failureReason) {
    try {
      store.addFailureReasonToJob(jobId, failureReason);
    } catch (IOException e) {
      monitor.severe(() -> "Problem adding failure reason to JobStore", e);
    }
  }

  private void markJobStarted(UUID jobId) {
    try {
      store.markJobAsStarted(jobId);
    } catch (IOException e) {
      monitor.severe(() -> format("Could not mark job %s as %s", jobId, State.IN_PROGRESS), e);
    }
  }
}
