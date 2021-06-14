package org.datatransferproject.transfer.photobucket;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.common.collect.ImmutableList;
import okhttp3.OkHttpClient;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import com.google.common.base.Preconditions;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.photobucket.client.PhotobucketCredentialsFactory;
import org.datatransferproject.transfer.photobucket.photos.PhotobucketPhotosExporter;
import org.datatransferproject.transfer.photobucket.photos.PhotobucketPhotosImporter;
import org.datatransferproject.types.transfer.auth.AppCredentials;

import static java.lang.String.format;
import static org.datatransferproject.transfer.photobucket.data.PhotobucketConstants.*;

public class PhotobucketTransferExtension implements TransferExtension {
  private static final ImmutableList<String> SUPPORTED_SERVICES = ImmutableList.of("PHOTOS");
  private PhotobucketPhotosExporter exporter;
  private PhotobucketPhotosImporter importer;
  private boolean initialized = false;

  @Override
  public String getServiceId() {
    return PB_SERVICE_ID;
  }

  @Override
  public Exporter<?, ?> getExporter(String transferDataType) {
    Preconditions.checkArgument(initialized);
    Preconditions.checkArgument(SUPPORTED_SERVICES.contains(transferDataType));
    return exporter;
  }

  @Override
  public Importer<?, ?> getImporter(String transferDataType) {
    Preconditions.checkArgument(initialized);
    Preconditions.checkArgument(SUPPORTED_SERVICES.contains(transferDataType));
    return importer;
  }

  @Override
  public void initialize(ExtensionContext context) {
    Monitor monitor = context.getMonitor();
    monitor.debug(() -> "Starting PhotobucketTransferExtension initialization");
    if (initialized) {
      monitor.severe(() -> "PhotobucketTransferExtension already initialized.");
      return;
    }
    AppCredentials credentials;

    try {
      credentials =
          context.getService(AppCredentialStore.class).getAppCredentials(PB_KEY, PB_SECRET);
    } catch (Exception e) {
      monitor.info(
          () ->
              format(
                  "Unable to retrieve Photobucket AppCredentials. Did you set %s and %s?",
                  PB_KEY, PB_SECRET),
          e);
      initialized = false;
      return;
    }

    OkHttpClient httpClient = context.getService(OkHttpClient.class);
    TemporaryPerJobDataStore jobStore = context.getService(TemporaryPerJobDataStore.class);
    HttpTransport httpTransport = context.getService(HttpTransport.class);
    JsonFactory jsonFactory = context.getService(JsonFactory.class);
    PhotobucketCredentialsFactory credentialsFactory =
        new PhotobucketCredentialsFactory(httpTransport, jsonFactory, credentials);

    importer = new PhotobucketPhotosImporter(credentialsFactory, monitor, httpClient, jobStore);
    exporter = new PhotobucketPhotosExporter(credentialsFactory, monitor);
    initialized = true;
  }
}
