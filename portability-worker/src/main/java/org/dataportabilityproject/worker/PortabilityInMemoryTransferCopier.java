/*
 * Copyright 2018 The Data-Portability Project Authors.
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
package org.dataportabilityproject.worker;

import org.dataportabilityproject.spi.transfer.InMemoryTransferCopier;
import org.dataportabilityproject.spi.transfer.provider.ExportResult;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.spi.transfer.provider.TransferServiceProviderRegistry;
import org.dataportabilityproject.spi.transfer.types.ContinuationData;
import org.dataportabilityproject.spi.transfer.types.ExportInformation;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.models.ContainerResource;
import org.dataportabilityproject.types.transfer.models.DataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of InMemoryTransferCopier.
 */
public class PortabilityInMemoryTransferCopier implements InMemoryTransferCopier {

    private static final AtomicInteger COPY_ITERATION_COUNTER = new AtomicInteger();
    private static final Logger logger = LoggerFactory
            .getLogger(PortabilityInMemoryTransferCopier.class);

    private final TransferServiceProviderRegistry registry;

    public PortabilityInMemoryTransferCopier(TransferServiceProviderRegistry registry) {
        this.registry = registry;
    }

    /**
     * Kicks off the transfer job for the datatype from the export service to the import service.
     *
     * @param dataType       The data type to transfer
     * @param exportService  The export service to transfer from
     * @param exportAuthData The auth data for the export service
     * @param importService  The import service to transfer to
     * @param importAuthData The auth data for the import service
     * @param jobId          The job id representing this copy
     */
    @Override
    public void copyDataType(String dataType,
                             String exportService,
                             AuthData exportAuthData,
                             String importService,
                             AuthData importAuthData,
                             UUID jobId) throws IOException {

        Exporter<? extends AuthData, ? extends DataModel> exporter = registry.getExporter(exportService, dataType);
        Importer<? extends AuthData, ? extends DataModel> importer = registry.getImporter(importService, dataType);

        logger.debug("Starting copy job, id: {}, source: {}, destination: {}", jobId, exportService,
                importService);

        // Initial copy, starts off the process with no previous paginationData or containerResource information
        ExportInformation emptyExportInfo = new ExportInformation(null, null);
        copy(exporter, importer, exportAuthData, importAuthData, emptyExportInfo);

    }

    /**
     * Transfers data fropm the given {@code exporter} optionally starting at the point specified in
     * the provided {@code exportInformation}. Imports the data using the provided {@code importer}.
     * If there is more data to required to be exported, recursively copies using the specific {@link
     * ExportInformation} to continue the process.
     *
     * @param exporter          The exporter to use
     * @param importer          The importer to use
     * @param exportAuthData    The auth data for the export
     * @param importAuthData    The auth data for the import
     * @param exportInformation Any pagination or resource information to use for subsequent calls.
     */
    private void copy(
            Exporter exporter,
            Importer importer,
            AuthData exportAuthData,
            AuthData importAuthData,
            ExportInformation exportInformation) throws IOException {

        logger.debug("copy iteration: {}", COPY_ITERATION_COUNTER.incrementAndGet());

        // NOTE: order is important below, do the import of all the items, then do continuation
        // then do sub resources, this ensures all parents are populated before children get
        // processed.
        logger.debug("Starting export, ExportInformation: {}", exportInformation);
        ExportResult<?> exportResult = exporter.export(exportAuthData, exportInformation);
        logger.debug("Finished export, results: {}", exportResult);

        logger.debug("Starting import");
        // TODO, use job Id?
        importer.importItem("1", importAuthData, exportResult.getExportedData());
        logger.debug("Finished import");

        ContinuationData continuationData = (ContinuationData) exportResult.getContinuationData();

        if (null != continuationData) {
            // Process the next page of items for the resource
            if (null != continuationData.getPaginationData()) {
                logger.debug("start off a new copy iteration with pagination info");
                copy(exporter, importer, exportAuthData, importAuthData,
                        new ExportInformation(continuationData.getPaginationData(),
                                exportInformation.getContainerResource()));
            }

            // Start processing sub-resources
            if (continuationData.getContainerResources() != null && !continuationData
                    .getContainerResources().isEmpty()) {
                for (ContainerResource resource : continuationData.getContainerResources()) {
                    copy(exporter, importer, exportAuthData, importAuthData,
                            new ExportInformation(null, resource));
                }
            }
        }

    }
}
