package org.datatransferproject.datatransfer.backblaze.videos;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.backblaze.common.BackblazeDataTransferClient;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.ImageStreamProvider;
import org.datatransferproject.types.common.models.videos.VideoObject;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;

public class BackblazeVideosImporter
    implements Importer<TokenSecretAuthData, VideosContainerResource> {

  private static final String VIDEO_TRANSFER_MAIN_FOLDER = "Video Transfer";

  private final TemporaryPerJobDataStore jobStore;
  private final ImageStreamProvider imageStreamProvider = new ImageStreamProvider();
  private final Monitor monitor;

  public BackblazeVideosImporter(Monitor monitor, TemporaryPerJobDataStore jobStore) {
    this.monitor = monitor;
    this.jobStore = jobStore;
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      TokenSecretAuthData authData,
      VideosContainerResource data)
      throws Exception {
    if (data == null) {
      // Nothing to do
      return ImportResult.OK;
    }

    BackblazeDataTransferClient b2Client = new BackblazeDataTransferClient(monitor);
    b2Client.init(authData.getToken(), authData.getSecret());

    if (data.getVideos() != null && data.getVideos().size() > 0) {
      for (VideoObject video : data.getVideos()) {
        idempotentExecutor.executeAndSwallowIOExceptions(
            video.getDataId(), video.getName(), () -> importSingleVideo(b2Client, video));
      }
    }

    return ImportResult.OK;
  }

  private String importSingleVideo(BackblazeDataTransferClient b2Client, VideoObject video)
      throws IOException {
    InputStream videoFileStream =
        imageStreamProvider.getConnection(video.getContentUrl().toString()).getInputStream();

    return b2Client.uploadFile(
        String.format("%s/%s.mp4", VIDEO_TRANSFER_MAIN_FOLDER, video.getDataId()),
        jobStore.getTempFileFromInputStream(videoFileStream, video.getDataId(), ".mp4"));
  }
}
