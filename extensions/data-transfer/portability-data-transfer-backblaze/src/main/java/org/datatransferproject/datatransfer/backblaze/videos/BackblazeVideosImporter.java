/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.datatransferproject.datatransfer.backblaze.videos;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.backblaze.common.BackblazeDataTransferClient;
import org.datatransferproject.datatransfer.backblaze.common.BackblazeDataTransferClientFactory;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.ImageStreamProvider;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;

public class BackblazeVideosImporter
    implements Importer<TokenSecretAuthData, VideosContainerResource> {

  private static final String VIDEO_TRANSFER_MAIN_FOLDER = "Video Transfer";

  private final TemporaryPerJobDataStore jobStore;
  private final ImageStreamProvider imageStreamProvider;
  private final Monitor monitor;
  private final BackblazeDataTransferClientFactory b2ClientFactory;

  public BackblazeVideosImporter(
          Monitor monitor,
          TemporaryPerJobDataStore jobStore,
          ImageStreamProvider imageStreamProvider,
          BackblazeDataTransferClientFactory b2ClientFactory) {
    this.monitor = monitor;
    this.jobStore = jobStore;
    this.imageStreamProvider = imageStreamProvider;
    this.b2ClientFactory = b2ClientFactory;
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

    BackblazeDataTransferClient b2Client = b2ClientFactory.getOrCreateB2Client(monitor, authData);

    if (data.getVideos() != null && data.getVideos().size() > 0) {
      for (VideoModel video : data.getVideos()) {
        idempotentExecutor.executeAndSwallowIOExceptions(
            video.getDataId(), video.getName(), () -> importSingleVideo(b2Client, video));
      }
    }

    return ImportResult.OK;
  }

  private String importSingleVideo(BackblazeDataTransferClient b2Client, VideoModel video)
      throws IOException {
    InputStream videoFileStream =
        imageStreamProvider.getConnection(video.getContentUrl().toString()).getInputStream();

    return b2Client.uploadFile(
        String.format("%s/%s.mp4", VIDEO_TRANSFER_MAIN_FOLDER, video.getDataId()),
        jobStore.getTempFileFromInputStream(videoFileStream, video.getDataId(), ".mp4"));
  }
}
