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

import com.google.inject.Provider;
import java.io.IOException;
import java.time.Clock;
import java.util.LinkedList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import org.dataportabilityproject.spi.transfer.provider.ExportResult;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.provider.ImportResult;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.spi.transfer.types.ContinuationData;
import org.dataportabilityproject.spi.transfer.types.ExportInformation;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.models.ContainerResource;
import org.dataportabilityproject.types.transfer.retry.RetryException;
import org.dataportabilityproject.types.transfer.retry.RetryStrategy;
import org.dataportabilityproject.types.transfer.retry.RetryStrategyLibrary;
import org.dataportabilityproject.types.transfer.retry.RetryingCallable;
import org.dataportabilityproject.types.transfer.retry.SimpleRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link InMemoryDataCopier}.
 */
final class PortabilityInMemoryDataCopier implements InMemoryDataCopier {

  private static final AtomicInteger COPY_ITERATION_COUNTER = new AtomicInteger();
  private static final Logger logger = LoggerFactory.getLogger(PortabilityInMemoryDataCopier.class);

  /**
   * Lazy evaluate exporter and importer as their providers depend on the polled {@code
   * PortabilityJob} which is not available at startup.
   */
  private final Provider<Exporter> exporter;

  private final Provider<Importer> importer;

  private final Provider<RetryStrategyLibrary> retryStrategyLibrary;

  @Inject
  public PortabilityInMemoryDataCopier(Provider<Exporter> exporter, Provider<Importer> importer,
      Provider<RetryStrategyLibrary> retryStrategyLibrary) {
    this.exporter = exporter;
    this.importer = importer;
    this.retryStrategyLibrary = retryStrategyLibrary;
  }

  /**
   * Kicks off transfer job {@code jobId} from {@code exporter} to {@code importer}.
   */
  @Override
  public void copy(AuthData exportAuthData, AuthData importAuthData, UUID jobId)
      throws IOException {
    // Initial copy, starts off the process with no previous paginationData or containerResource
    // information
    Optional<ExportInformation> emptyExportInfo = Optional.empty();
    copyHelper(jobId, exportAuthData, importAuthData, emptyExportInfo);
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
      Optional<ExportInformation> exportInformation) {

    String jobIdPrefix = "Job " + jobId + ": ";
    logger.debug(jobIdPrefix + "copy iteration: {}", COPY_ITERATION_COUNTER.incrementAndGet());

    RetryStrategyLibrary library = retryStrategyLibrary.get();

    // NOTE: order is important below, do the import of all the items, then do continuation
    // then do sub resources, this ensures all parents are populated before children get
    // processed.
    logger.debug(jobIdPrefix + "Starting export");
    CallableExporter callableExporter = new CallableExporter(exporter, jobId, exportAuthData,
        exportInformation);
    RetryingCallable<ExportResult> retryingExporter = new RetryingCallable<>(callableExporter,
        library, Clock.systemUTC());
    ExportResult<?> exportResult;
    try {
      exportResult = retryingExporter.call();
    } catch (RetryException e) {
      logger.warn(jobIdPrefix + "Error happened during export: {}", e);
      return;
    }
    logger.debug(jobIdPrefix + "Finished export");

    logger.debug(jobIdPrefix + "Starting import");
    CallableImporter callableImporter = new CallableImporter(importer, jobId, importAuthData,
        exportResult.getExportedData());
    RetryingCallable<ImportResult> retryingImporter = new RetryingCallable<>(callableImporter,
        library, Clock.systemUTC());
    try {
      retryingImporter.call();
    } catch (RetryException e) {
      logger.warn(jobIdPrefix + "Error happened during import: {}", e);
      return;
    }
    logger.debug(jobIdPrefix + "Finished import");

    // Import and Export were successful, determine what to do next
    ContinuationData continuationData = exportResult.getContinuationData();

    if (null != continuationData) {
      // Process the next page of items for the resource
      if (null != continuationData.getPaginationData()) {
        logger.debug(jobIdPrefix + "Starting off a new copy iteration with pagination info");
        copyHelper(
            jobId,
            exportAuthData,
            importAuthData,
            Optional.of(new ExportInformation(
                continuationData.getPaginationData(),
                exportInformation.get().getContainerResource())));
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
