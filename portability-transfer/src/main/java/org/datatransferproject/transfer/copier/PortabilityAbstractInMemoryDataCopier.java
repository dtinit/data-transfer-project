/*
 * Copyright 2020 The Data Transfer Project Authors.
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
package org.datatransferproject.transfer.copier;

import com.google.common.base.Stopwatch;
import com.google.inject.Provider;
import java.io.IOException;
import java.time.Clock;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.datatransferproject.api.launcher.DtpInternalMetricRecorder;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.launcher.monitor.events.EventCode;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.spi.transfer.types.CopyException;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.transfer.CallableExporter;
import org.datatransferproject.transfer.CallableImporter;
import org.datatransferproject.transfer.JobMetadata;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.errors.ErrorDetail;
import org.datatransferproject.types.transfer.retry.RetryException;
import org.datatransferproject.types.transfer.retry.RetryStrategyLibrary;
import org.datatransferproject.types.transfer.retry.RetryingCallable;



public abstract class PortabilityAbstractInMemoryDataCopier implements InMemoryDataCopier {
  /**
   * Lazy evaluate exporter and importer as their providers depend on the polled {@code
   * PortabilityJob} which is not available at startup.
   */
  protected final Provider<Exporter> exporterProvider;

  protected final Provider<Importer> importerProvider;
  protected final IdempotentImportExecutor idempotentImportExecutor;
  protected final Provider<RetryStrategyLibrary> retryStrategyLibraryProvider;
  protected final Monitor monitor;
  protected final DtpInternalMetricRecorder metricRecorder;
  protected final JobStore jobStore;

  public PortabilityAbstractInMemoryDataCopier(
      Provider<Exporter> exporterProvider,
      Provider<Importer> importerProvider,
      Provider<RetryStrategyLibrary> retryStrategyLibraryProvider,
      Monitor monitor,
      IdempotentImportExecutor idempotentImportExecutor,
      DtpInternalMetricRecorder dtpInternalMetricRecorder,
      JobStore jobStore) {
    this.exporterProvider = exporterProvider;
    this.importerProvider = importerProvider;
    this.retryStrategyLibraryProvider = retryStrategyLibraryProvider;
    this.monitor = monitor;
    this.idempotentImportExecutor = idempotentImportExecutor;
    this.metricRecorder = dtpInternalMetricRecorder;
    this.jobStore = jobStore;
  }

  public abstract void resetCopyIterationCounter();

  /** Kicks off transfer job {@code jobId} from {@code exporter} to {@code importer}. */
  @Override
  public abstract Collection<ErrorDetail> copy(
      AuthData exportAuthData,
      AuthData importAuthData,
      UUID jobId,
      Optional<ExportInformation> exportInfo)
      throws IOException, CopyException;

  protected ExportResult<?> copyIteration(
      UUID jobId,
      AuthData exportAuthData,
      AuthData importAuthData,
      Optional<ExportInformation> exportInformation,
      String jobIdPrefix,
      int copyIteration)
      throws CopyException {
    monitor.debug(() -> jobIdPrefix + "Copy iteration: " + copyIteration);

    RetryStrategyLibrary retryStrategyLibrary = retryStrategyLibraryProvider.get();

    monitor.debug(
            () -> jobIdPrefix + "Starting export, copy iteration: " + copyIteration,
            EventCode.COPIER_STARTED_EXPORT);
    CallableExporter callableExporter =
            new CallableExporter(
                    exporterProvider, jobId, exportAuthData, exportInformation, metricRecorder);
    RetryingCallable<ExportResult> retryingExporter =
            new RetryingCallable<>(callableExporter, retryStrategyLibrary, Clock.systemUTC(), monitor, JobMetadata.getDataType(), JobMetadata.getExportService());
    ExportResult<?> exportResult;
    boolean exportSuccess = false;
    Stopwatch exportStopwatch = Stopwatch.createStarted();
    try {
      exportResult = retryingExporter.call();
      exportSuccess = exportResult.getType() != ExportResult.ResultType.ERROR;
    } catch (RetryException | RuntimeException e) {
      if (e.getClass() == RetryException.class
              && CopyExceptionWithFailureReason.class.isAssignableFrom(e.getCause().getClass())) {
        throw (CopyExceptionWithFailureReason) e.getCause();
      }
      throw new CopyException(jobIdPrefix + "Error happened during export", e);
    } finally {
      metricRecorder.exportPageFinished(
              JobMetadata.getDataType(),
              JobMetadata.getExportService(),
              exportSuccess,
              exportStopwatch.elapsed());
    }
    monitor.debug(
            () -> jobIdPrefix + "Finished export, copy iteration: " + copyIteration,
            EventCode.COPIER_FINISHED_EXPORT);

    if (exportResult.getExportedData() != null) {
      monitor.debug(
              () -> jobIdPrefix + "Starting import, copy iteration: " + copyIteration,
              EventCode.COPIER_STARTED_IMPORT);
      CallableImporter callableImporter =
              new CallableImporter(
                      importerProvider,
                      jobId,
                      idempotentImportExecutor,
                      importAuthData,
                      exportResult.getExportedData(),
                      metricRecorder);
      RetryingCallable<ImportResult> retryingImporter =
              new RetryingCallable<>(
                      callableImporter, retryStrategyLibrary, Clock.systemUTC(), monitor, JobMetadata.getDataType(), JobMetadata.getImportService());
      boolean importSuccess = false;
      Stopwatch importStopwatch = Stopwatch.createStarted();
      try {
        ImportResult importResult = retryingImporter.call();
        importSuccess = importResult.getType() == ImportResult.ResultType.OK;
        if (importSuccess) {
          try {
            jobStore.addCounts(jobId, importResult.getCounts().orElse(null));
            jobStore.addBytes(jobId, importResult.getBytes().orElse(null));
          } catch (IOException e) {
            monitor.debug(() -> jobIdPrefix + "Unable to add counts to job: ", e);
          }
        }
      } catch (RetryException | RuntimeException e) {
        if (e.getClass() == RetryException.class
                && CopyExceptionWithFailureReason.class.isAssignableFrom(e.getCause().getClass())) {
          throw (CopyExceptionWithFailureReason) e.getCause();
        }
        throw new CopyException(jobIdPrefix + "Error happened during import", e);
      } finally {
        metricRecorder.importPageFinished(
                JobMetadata.getDataType(),
                JobMetadata.getImportService(),
                importSuccess,
                importStopwatch.elapsed());
      }
      monitor.debug(
              () -> jobIdPrefix + "Finished import, copy iteration: " + copyIteration,
              EventCode.COPIER_FINISHED_IMPORT);
    }
    return exportResult;
  }
}
