package org.datatransferproject.transfer.koofr;

import static java.lang.String.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.koofr.common.KoofrClientFactory;
import org.datatransferproject.transfer.koofr.common.KoofrCredentialFactory;
import org.datatransferproject.transfer.koofr.photos.KoofrPhotosExporter;
import org.datatransferproject.transfer.koofr.photos.KoofrPhotosImporter;
import org.datatransferproject.transfer.koofr.videos.KoofrVideosExporter;
import org.datatransferproject.transfer.koofr.videos.KoofrVideosImporter;
import org.datatransferproject.types.transfer.auth.AppCredentials;

/** Bootstraps the Koofr data transfer services. */
public class KoofrTransferExtension implements TransferExtension {
  public static final String SERVICE_ID = "koofr";
  private static final String PHOTOS = "PHOTOS";
  private static final String VIDEOS = "VIDEOS";
  private static final ImmutableList<String> SUPPORTED_IMPORT_SERVICES =
      ImmutableList.of(PHOTOS, VIDEOS);
  private static final ImmutableList<String> SUPPORTED_EXPORT_SERVICES =
      ImmutableList.of(PHOTOS, VIDEOS);
  private static final String BASE_API_URL = "https://app.koofr.net";
  private ImmutableMap<String, Importer> importerMap;
  private ImmutableMap<String, Exporter> exporterMap;
  private boolean initialized = false;

  // Needed for ServiceLoader to load this class.
  public KoofrTransferExtension() {}

  @Override
  public String getServiceId() {
    return SERVICE_ID;
  }

  @Override
  public Exporter<?, ?> getExporter(String transferDataType) {
    Preconditions.checkState(initialized);
    Preconditions.checkArgument(SUPPORTED_EXPORT_SERVICES.contains(transferDataType));
    return exporterMap.get(transferDataType);
  }

  @Override
  public Importer<?, ?> getImporter(String transferDataType) {
    Preconditions.checkState(initialized);
    Preconditions.checkArgument(SUPPORTED_IMPORT_SERVICES.contains(transferDataType));
    return importerMap.get(transferDataType);
  }

  @Override
  public void initialize(ExtensionContext context) {
    // Note: initialize could be called twice in an account migration scenario
    // where we import and export to the same service provider. So just return
    // rather than throwing if called multiple times.
    if (initialized) return;

    JobStore jobStore = context.getService(JobStore.class);
    HttpTransport httpTransport = context.getService(HttpTransport.class);
    JsonFactory jsonFactory = context.getService(JsonFactory.class);
    OkHttpClient client = new OkHttpClient.Builder().build();
    ObjectMapper mapper = new ObjectMapper();

    AppCredentials appCredentials;
    try {
      appCredentials =
          context
              .getService(AppCredentialStore.class)
              .getAppCredentials("KOOFR_KEY", "KOOFR_SECRET");
    } catch (IOException e) {
      Monitor monitor = context.getMonitor();
      monitor.info(
          () -> "Unable to retrieve Koofr AppCredentials. Did you set KOOFR_KEY and KOOFR_SECRET?");
      return;
    }

    // Create the KoofrCredentialFactory with the given {@link AppCredentials}.
    KoofrCredentialFactory credentialFactory =
        new KoofrCredentialFactory(httpTransport, jsonFactory, appCredentials);

    Monitor monitor = context.getMonitor();

    int fileUploadReadTimeout = context.getSetting("koofrFileUploadReadTimeout", 60000);
    int fileUploadWriteTimeout = context.getSetting("koofrFileUploadWriteTimeout", 60000);

    monitor.info(
        () ->
            format(
                "Configuring Koofr HTTP file upload client with read timeout %d ms and write timeout %d ms",
                fileUploadReadTimeout, fileUploadWriteTimeout));

    OkHttpClient fileUploadClient =
        client
            .newBuilder()
            .readTimeout(fileUploadReadTimeout, TimeUnit.MILLISECONDS)
            .writeTimeout(fileUploadReadTimeout, TimeUnit.MILLISECONDS)
            .build();

    KoofrClientFactory koofrClientFactory =
        new KoofrClientFactory(
            BASE_API_URL, client, fileUploadClient, mapper, monitor, credentialFactory);

    ImmutableMap.Builder<String, Importer> importBuilder = ImmutableMap.builder();
    importBuilder.put(PHOTOS, new KoofrPhotosImporter(koofrClientFactory, monitor, jobStore));
    importBuilder.put(VIDEOS, new KoofrVideosImporter(koofrClientFactory, monitor));
    importerMap = importBuilder.build();

    ImmutableMap.Builder<String, Exporter> exportBuilder = ImmutableMap.builder();
    exportBuilder.put(PHOTOS, new KoofrPhotosExporter(koofrClientFactory, monitor));
    exportBuilder.put(VIDEOS, new KoofrVideosExporter(koofrClientFactory, monitor));
    exporterMap = exportBuilder.build();

    initialized = true;
  }
}
