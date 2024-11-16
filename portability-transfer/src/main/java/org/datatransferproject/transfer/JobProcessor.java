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
import com.google.inject.Provider;
import com.google.inject.name.Named;

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
import org.datatransferproject.spi.transfer.provider.SignalHandler;
import org.datatransferproject.spi.transfer.provider.SignalRequest;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle;
import org.datatransferproject.spi.transfer.security.AuthDataDecryptService;
import org.datatransferproject.spi.transfer.types.CopyException;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.spi.transfer.types.FailureReasons;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle.EndReason;
import org.datatransferproject.transfer.Annotations.ExportSignalHandler;
import org.datatransferproject.transfer.Annotations.ImportSignalHandler;
import org.datatransferproject.transfer.copier.InMemoryDataCopier;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.auth.AuthDataPair;
import org.datatransferproject.types.transfer.errors.ErrorDetail;
import org.datatransferproject.types.transfer.retry.RetryException;

/**
 * Process a job in two steps: <br>
 * (1) Decrypt the stored credentials, which have been encrypted with this transfer worker's public
 * key<br>
 * (2)Run the copy job
 */
class JobProcessor {
  private final JobStore store;
  private final JobHooks hooks;
  private final ObjectMapper objectMapper;
  private final InMemoryDataCopier copier;
  private final AuthDataDecryptService decryptService;
  private final Provider<SignalHandler> exportSignalHandlerProvider;
  private final Provider<SignalHandler> importSignalHandlerProvider;
  private final Monitor monitor;
  private final DtpInternalMetricRecorder dtpInternalMetricRecorder;
  private final boolean transferSignalEnabled;

  @Inject
  JobProcessor(
      JobStore store,
      JobHooks hooks,
      ObjectMapper objectMapper,
      InMemoryDataCopier copier,
      AuthDataDecryptService decryptService,
      @ExportSignalHandler Provider<SignalHandler> exportSignalHandlerProvider,
      @ImportSignalHandler Provider<SignalHandler> importSignalHandlerProvider,
      @Named("transferSignalEnabled") Boolean transferSignalEnabled,
      Monitor monitor,
      DtpInternalMetricRecorder dtpInternalMetricRecorder) {
    this.store = store;
    this.hooks = hooks;
    this.objectMapper = objectMapper;
    this.copier = copier;
    this.decryptService = decryptService;
    this.exportSignalHandlerProvider = exportSignalHandlerProvider;
    this.importSignalHandlerProvider = importSignalHandlerProvider;
    this.monitor = monitor;
    this.dtpInternalMetricRecorder = dtpInternalMetricRecorder;
    this.transferSignalEnabled = transferSignalEnabled.booleanValue();
  }

  /** Process our job, whose metadata is available via {@link JobMetadata}. */
  void processJob() {
    boolean success = false;
    UUID jobId = JobMetadata.getJobId();
    monitor.debug(() -> format("Begin processing jobId: %s", jobId), EventCode.WORKER_JOB_STARTED);
    AuthData exportAuthData = null;
    AuthData importAuthData = null;

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

      exportAuthData = objectMapper.readValue(pair.getExportAuthData(), AuthData.class);
      importAuthData = objectMapper.readValue(pair.getImportAuthData(), AuthData.class);

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
      sendSignals(jobId, exportAuthData, importAuthData, JobLifeCycle.JOB_STARTED(), monitor);
      copier.copy(exportAuthData, importAuthData, jobId, exportInfo);
      success = true;
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
      // The errors returned by copier.getErrors are those logged by the idempotentImportExecutor
      // and are distinct from the exceptions thrown by copier.copy
      final Collection<ErrorDetail> loggedErrors = copier.getErrors(jobId);
      final int numErrors = loggedErrors.size();
      // success is set to true above if copy returned without throwing
      success &= loggedErrors.isEmpty();
      monitor.debug(
          () -> format("Finished processing jobId: %s with %d error(s).", jobId, numErrors),
          EventCode.WORKER_JOB_FINISHED);
      addErrorsAndMarkJobFinished(jobId, success, loggedErrors);
      hooks.jobFinished(jobId, success);
      JobLifeCycle finalStatus = deriveFinalJobStatus(success);
      sendSignals(jobId, exportAuthData, importAuthData, finalStatus, monitor);
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

  private static JobLifeCycle deriveFinalJobStatus(boolean success) {
    return JobLifeCycle.builder()
      .setState(JobLifeCycle.State.ENDED)
      .setEndReason(success ? EndReason.SUCCESSFULLY_COMPLETED : EndReason.PARTIALLY_COMPLETED)
      .build();
  }

  private void sendSignals(UUID jobId, AuthData exportAuthData, AuthData importAuthData,
    JobLifeCycle jobLifeCycle, Monitor monitor) {
    if(!transferSignalEnabled) {
      monitor.info(() -> "Transfer Signal Disabled.");
      return;
    }

    SignalRequest signalRequest = SignalRequest.builder()
      .setJobId(jobId.toString())
      .setDataType(JobMetadata.getDataType().getDataType())
      .setJobStatus(jobLifeCycle)
      .setExportingService(JobMetadata.getExportService())
      .setImportingService(JobMetadata.getImportService())
      .build();

    try {
      exportSignalHandlerProvider.get().sendSignal(signalRequest, exportAuthData, monitor);
      importSignalHandlerProvider.get().sendSignal(signalRequest, importAuthData, monitor);
    } catch (CopyExceptionWithFailureReason | IOException | RetryException e) {
      monitor.info(() -> "Errored while sending transfer signals.", e);
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
