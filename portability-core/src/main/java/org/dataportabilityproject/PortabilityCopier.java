/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.dataportabilityproject.dataModels.ContinuationInformation;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.dataModels.ExportInformation;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.dataModels.Resource;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.auth.AuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortabilityCopier {
  // TODO: Use better monitoring, this is a hack!
  private static final AtomicInteger COPY_ITERATION_COUNTER = new AtomicInteger();
  private static final Logger logger = LoggerFactory.getLogger(PortabilityCopier.class);

  // Start the copy data process
  public static <T extends DataModel> void copyDataType(ServiceProviderRegistry registry,
      PortableDataType dataType,
      String exportService,
      AuthData exportAuthData,
      String importService,
      AuthData importAuthData,
      String jobId) throws IOException {

    Exporter<T> exporter = registry.getExporter(exportService, dataType, jobId, exportAuthData);
    Importer<T> importer = registry.getImporter(importService, dataType, jobId, importAuthData);
    ExportInformation emptyExportInfo =
        new ExportInformation(Optional.empty(), Optional.empty());
    logger.debug("Starting copy job, id: {}, source: {}, destination: {}",
        jobId, exportService, importService);
    copy(exporter, importer, emptyExportInfo);
  }

  private static <T extends DataModel> void copy(
      Exporter<T> exporter,
      Importer<T> importer,
      ExportInformation exportInformation) throws IOException {
    logger.debug("copy iteration: {}", COPY_ITERATION_COUNTER.incrementAndGet());

    // NOTE: order is important bellow, do the import of all the items, then do continuation
    // then do sub resources, this ensures all parents are populated before children get
    // processed.

    logger.debug("Starting export, exportInformation: {}", exportInformation);
    T items = exporter.export(exportInformation);
    logger.debug("Finished export, results: {}", items);

    logger.debug("Starting import");
    // The collection of items can be both containers and items
    importer.importItem(items);
    logger.debug("Finished import");

    ContinuationInformation continuationInfo = items.getContinuationInformation();
    if (null != continuationInfo) {

      // Process the next page of items for the resource
      if (null != continuationInfo.getPaginationInformation()) {
        logger.debug("start off a new copy iteration with pagination info");
        copy(exporter, importer,
            new ExportInformation(
                exportInformation.getResource(), // Resource with additional pages to fetch
                Optional.of(continuationInfo.getPaginationInformation())));
      }

      // Start processing sub-resources
      if (continuationInfo.getSubResources() != null && !continuationInfo.getSubResources().isEmpty()) {
        logger.debug("start off a new copy iterations with a sub resource, size: {}",
            continuationInfo.getSubResources().size());
        for (Resource resource : continuationInfo.getSubResources()) {
          copy(
              exporter,
              importer,
              new ExportInformation(Optional.of(resource), Optional.empty()));
        }
      }
    }
  }
}
