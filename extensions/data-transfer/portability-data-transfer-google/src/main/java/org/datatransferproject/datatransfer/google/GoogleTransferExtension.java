package org.datatransferproject.datatransfer.google;

import static org.datatransferproject.types.common.models.DataVertical.BLOBS;
import static org.datatransferproject.types.common.models.DataVertical.CALENDAR;
import static org.datatransferproject.types.common.models.DataVertical.CONTACTS;
import static org.datatransferproject.types.common.models.DataVertical.MAIL;
import static org.datatransferproject.types.common.models.DataVertical.PHOTOS;
import static org.datatransferproject.types.common.models.DataVertical.SOCIAL_POSTS;
import static org.datatransferproject.types.common.models.DataVertical.TASKS;
import static org.datatransferproject.types.common.models.DataVertical.VIDEOS;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.google.calendar.GoogleCalendarExporter;
import org.datatransferproject.datatransfer.google.calendar.GoogleCalendarImporter;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.contacts.GoogleContactsExporter;
import org.datatransferproject.datatransfer.google.contacts.GoogleContactsImporter;
import org.datatransferproject.datatransfer.google.drive.DriveExporter;
import org.datatransferproject.datatransfer.google.drive.DriveImporter;
import org.datatransferproject.datatransfer.google.gplus.GooglePlusExporter;
import org.datatransferproject.datatransfer.google.mail.GoogleMailExporter;
import org.datatransferproject.datatransfer.google.mail.GoogleMailImporter;
import org.datatransferproject.datatransfer.google.photos.GooglePhotosExporter;
import org.datatransferproject.datatransfer.google.photos.GooglePhotosImporter;
import org.datatransferproject.datatransfer.google.tasks.GoogleTasksExporter;
import org.datatransferproject.datatransfer.google.tasks.GoogleTasksImporter;
import org.datatransferproject.datatransfer.google.videos.GoogleVideosExporter;
import org.datatransferproject.datatransfer.google.videos.GoogleVideosImporter;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.transfer.auth.AppCredentials;

/*
 * GoogleTransferExtension allows for importers and exporters of data types
 * to be retrieved.
 */
public class GoogleTransferExtension implements TransferExtension {
  public static final String SERVICE_ID = "google";
  // TODO: centralized place, or enum type for these
  private static final ImmutableList<DataVertical> SUPPORTED_SERVICES =
      ImmutableList.of(
          BLOBS, CALENDAR, CONTACTS, MAIL, PHOTOS, SOCIAL_POSTS, TASKS, VIDEOS);
  private ImmutableMap<DataVertical, Importer> importerMap;
  private ImmutableMap<DataVertical, Exporter> exporterMap;
  private boolean initialized = false;

  @Override
  public String getServiceId() {
    return SERVICE_ID;
  }

  @Override
  public Exporter<?, ?> getExporter(DataVertical transferDataType) {
    Preconditions.checkArgument(initialized);
    Preconditions.checkArgument(SUPPORTED_SERVICES.contains(transferDataType));
    return exporterMap.get(transferDataType);
  }

  @Override
  public Importer<?, ?> getImporter(DataVertical transferDataType) {
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
      Monitor monitor = context.getMonitor();
      monitor.info(
          () ->
              "Unable to retrieve Google AppCredentials. Did you set GOOGLE_KEY and GOOGLE_SECRET?");
      return;
    }

    Monitor monitor = context.getMonitor();

    // Create the GoogleCredentialFactory with the given {@link AppCredentials}.
    GoogleCredentialFactory credentialFactory =
        new GoogleCredentialFactory(httpTransport, jsonFactory, appCredentials, monitor);

    ImmutableMap.Builder<DataVertical, Importer> importerBuilder = ImmutableMap.builder();
    importerBuilder.put(BLOBS, new DriveImporter(credentialFactory, jobStore, monitor));
    importerBuilder.put(CONTACTS, new GoogleContactsImporter(credentialFactory));
    importerBuilder.put(CALENDAR, new GoogleCalendarImporter(credentialFactory));
    importerBuilder.put(MAIL, new GoogleMailImporter(credentialFactory, monitor));
    importerBuilder.put(TASKS, new GoogleTasksImporter(credentialFactory));
    importerBuilder.put(
        PHOTOS,
        new GooglePhotosImporter(
            credentialFactory,
            jobStore,
            jsonFactory,
            monitor,
            context.getSetting("googleWritesPerSecond", 1.0)));
    importerBuilder.put(VIDEOS, new GoogleVideosImporter(appCredentials, jobStore, monitor));
    importerMap = importerBuilder.build();

    ImmutableMap.Builder<DataVertical, Exporter> exporterBuilder = ImmutableMap.builder();
    exporterBuilder.put(BLOBS, new DriveExporter(credentialFactory, jobStore, monitor));
    exporterBuilder.put(CONTACTS, new GoogleContactsExporter(credentialFactory));
    exporterBuilder.put(CALENDAR, new GoogleCalendarExporter(credentialFactory));
    exporterBuilder.put(MAIL, new GoogleMailExporter(credentialFactory));
    exporterBuilder.put(SOCIAL_POSTS, new GooglePlusExporter(credentialFactory));
    exporterBuilder.put(TASKS, new GoogleTasksExporter(credentialFactory, monitor));
    exporterBuilder.put(
        PHOTOS, new GooglePhotosExporter(credentialFactory, jobStore, jsonFactory, monitor));
    exporterBuilder.put(VIDEOS, new GoogleVideosExporter(credentialFactory, jsonFactory));

    exporterMap = exporterBuilder.build();

    initialized = true;
  }
}
