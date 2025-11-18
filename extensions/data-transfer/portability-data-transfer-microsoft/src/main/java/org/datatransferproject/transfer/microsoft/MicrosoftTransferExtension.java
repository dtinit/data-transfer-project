package org.datatransferproject.transfer.microsoft;

import static org.datatransferproject.types.common.models.DataVertical.CALENDAR;
import static org.datatransferproject.types.common.models.DataVertical.CONTACTS;
import static org.datatransferproject.types.common.models.DataVertical.MEDIA;
import static org.datatransferproject.types.common.models.DataVertical.OFFLINE_DATA;
import static org.datatransferproject.types.common.models.DataVertical.PHOTOS;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import okhttp3.OkHttpClient;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.api.transport.JobFileStream;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.microsoft.calendar.MicrosoftCalendarExporter;
import org.datatransferproject.transfer.microsoft.calendar.MicrosoftCalendarImporter;
import org.datatransferproject.transfer.microsoft.common.MicrosoftCredentialFactory;
import org.datatransferproject.transfer.microsoft.contacts.MicrosoftContactsExporter;
import org.datatransferproject.transfer.microsoft.contacts.MicrosoftContactsImporter;
import org.datatransferproject.transfer.microsoft.offline.MicrosoftOfflineDataExporter;
import org.datatransferproject.transfer.microsoft.media.MicrosoftMediaExporter;
import org.datatransferproject.transfer.microsoft.media.MicrosoftMediaImporter;
import org.datatransferproject.transfer.microsoft.photos.MicrosoftPhotosExporter;
import org.datatransferproject.transfer.microsoft.photos.MicrosoftPhotosImporter;
import org.datatransferproject.transfer.microsoft.transformer.TransformerService;
import org.datatransferproject.transfer.microsoft.transformer.TransformerServiceImpl;
import org.datatransferproject.types.transfer.auth.AppCredentials;

import java.io.IOException;

/** Bootstraps the Microsoft data transfer services. */
public class MicrosoftTransferExtension implements TransferExtension {
  public static final String SERVICE_ID = "microsoft";

  // TODO(#1065) don't keep adding here - just have the converters invoked automatically when Media
  // isn't supported on one or the other side of this equation; this is just a WIP prototype to show
  // the concept of converters at play.
  private static final ImmutableList<DataVertical> SUPPORTED_IMPORT_SERVICES =
      ImmutableList.of(CALENDAR, CONTACTS, PHOTOS, MEDIA);
  private static final ImmutableList<DataVertical> SUPPORTED_EXPORT_SERVICES =
      ImmutableList.of(CALENDAR, CONTACTS, PHOTOS, MEDIA, OFFLINE_DATA);
  private ImmutableMap<DataVertical, Importer> importerMap;
  private ImmutableMap<DataVertical, Exporter> exporterMap;

  private static final String BASE_GRAPH_URL = "https://graph.microsoft.com";

  private final boolean offlineData;

  private boolean initialized = false;

  // Needed for ServiceLoader to load this class.
  public MicrosoftTransferExtension() {
    offlineData = Boolean.parseBoolean(System.getProperty("offlineData"));
  }

  @Override
  public String getServiceId() {
    return SERVICE_ID;
  }

  @Override
  public Exporter<?, ?> getExporter(DataVertical transferDataType) {
    Preconditions.checkState(initialized);

    if (!offlineData && transferDataType.equals(OFFLINE_DATA)) {
      // only enable if derived data explicitly set as a configuration value
      // TODO we may want to provide a config option that allows deployers to disable transfer of
      // certain data types
      return null;
    }

    Preconditions.checkArgument(SUPPORTED_EXPORT_SERVICES.contains(transferDataType));
    return exporterMap.get(transferDataType);
  }

  @Override
  public Importer<?, ?> getImporter(DataVertical transferDataType) {
    Preconditions.checkState(initialized);
    Preconditions.checkArgument(SUPPORTED_IMPORT_SERVICES.contains(transferDataType));
    return importerMap.get(transferDataType);
  }

  @Override
  public void initialize(ExtensionContext context) {
    // Note: initialize could be called twice in an account migration scenario where we import and
    // export to the same service provider. So just return rather than throwing if called multiple
    // times.
    if (initialized) return;

    final double maxWritesPerSecond =  context.getSetting("msoftMaxWritesPerSecond", 1.0);
    TemporaryPerJobDataStore jobStore = context.getService(TemporaryPerJobDataStore.class);
    HttpTransport httpTransport = context.getService(HttpTransport.class);

    JsonFactory jsonFactory = context.getService(JsonFactory.class);
    TransformerService transformerService = new TransformerServiceImpl();
    OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
    ObjectMapper mapper = new ObjectMapper();

    // TODO(jzacsh) does anyone really want this across importer/exporters of
    // this adapter? we can probably delete this and pass a fresh .build() to
    // every constructor below, right?
    final OkHttpClient sharedHttpClient = httpClientBuilder.build();

    AppCredentials appCredentials;
    try {
      appCredentials =
          context
              .getService(AppCredentialStore.class)
              .getAppCredentials("MICROSOFT_KEY", "MICROSOFT_SECRET");
    } catch (IOException e) {
      Monitor monitor = context.getMonitor();
      monitor.info(
          () ->
              "Unable to retrieve Microsoft AppCredentials. Did you set MICROSOFT_KEY and MICROSOFT_SECRET?");
      return;
    }

    // Create the MicrosoftCredentialFactory with the given {@link AppCredentials}.
    MicrosoftCredentialFactory credentialFactory =
        new MicrosoftCredentialFactory(httpTransport, jsonFactory, appCredentials);
    JobFileStream jobFileStream = new JobFileStream();

    Monitor monitor = context.getMonitor();

    ImmutableMap.Builder<DataVertical, Importer> importBuilder = ImmutableMap.builder();
    importBuilder.put(
        CONTACTS,
        new MicrosoftContactsImporter(BASE_GRAPH_URL, sharedHttpClient, mapper, transformerService));
    importBuilder.put(
        CALENDAR,
        new MicrosoftCalendarImporter(BASE_GRAPH_URL, sharedHttpClient, mapper, transformerService));
    // WARNING: do not use this importer; it will be deleted soon by jzacsh@ in favor of the actually maintained Media variant.
    importBuilder.put(
        PHOTOS, new MicrosoftPhotosImporter(BASE_GRAPH_URL, sharedHttpClient, mapper, jobStore, monitor,
          credentialFactory, jobFileStream));
    importBuilder.put(MEDIA, new MicrosoftMediaImporter(BASE_GRAPH_URL, httpClientBuilder, mapper, jobStore, monitor,
          credentialFactory, jobFileStream, maxWritesPerSecond));
    importerMap = importBuilder.build();

    ImmutableMap.Builder<DataVertical, Exporter> exporterBuilder = ImmutableMap.builder();
    exporterBuilder.put(
        CONTACTS,
        new MicrosoftContactsExporter(BASE_GRAPH_URL, sharedHttpClient, mapper, transformerService));
    exporterBuilder.put(
        CALENDAR,
        new MicrosoftCalendarExporter(BASE_GRAPH_URL, sharedHttpClient, mapper, transformerService));
    // WARNING: do not use this importer; it will be deleted soon by jzacsh@ in favor of the actually maintained Media variant.
    exporterBuilder.put(PHOTOS, new MicrosoftPhotosExporter(credentialFactory, jsonFactory, monitor));
    exporterBuilder.put(MEDIA, new MicrosoftMediaExporter(credentialFactory, jsonFactory, monitor));
    exporterBuilder.put(
        OFFLINE_DATA, new MicrosoftOfflineDataExporter(BASE_GRAPH_URL, sharedHttpClient, mapper));

    exporterMap = exporterBuilder.build();

    initialized = true;
  }
}
