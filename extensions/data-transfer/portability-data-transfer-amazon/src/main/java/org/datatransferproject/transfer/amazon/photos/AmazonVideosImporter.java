/*
 * Copyright 2026 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.amazon.photos;

import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.models.videos.VideoAlbum;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.util.UUID;

/**
 * Imports videos into Amazon Photos from other DTP-supported services.
 *
 * <p>The per-job client, album creation, download+MD5, upload, and duplicate/quota handling live in
 * {@link AmazonImportHelper} and are shared with the photos and media importers; this class only
 * iterates the videos container and maps each video onto those shared operations.
 */
public class AmazonVideosImporter
    implements Importer<TokensAndUrlAuthData, VideosContainerResource> {

  private final AmazonImportHelper importHelper;
  private final IdempotentImportExecutor retryingIdempotentExecutor;
  private final boolean enableRetrying;

  public AmazonVideosImporter(Monitor monitor, String clientId, String clientSecret,
                              TemporaryPerJobDataStore dataStore,
                              IdempotentImportExecutor retryingIdempotentExecutor,
                              boolean enableRetrying) {
    this.importHelper = new AmazonImportHelper(dataStore, clientId, clientSecret, monitor);
    this.retryingIdempotentExecutor = retryingIdempotentExecutor;
    this.enableRetrying = enableRetrying;
  }

  AmazonVideosImporter(Monitor monitor, TemporaryPerJobDataStore dataStore,
                       AmazonPhotosInterface client) {
    this(monitor, dataStore, client, null, false);
  }

  AmazonVideosImporter(Monitor monitor, TemporaryPerJobDataStore dataStore,
                       AmazonPhotosInterface client,
                       IdempotentImportExecutor retryingIdempotentExecutor,
                       boolean enableRetrying) {
    this.importHelper = new AmazonImportHelper(dataStore, client, monitor);
    this.retryingIdempotentExecutor = retryingIdempotentExecutor;
    this.enableRetrying = enableRetrying;
  }

  @Override
  public ImportResult importItem(UUID jobId, IdempotentImportExecutor idempotentImportExecutor,
                                 TokensAndUrlAuthData authData,
                                 VideosContainerResource data) throws Exception {
    AmazonPhotosInterface client = importHelper.getOrCreateClient(jobId, authData);

    // Prefer the platform's retrying executor when enabled so transient failures are retried
    // (per the host-configured RetryStrategyLibrary) before being recorded and skipped.
    IdempotentImportExecutor executor =
        (retryingIdempotentExecutor != null && enableRetrying)
            ? retryingIdempotentExecutor
            : idempotentImportExecutor;

    for (VideoAlbum album : data.getAlbums()) {
      executor.executeAndSwallowIOExceptions(
          album.getId(), album.getName(),
          () -> importHelper.createAlbum(client, jobId, album.getId(), album.getName()));
    }

    for (VideoModel video : data.getVideos()) {
      executor.executeAndSwallowIOExceptions(
          video.getIdempotentId(), video.getName(),
          () -> importHelper.uploadItem(client, jobId, UploadItemRequest.forVideo(video), executor));
    }

    return ImportResult.OK;
  }
}
