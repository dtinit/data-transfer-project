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

import com.google.common.base.Stopwatch;
import com.google.inject.Provider;
import java.io.IOException;
import java.time.Clock;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import org.datatransferproject.api.launcher.DtpInternalMetricRecorder;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.launcher.monitor.events.EventCode;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.models.ContainerResource;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.errors.ErrorDetail;
import org.datatransferproject.types.transfer.retry.RetryException;
import org.datatransferproject.types.transfer.retry.RetryStrategyLibrary;
import org.datatransferproject.types.transfer.retry.RetryingCallable;

/** Implementation of {@link InMemoryDataCopier}. */
final class PortabilityInMemoryDataCopier implements InMemoryDataCopier {

  private static final AtomicInteger COPY_ITERATION_COUNTER = new AtomicInteger();

  /**
   * Lazy evaluate exporter and importer as their providers depend on the polled {@code
   * PortabilityJob} which is not available at startup.
   */
  private final Provider<Exporter> exporterProvider;

  private final Provider<Importer> importerProvider;
  private final IdempotentImportExecutor idempotentImportExecutor;

  private final Provider<RetryStrategyLibrary> retryStrategyLibraryProvider;
  private final Monitor monitor;
  private final DtpInternalMetricRecorder metricRecorder;
  private final JobStore jobStore;

  @Inject
  public PortabilityInMemoryDataCopier(
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

  /** Kicks off transfer job {@code jobId} from {@code exporter} to {@code importer}. */
  @Override
  public Collection<ErrorDetail> copy(
      AuthData exportAuthData,
      AuthData importAuthData,
      UUID jobId,
      Optional<ExportInformation> exportInfo)
      throws IOException, CopyException {
    idempotentImportExecutor.setJobId(jobId);
    return copyHelper(jobId, exportAuthData, importAuthData, exportInfo);
  }

  /**
   * Transfers data from the given {@code exporter} optionally starting at the point specified in
   * the provided {@code exportInformation}. Imports the data using the provided {@code importer}.
   * If there is more data to required to be exported, recursively copies using the specific {@link
   * ExportInformation} to continue the process.
   *
   * @param exportAuthData The auth data for the export
   * @param importAuthData The auth data for the import
   * @param exportInformation Any pagination or resource information to use for subsequent calls.
   */
  private Collection<ErrorDetail> copyHelper(
      UUID jobId,
      AuthData exportAuthData,
      AuthData importAuthData,
      Optional<ExportInformation> exportInformation)
      throws CopyException {

    String jobIdPrefix = "Job " + jobId + ": ";
    final int i = COPY_ITERATION_COUNTER.incrementAndGet();
    monitor.debug(() -> jobIdPrefix + "Copy iteration: " + i);

    RetryStrategyLibrary retryStrategyLibrary = retryStrategyLibraryProvider.get();

    // NOTE: order is important below, do the import of all the items, then do continuation
    // then do sub resources, this ensures all parents are populated before children get
    // processed.
    monitor.debug(() -> jobIdPrefix + "Starting export", EventCode.COPIER_STARTED_EXPORT);
    CallableExporter callableExporter =
        new CallableExporter(
            exporterProvider, jobId, exportAuthData, exportInformation, metricRecorder);
    RetryingCallable<ExportResult> retryingExporter =
        new RetryingCallable<>(callableExporter, retryStrategyLibrary, Clock.systemUTC(), monitor);
    ExportResult<?> exportResult;
    boolean exportSuccess = false;
    Stopwatch exportStopwatch = Stopwatch.createStarted();
    try {
      exportResult = retryingExporter.call();
      exportSuccess = exportResult.getType() != ExportResult.ResultType.ERROR;
    } catch (RetryException | RuntimeException e) {
      throw new CopyException(jobIdPrefix + "Error happened during export", e);
    } finally {
      metricRecorder.exportPageFinished(
          JobMetadata.getDataType(),
          JobMetadata.getExportService(),
          exportSuccess,
          exportStopwatch.elapsed());
    }
    monitor.debug(() -> jobIdPrefix + "Finished export", EventCode.COPIER_FINISHED_EXPORT);

    if (exportResult.getExportedData() != null) {
      monitor.debug(() -> jobIdPrefix + "Starting import", EventCode.COPIER_STARTED_IMPORT);
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
              callableImporter, retryStrategyLibrary, Clock.systemUTC(), monitor);
      boolean importSuccess = false;
      Stopwatch importStopwatch = Stopwatch.createStarted();
      try {
        ImportResult importResult = retryingImporter.call();
        importSuccess = importResult.getType() == ImportResult.ResultType.OK;
        if (importSuccess) {
          try {
            jobStore.addCounts(jobId, importResult.getCounts().orElse(null));
          } catch (IOException e) {
            monitor.debug(() -> jobIdPrefix + "Unable to add counts to job: ", e);
          }
        }
      } catch (RetryException | RuntimeException e) {
        monitor.severe(() -> format("Got error importing data: %s", e), e);
        if (e.getClass() == RetryException.class
            && e.getCause().getClass() == DestinationMemoryFullException.class) {
          throw (DestinationMemoryFullException) e.getCause();
        }
      } finally {
        metricRecorder.importPageFinished(
            JobMetadata.getDataType(),
            JobMetadata.getImportService(),
            importSuccess,
            importStopwatch.elapsed());
      }
      monitor.debug(() -> jobIdPrefix + "Finished import", EventCode.COPIER_FINISHED_IMPORT);
    }

    // Import and Export were successful, determine what to do next
    ContinuationData continuationData = exportResult.getContinuationData();

    if (null != continuationData) {
      // Process the next page of items for the resource
      if (null != continuationData.getPaginationData()) {
        monitor.debug(() -> jobIdPrefix + "Starting off a new copy iteration with pagination info");
        copyHelper(
            jobId,
            exportAuthData,
            importAuthData,
            Optional.of(
                new ExportInformation(
                    continuationData.getPaginationData(),
                    exportInformation.isPresent()
                        ? exportInformation.get().getContainerResource()
                        : null)));
      }

      // Start processing sub-resources
      if (continuationData.getContainerResources() != null
          && !continuationData.getContainerResources().isEmpty()) {
        for (ContainerResource resource : continuationData.getContainerResources()) {
          copyHelper(
              jobId,
              exportAuthData,
              importAuthData,
              Optional.of(new ExportInformation(null, resource)));
        }
      }
    }
    return idempotentImportExecutor.getErrors();
  }
}
