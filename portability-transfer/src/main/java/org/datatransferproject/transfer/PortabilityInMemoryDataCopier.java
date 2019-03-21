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

import com.google.inject.Provider;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.launcher.monitor.events.EventCode;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.models.ContainerResource;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.retry.RetryException;
import org.datatransferproject.types.transfer.retry.RetryStrategyLibrary;
import org.datatransferproject.types.transfer.retry.RetryingCallable;

import javax.inject.Inject;
import java.io.IOException;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Implementation of {@link InMemoryDataCopier}. */
final class PortabilityInMemoryDataCopier implements InMemoryDataCopier {

  private static final AtomicInteger COPY_ITERATION_COUNTER = new AtomicInteger();

  /**
   * Lazy evaluate exporter and importer as their providers depend on the polled {@code
   * PortabilityJob} which is not available at startup.
   */
  private final Provider<Exporter> exporterProvider;

  private final Provider<Importer> importerProvider;
  private final InMemoryIdempotentImportExecutor inMemoryIdempotentImportExecutor;

  private final Provider<RetryStrategyLibrary> retryStrategyLibraryProvider;
  private final Monitor monitor;

  @Inject
  public PortabilityInMemoryDataCopier(
      Provider<Exporter> exporterProvider,
      Provider<Importer> importerProvider,
      Provider<RetryStrategyLibrary> retryStrategyLibraryProvider,
      Monitor monitor) {
    this.exporterProvider = exporterProvider;
    this.importerProvider = importerProvider;
    this.retryStrategyLibraryProvider = retryStrategyLibraryProvider;
    this.monitor = monitor;
    this.inMemoryIdempotentImportExecutor = new InMemoryIdempotentImportExecutor(monitor);
  }

  /** Kicks off transfer job {@code jobId} from {@code exporter} to {@code importer}. */
  @Override
   public void copy(AuthData exportAuthData, AuthData importAuthData, UUID jobId, Optional<ExportInformation> exportInfo)
      throws IOException, CopyException {
    copyHelper(jobId, exportAuthData, importAuthData, exportInfo);
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
  private void copyHelper(
      UUID jobId,
      AuthData exportAuthData,
      AuthData importAuthData,
      Optional<ExportInformation> exportInformation) throws CopyException {

    String jobIdPrefix = "Job " + jobId + ": ";
    monitor.debug(
        () -> jobIdPrefix + "Copy iteration: " + COPY_ITERATION_COUNTER.incrementAndGet());

    RetryStrategyLibrary retryStrategyLibrary = retryStrategyLibraryProvider.get();

    // NOTE: order is important below, do the import of all the items, then do continuation
    // then do sub resources, this ensures all parents are populated before children get
    // processed.
    monitor.debug(() -> jobIdPrefix + "Starting export", EventCode.COPIER_STARTED_EXPORT);
    CallableExporter callableExporter =
        new CallableExporter(exporterProvider, jobId, exportAuthData, exportInformation);
    RetryingCallable<ExportResult> retryingExporter =
        new RetryingCallable<>(callableExporter, retryStrategyLibrary, Clock.systemUTC(), monitor);
    ExportResult<?> exportResult;
    try {
      exportResult = retryingExporter.call();
    } catch (RetryException e) {
      throw new CopyException(jobIdPrefix + "Error happened during export", e);
    }
    monitor.debug(() -> jobIdPrefix + "Finished export", EventCode.COPIER_FINISHED_EXPORT);

    if (exportResult.getExportedData() != null) {
      monitor.debug(() -> jobIdPrefix + "Starting import", EventCode.COPIER_STARTED_IMPORT);
      CallableImporter callableImporter =
          new CallableImporter(
              importerProvider,
              jobId,
              inMemoryIdempotentImportExecutor,
              importAuthData,
              exportResult.getExportedData());
      RetryingCallable<ImportResult> retryingImporter =
          new RetryingCallable<>(
              callableImporter, retryStrategyLibrary, Clock.systemUTC(), monitor);
      try {
        retryingImporter.call();
      } catch (RetryException e) {
        throw new CopyException(jobIdPrefix + "Error happened during import", e);
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
            Optional.of(new ExportInformation(
                continuationData.getPaginationData(),
                exportInformation.isPresent()
                    ? exportInformation.get().getContainerResource()
                    : null)));
      }

      // Start processing sub-resources
      if (continuationData.getContainerResources() != null
          && !continuationData.getContainerResources().isEmpty()) {
        for (ContainerResource resource : continuationData.getContainerResources()) {
          copyHelper(jobId, exportAuthData, importAuthData,
              Optional.of(new ExportInformation(null, resource)));
        }
      }
    }
  }
}
