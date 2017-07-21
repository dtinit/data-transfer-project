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

    log("Starting export: %s", exportInformation);
    T items = exporter.export(exportInformation);
    log("Finished export");
    logExportResults(items);

    log("Starting import: %s", exportInformation);
    importer.importItem(items);
    log("Finished import");

    ContinuationInformation continuationInfo = items.getContinuationInformation();
    if (null != continuationInfo) {
      if (null != continuationInfo.getPaginationInformation()) {
        log("start off a new copy iteration with pagination info");
        copy(exporter, importer,
            new ExportInformation(
                exportInformation.getResource(),
                Optional.of(continuationInfo.getPaginationInformation())));
      }

      if (continuationInfo.getSubResources() != null) {
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

  private static <T extends DataModel> void logExportResults(T items) {
    ContinuationInformation continuationInfo = items.getContinuationInformation();
    boolean hasContinuationInfo = (continuationInfo != null);
    boolean hasPaginationInfo = hasContinuationInfo && (null != continuationInfo.getPaginationInformation());
    boolean hasSubResources = hasContinuationInfo && (continuationInfo.getSubResources() != null);
    int subResourceSize = hasSubResources ? continuationInfo.getSubResources().size() : -1;
    log("export results, hasContinuationInfo: %b, hasPaginationInfo: %b, hasSubResources: %b, subResourceSize: %d",
        hasContinuationInfo, hasPaginationInfo,hasSubResources, subResourceSize);
    if(items instanceof CalendarModelWrapper) {
      CalendarModelWrapper model = (CalendarModelWrapper)items;
      log("export results for calendar, cals: %s, events: %s", model.getCalendars().size(), model.getEvents().size());
    } else if (items instanceof MailModelWrapper) {
      MailModelWrapper model = (MailModelWrapper)items;
      log("export results for mail, messages: %s", model.getMessages().size());
    }
  }

  // TODO: Replace with logging framework
  private static void log (String fmt, Object... args) {
    System.out.println(String.format(fmt, args));
  }
}
