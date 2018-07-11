package org.datatransferproject.datatransfer.google;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.datatransfer.google.calendar.GoogleCalendarExporter;
import org.datatransferproject.datatransfer.google.calendar.GoogleCalendarImporter;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.contacts.GoogleContactsExporter;
import org.datatransferproject.datatransfer.google.contacts.GoogleContactsImporter;
import org.datatransferproject.datatransfer.google.photos.GooglePhotosExporter;
import org.datatransferproject.datatransfer.google.photos.GooglePhotosImporter;
import org.datatransferproject.datatransfer.google.mail.GoogleMailExporter;
import org.datatransferproject.datatransfer.google.mail.GoogleMailImporter;
import org.datatransferproject.datatransfer.google.tasks.GoogleTasksExporter;
import org.datatransferproject.datatransfer.google.tasks.GoogleTasksImporter;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.transfer.auth.AppCredentials;
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
  private static final ImmutableList<String> SUPPORTED_SERVICES =
      ImmutableList.of("calendar", "contacts", "mail", "photos", "tasks");
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
    Preconditions.checkArgument(SUPPORTED_SERVICES.contains(transferDataType));
    return exporterMap.get(transferDataType);
  }

  @Override
  public Importer<?, ?> getImporter(String transferDataType) {
    Preconditions.checkArgument(initialized);
    Preconditions.checkArgument(SUPPORTED_SERVICES.contains(transferDataType));
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
    importerBuilder.put("mail", new GoogleMailImporter(credentialFactory, jobStore)) ;
    importerBuilder.put("tasks", new GoogleTasksImporter(credentialFactory, jobStore));
    importerBuilder.put("photos", new GooglePhotosImporter(credentialFactory, jobStore));
    importerMap = importerBuilder.build();

    ImmutableMap.Builder<String, Exporter> exporterBuilder = ImmutableMap.builder();
    exporterBuilder.put("contacts", new GoogleContactsExporter(credentialFactory));
    exporterBuilder.put("calendar", new GoogleCalendarExporter(credentialFactory));
    exporterBuilder.put("mail", new GoogleMailExporter(credentialFactory));
    exporterBuilder.put("tasks", new GoogleTasksExporter(credentialFactory));
    exporterBuilder.put("photos", new GooglePhotosExporter(credentialFactory));

    exporterMap = exporterBuilder.build();

    initialized = true;
  }
}
