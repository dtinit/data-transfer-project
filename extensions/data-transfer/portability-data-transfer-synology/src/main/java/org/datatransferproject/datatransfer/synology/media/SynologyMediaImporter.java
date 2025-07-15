package org.datatransferproject.datatransfer.synology.media;

import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.synology.service.SynologyOAuthTokenManager;
import org.datatransferproject.datatransfer.synology.uploader.SynologyUploader;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class SynologyMediaImporter
    implements Importer<TokensAndUrlAuthData, MediaContainerResource> {
  private final Monitor monitor;
  private final SynologyOAuthTokenManager tokenManager;
  private final SynologyUploader synologyUploader;

  public SynologyMediaImporter(
      Monitor monitor, SynologyOAuthTokenManager tokenManager, SynologyUploader synologyUploader) {
    this.monitor = monitor;
    this.tokenManager = tokenManager;
    this.synologyUploader = synologyUploader;

    monitor.info(() -> "Creating SynologyMediaImporter");
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      TokensAndUrlAuthData authData,
      MediaContainerResource data)
      throws Exception {
    monitor.info(() -> "==== [SynologyImporter]: SynologyMediaImporter starts importing item ====");
    tokenManager.addAuthDataIfNotExist(jobId, authData);

    try {
      synologyUploader.importAlbums(data.getAlbums(), jobId);
      synologyUploader.importPhotos(data.getPhotos(), jobId);
      synologyUploader.importVideos(data.getVideos(), jobId);
    } catch (Exception e) {
      monitor.severe(() -> "[SynologyImporter] SynologyMediaImporter failed to import data:" + e);
      return new ImportResult(e);
    }

    return ImportResult.OK;
  }
}
