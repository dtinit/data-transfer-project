package org.dataportabilityproject.worker;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.dataportabilityproject.spi.transfer.InMemoryTransferCopier;
import org.dataportabilityproject.spi.transfer.provider.ExportResult;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.spi.transfer.provider.TransferServiceProviderRegistry;
import org.dataportabilityproject.spi.transfer.types.ContinuationData;
import org.dataportabilityproject.spi.transfer.types.ExportInformation;
import org.dataportabilityproject.types.transfer.PortableType;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.models.ContainerResource;
import org.dataportabilityproject.types.transfer.models.DataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of InMemoryTransferCopier.
 */
public class PortabilityInMemoryTransferCopier implements InMemoryTransferCopier {

  private static final AtomicInteger COPY_ITERATION_COUNTER = new AtomicInteger();
  private static final Logger logger = LoggerFactory.getLogger(PortabilityInMemoryTransferCopier.class);

  @Override
  public void copyDataType(TransferServiceProviderRegistry registry,
      PortableType dataType,
      String exportService,
      AuthData exportAuthData,
      String importService,
      AuthData importAuthData,
      String jobId) throws IOException {

    Exporter<AuthData, DataModel> exporter = registry.getExporter(exportService, dataType);
    Importer<AuthData, DataModel> importer = registry.getImporter(importService, dataType);

    ExportInformation emptyExportInfo = new ExportInformation(null, null);
    logger.debug("Starting copy job, id: {}, source: {}, destination: {}", jobId, exportService,
        importService);
    copy(exporter, importer, exportAuthData, importAuthData, emptyExportInfo);

  }

  private void copy(
      Exporter<AuthData, DataModel> exporter,
      Importer<AuthData, DataModel> importer,
      AuthData exportAuthData,
      AuthData importAuthData,
      ExportInformation exportInformation) throws IOException {

    logger.debug("copy iteration: {}", COPY_ITERATION_COUNTER.incrementAndGet());

    // NOTE: order is important below, do the import of all the items, then do continuation
    // then do sub resources, this ensures all parents are populated before children get
    // processed.
    logger.debug("Starting export, ExportInformation: {}", exportInformation);
    ExportResult<DataModel> exportResult = exporter.export(exportAuthData, exportInformation);
    logger.debug("Finished export, results: {}", exportResult);

    logger.debug("Starting import");
    importer.importItem(importAuthData, exportResult.getExportedData());
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
