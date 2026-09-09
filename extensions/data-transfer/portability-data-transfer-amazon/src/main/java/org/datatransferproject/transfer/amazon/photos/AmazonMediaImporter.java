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
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.util.UUID;

/**
 * Imports the unified MEDIA vertical (albums + photos + videos in one {@link
 * MediaContainerResource}) into Amazon Photos.
 *
 * <p>Albums are created first so photos and videos can resolve their parent album from the {@link
 * IdempotentImportExecutor} cache. All the actual work — per-job client provisioning, album
 * creation, download+MD5, upload, and duplicate/quota handling — lives in {@link
 * AmazonImportHelper} and is shared with the photos and videos importers; this class only maps the
 * MEDIA container's three item types onto those shared operations.
 */
public class AmazonMediaImporter
    implements Importer<TokensAndUrlAuthData, MediaContainerResource> {

  private final AmazonImportHelper importHelper;
  private final AmazonMediaTransmogrificationConfig transmogrificationConfig =
      new AmazonMediaTransmogrificationConfig();
  private final IdempotentImportExecutor retryingIdempotentExecutor;
  private final boolean enableRetrying;

  public AmazonMediaImporter(Monitor monitor, String clientId, String clientSecret,
                             TemporaryPerJobDataStore dataStore,
                             IdempotentImportExecutor retryingIdempotentExecutor,
                             boolean enableRetrying) {
    this.importHelper = new AmazonImportHelper(dataStore, clientId, clientSecret, monitor);
    this.retryingIdempotentExecutor = retryingIdempotentExecutor;
    this.enableRetrying = enableRetrying;
  }

  AmazonMediaImporter(Monitor monitor, TemporaryPerJobDataStore dataStore,
                      AmazonPhotosInterface client) {
    this(monitor, dataStore, client, null, false);
  }

  AmazonMediaImporter(Monitor monitor, TemporaryPerJobDataStore dataStore,
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
                                 MediaContainerResource data) throws Exception {
    AmazonPhotosInterface client = importHelper.getOrCreateClient(jobId, authData);
    data.transmogrify(transmogrificationConfig);

    // Prefer the platform's retrying executor when enabled so transient failures are retried
    // (per the host-configured RetryStrategyLibrary) before being recorded and skipped.
    IdempotentImportExecutor executor =
        (retryingIdempotentExecutor != null && enableRetrying)
            ? retryingIdempotentExecutor
            : idempotentImportExecutor;

    // Albums first: photos and videos resolve their parent album from the executor cache.
    for (MediaAlbum album : data.getAlbums()) {
      executor.executeAndSwallowIOExceptions(
          album.getId(), album.getName(),
          () -> importHelper.createAlbum(client, album.getId(), album.getName()));
    }

    for (PhotoModel photo : data.getPhotos()) {
      executor.executeAndSwallowIOExceptions(
          photo.getIdempotentId(), photo.getTitle(),
          () -> importHelper.uploadItem(client, jobId, UploadItemRequest.forPhoto(photo), executor));
    }

    for (VideoModel video : data.getVideos()) {
      executor.executeAndSwallowIOExceptions(
          video.getIdempotentId(), video.getName(),
          () -> importHelper.uploadItem(client, jobId, UploadItemRequest.forVideo(video), executor));
    }

    return ImportResult.OK;
  }
}
