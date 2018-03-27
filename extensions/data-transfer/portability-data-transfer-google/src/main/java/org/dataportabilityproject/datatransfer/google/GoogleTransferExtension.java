package org.dataportabilityproject.datatransfer.google;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.datatransfer.google.calendar.GoogleCalendarExporter;
import org.dataportabilityproject.datatransfer.google.calendar.GoogleCalendarImporter;
import org.dataportabilityproject.datatransfer.google.contacts.GoogleContactsExporter;
import org.dataportabilityproject.datatransfer.google.contacts.GoogleContactsImporter;
import org.dataportabilityproject.datatransfer.google.tasks.GoogleTasksExporter;
import org.dataportabilityproject.datatransfer.google.tasks.GoogleTasksImporter;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.transfer.extension.TransferExtension;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.provider.Importer;

/*
 * GoogleTransferExtension allows for importers and exporters of data types
 * to be retrieved.
 */
public class GoogleTransferExtension implements TransferExtension {
  public static final String SERVICE_ID = "google";
  // TODO: centralized place, or enum type for these
  private ImmutableList<String> supportedServices = ImmutableList.of("calendar", "contacts", "tasks");
  private ImmutableMap<String, Importer> importerMap;
  private ImmutableMap<String, Exporter> exporterMap;
  private JobStore jobStore;
  private boolean initialized = false;

  @Override
  public String getServiceId() {
    return SERVICE_ID;
  }

  @Override
  public Exporter<?, ?> getExporter(String transferDataType) {
    Preconditions.checkArgument(initialized);
    Preconditions.checkArgument(supportedServices.contains(transferDataType));
    return exporterMap.get(transferDataType);
  }

  @Override
  public Importer<?, ?> getImporter(String transferDataType) {
    Preconditions.checkArgument(initialized);
    Preconditions.checkArgument(supportedServices.contains(transferDataType));
    return importerMap.get(transferDataType);
  }

  @Override
  public void initialize(ExtensionContext context) {
    // Note: initialize could be called twice in an account migration scenario where we import and
    // export to the same service provider. So just return rather than throwing if called multiple
    // times.
    if(initialized) return;

    jobStore = context.getService(JobStore.class);

    ImmutableMap.Builder<String, Importer> importerBuilder = ImmutableMap.builder();
    importerBuilder.put("contacts", new GoogleContactsImporter());
    importerBuilder.put("calendar", new GoogleCalendarImporter(jobStore)) ;
    importerBuilder.put("tasks", new GoogleTasksImporter(jobStore));
    importerMap = importerBuilder.build();

    ImmutableMap.Builder<String, Exporter> exporterBuilder = ImmutableMap.builder();
    exporterBuilder.put("contacts", new GoogleContactsExporter());
    exporterBuilder.put("calendar", new GoogleCalendarExporter());
    exporterBuilder.put("tasks", new GoogleTasksExporter());
    exporterMap = exporterBuilder.build();

    initialized = true;
  }
}
