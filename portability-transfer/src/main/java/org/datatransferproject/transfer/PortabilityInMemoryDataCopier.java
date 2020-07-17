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

import com.google.inject.Provider;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import org.datatransferproject.api.launcher.DtpInternalMetricRecorder;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.spi.transfer.types.CopyException;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.models.ContainerResource;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.errors.ErrorDetail;
import org.datatransferproject.types.transfer.retry.RetryStrategyLibrary;

/** Implementation of {@link InMemoryDataCopier}. */
final class PortabilityInMemoryDataCopier extends PortabilityAbstractInMemoryDataCopier
    implements InMemoryDataCopier {

  private static final AtomicInteger COPY_ITERATION_COUNTER = new AtomicInteger();

  @Inject
  public PortabilityInMemoryDataCopier(
      Provider<Exporter> exporterProvider,
      Provider<Importer> importerProvider,
      Provider<RetryStrategyLibrary> retryStrategyLibraryProvider,
      Monitor monitor,
      IdempotentImportExecutor idempotentImportExecutor,
      DtpInternalMetricRecorder dtpInternalMetricRecorder,
      JobStore jobStore) {
    super(
        exporterProvider,
        importerProvider,
        retryStrategyLibraryProvider,
        monitor,
        idempotentImportExecutor,
        dtpInternalMetricRecorder,
        jobStore);
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
    final int copyIteration = COPY_ITERATION_COUNTER.incrementAndGet();

    // NOTE: order is important below, do the import of all the items, then do continuation
    // then do sub resources, this ensures all parents are populated before children get
    // processed.

    ExportResult<?> exportResult =
        copyIteration(
            jobId, exportAuthData, importAuthData, exportInformation, jobIdPrefix, copyIteration);

    // Import and Export were successful, determine what to do next
    ContinuationData continuationData = exportResult.getContinuationData();

    if (null != continuationData) {
      // Process the next page of items for the resource
      if (null != continuationData.getPaginationData()) {
        monitor.debug(
            () ->
                jobIdPrefix
                    + "Starting off a new copy iteration with pagination info, copy iteration: "
                    + copyIteration);
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
          monitor.debug(
              () ->
                  jobIdPrefix
                      + "Starting off a new copy iteration with a new container resource, copy iteration: "
                      + copyIteration);
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
