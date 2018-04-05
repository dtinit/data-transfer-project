package org.dataportabilityproject.datatransfer.google;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.datatransfer.google.calendar.GoogleCalendarExporter;
import org.dataportabilityproject.datatransfer.google.calendar.GoogleCalendarImporter;
import org.dataportabilityproject.datatransfer.google.common.GoogleCredentialFactory;
import org.dataportabilityproject.datatransfer.google.contacts.GoogleContactsExporter;
import org.dataportabilityproject.datatransfer.google.contacts.GoogleContactsImporter;
import org.dataportabilityproject.datatransfer.google.photos.GooglePhotosExporter;
import org.dataportabilityproject.datatransfer.google.photos.GooglePhotosImporter;
import org.dataportabilityproject.datatransfer.google.tasks.GoogleTasksExporter;
import org.dataportabilityproject.datatransfer.google.tasks.GoogleTasksImporter;
import org.dataportabilityproject.spi.cloud.storage.AppCredentialStore;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.transfer.extension.TransferExtension;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * GoogleTransferExtension allows for importers and exporters of data types
 * to be retrieved.
 */
public class GoogleTransferExtension implements TransferExtension {
  private static final Logger logger = LoggerFactory.getLogger(GoogleTransferExtension.class);
  public static final String SERVICE_ID = "google";
  // TODO: centralized place, or enum type for these
  private ImmutableList<String> supportedServices =
      ImmutableList.of("calendar", "contacts", "tasks", "photos");
  private ImmutableMap<String, Importer> importerMap;
  private ImmutableMap<String, Exporter> exporterMap;
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
    if (initialized) return;

    JobStore jobStore = context.getService(JobStore.class);
    HttpTransport httpTransport = context.getService(HttpTransport.class);
    JsonFactory jsonFactory = context.getService(JsonFactory.class);

    AppCredentials appCredentials;
    try {
      appCredentials =
          context
              .getService(AppCredentialStore.class)
              .getAppCredentials("GOOGLE_KEY", "GOOGLE_SECRET");
    } catch (IOException e) {
      logger.warn(
          "Problem getting AppCredentials: {}. Did you set GOOGLE_KEY and GOOGLE_SECRET?", e);
      return;
    }

    // Create the GoogleCredentialFactory with the given {@link AppCredentials}.
    GoogleCredentialFactory credentialFactory =
        new GoogleCredentialFactory(httpTransport, jsonFactory, appCredentials);

    ImmutableMap.Builder<String, Importer> importerBuilder = ImmutableMap.builder();
    importerBuilder.put("contacts", new GoogleContactsImporter(credentialFactory));
    importerBuilder.put("calendar", new GoogleCalendarImporter(credentialFactory, jobStore));
    importerBuilder.put("tasks", new GoogleTasksImporter(credentialFactory, jobStore));
    importerBuilder.put("photos", new GooglePhotosImporter(credentialFactory, jobStore));
    importerMap = importerBuilder.build();

    ImmutableMap.Builder<String, Exporter> exporterBuilder = ImmutableMap.builder();
    exporterBuilder.put("contacts", new GoogleContactsExporter(credentialFactory));
    exporterBuilder.put("calendar", new GoogleCalendarExporter(credentialFactory));
    exporterBuilder.put("tasks", new GoogleTasksExporter(credentialFactory));
    exporterBuilder.put("photos", new GooglePhotosExporter(credentialFactory));
    exporterMap = exporterBuilder.build();

    initialized = true;
  }
}
