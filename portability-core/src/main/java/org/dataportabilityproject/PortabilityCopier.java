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
import org.dataportabilityproject.dataModels.calendar.CalendarModelWrapper;
import org.dataportabilityproject.dataModels.mail.MailModelWrapper;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.auth.AuthData;

public class PortabilityCopier {
  // TODO: Use better monitoring, this is a hack!
  private static final AtomicInteger COPY_ITERATION_COUNTER = new AtomicInteger();

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
    log("Starting copy job, id: %s, source: %s, destination: %s",
        jobId, exportService, importService);
    copy(exporter, importer, emptyExportInfo);
  }

  private static <T extends DataModel> void copy(
      Exporter<T> exporter,
      Importer<T> importer,
      ExportInformation exportInformation) throws IOException {
    log("copy iteration: %d", COPY_ITERATION_COUNTER.incrementAndGet());

    // NOTE: order is important bellow, do the import of all the items, then do continuation
    // then do sub resources, this ensures all parents are populated before children get
    // processed.

    log("Starting export, exportInformation: %s", exportInformation);
    T items = exporter.export(exportInformation);
    log("Finished export, results: %s", items);

    log("Starting import");
    // The collection of items can be both containers and items
    importer.importItem(items);
    log("Finished import");

    ContinuationInformation continuationInfo = items.getContinuationInformation();
    if (null != continuationInfo) {

      // Process the next page of items for the resource
      if (null != continuationInfo.getPaginationInformation()) {
        log("start off a new copy iteration with pagination info");
        copy(exporter, importer,
            new ExportInformation(
                exportInformation.getResource(), // Resource with additional pages to fetch
                Optional.of(continuationInfo.getPaginationInformation())));
      }

      // Start processing sub-resources
      if (continuationInfo.getSubResources() != null && !continuationInfo.getSubResources().isEmpty()) {
        log("start off a new copy iterations with a sub resource, size: %d",
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

  // TODO: Replace with logging framework
  private static void log (String fmt, Object... args) {
    System.out.println(String.format("PortabilityCopier: " + fmt, args));
  }
}
