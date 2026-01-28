package org.datatransferproject.datatransfer.synology.videos;

import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.synology.exceptions.SynologyException;
import org.datatransferproject.datatransfer.synology.service.SynologyOAuthTokenManager;
import org.datatransferproject.datatransfer.synology.uploader.SynologyUploader;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class SynologyVideosImporter
    implements Importer<TokensAndUrlAuthData, VideosContainerResource> {
  private final Monitor monitor;
  private final SynologyOAuthTokenManager tokenManager;
  private final SynologyUploader synologyUploader;

  public SynologyVideosImporter(
      Monitor monitor, SynologyOAuthTokenManager tokenManager, SynologyUploader synologyUploader) {
    this.monitor = monitor;
    this.tokenManager = tokenManager;
    this.synologyUploader = synologyUploader;

    monitor.info(() -> "Creating SynologyVideosImporter");
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      TokensAndUrlAuthData authData,
      VideosContainerResource data)
      throws Exception {
    monitor.info(() -> "==== [SynologyImporter] SynologyVideosImporter starts importing data ====");
    tokenManager.addAuthDataIfNotExist(jobId, authData);

    try {
      synologyUploader.importAlbums(data.getAlbums(), jobId);
      synologyUploader.importVideos(data.getVideos(), jobId);
    } catch (SynologyException e) {
      monitor.severe(() -> "[SynologyImporter] SynologyVideosImporter failed to import data:" + e);
      throw e;
    } catch (Exception e) {
      monitor.severe(() -> "[SynologyImporter] SynologyVideosImporter failed to import data:" + e);
      return new ImportResult(e);
    }

    return ImportResult.OK;
  }
}
