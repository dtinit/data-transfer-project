package org.datatransferproject.datatransfer.synology;

import static org.datatransferproject.types.common.models.DataVertical.MEDIA;
import static org.datatransferproject.types.common.models.DataVertical.PHOTOS;
import static org.datatransferproject.types.common.models.DataVertical.VIDEOS;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import okhttp3.OkHttpClient;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.synology.media.SynologyMediaImporter;
import org.datatransferproject.datatransfer.synology.photos.SynologyPhotosImporter;
import org.datatransferproject.datatransfer.synology.service.SynologyDTPService;
import org.datatransferproject.datatransfer.synology.service.SynologyOAuthTokenManager;
import org.datatransferproject.datatransfer.synology.uploader.SynologyUploader;
import org.datatransferproject.datatransfer.synology.videos.SynologyVideosImporter;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutorExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.JobMetadata;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.serviceconfig.TransferServiceConfig;

public class SynologyTransferExtension implements TransferExtension {
  private static final ImmutableList<DataVertical> SUPPORTED_SERVICES =
      ImmutableList.of(PHOTOS, VIDEOS, MEDIA);
  private Monitor monitor;

  private boolean initialized = false;

  private ImmutableMap<DataVertical, Importer> importerMap;

  @Override
  public String getServiceId() {
    return "Synology";
  }

  private String getExtensionKey() {
    return getServiceId().toUpperCase() + "_KEY";
  }

  private String getExtensionSecret() {
    return getServiceId().toUpperCase() + "_SECRET";
  }

  @Override
  public Exporter<?, ?> getExporter(DataVertical transferDataType) {
    return null;
  }

  @Override
  public Importer<?, ?> getImporter(DataVertical transferDataType) {
    Preconditions.checkArgument(initialized);
    Preconditions.checkArgument(SUPPORTED_SERVICES.contains(transferDataType));
    monitor.info(() -> "Getting importer for " + transferDataType);
    monitor.info(() -> "has importer: " + importerMap.containsKey(transferDataType));
    return importerMap.get(transferDataType);
  }

  @Override
  public void initialize(ExtensionContext context) {
    if (initialized) {
      return;
    }

    monitor = context.getMonitor();

    AppCredentials appCredentials;
    try {
      appCredentials =
          context
              .getService(AppCredentialStore.class)
              .getAppCredentials(getExtensionKey(), getExtensionSecret());

    } catch (Exception e) {
      monitor.info(
          () ->
              "Unable to retrieve Synology AppCredentials. Please configure Synology KEY and"
                  + " SECRET.");
      return;
    }

    JobStore jobStore = context.getService(JobStore.class);
    IdempotentImportExecutor idempotentImportExecutor =
        context
            .getService(IdempotentImportExecutorExtension.class)
            .getRetryingIdempotentImportExecutor(context);
    SynologyOAuthTokenManager tokenManager = new SynologyOAuthTokenManager(appCredentials, monitor);
    OkHttpClient client = context.getService(OkHttpClient.class);
    TransferServiceConfig transferServiceConfig = context.getService(TransferServiceConfig.class);
    SynologyDTPService synologyDTPService =
        new SynologyDTPService(
            monitor,
            transferServiceConfig,
            JobMetadata.getExportService(),
            jobStore,
            tokenManager,
            client);
    SynologyUploader synologyUploader =
        new SynologyUploader(idempotentImportExecutor, monitor, synologyDTPService);

    ImmutableMap.Builder<DataVertical, Importer> importerBuilder = ImmutableMap.builder();
    importerBuilder.put(MEDIA, new SynologyMediaImporter(monitor, tokenManager, synologyUploader));
    importerBuilder.put(
        PHOTOS, new SynologyPhotosImporter(monitor, tokenManager, synologyUploader));
    importerBuilder.put(
        VIDEOS, new SynologyVideosImporter(monitor, tokenManager, synologyUploader));
    importerMap = importerBuilder.build();

    monitor.info(() -> "Initializing SynologyTransferExtension");
    initialized = true;
  }
}
