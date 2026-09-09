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
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.util.UUID;

/**
 * Imports photos into Amazon Photos from other DTP-supported services.
 *
 * <p>The per-job client, album creation, download+MD5, upload, and duplicate/quota handling live in
 * {@link AmazonImportHelper} and are shared with the videos and media importers; this class only
 * iterates the photos container and supplies the photo-specific title and favorite flag.
 */
public class AmazonPhotosImporter
    implements Importer<TokensAndUrlAuthData, PhotosContainerResource> {

  private final AmazonImportHelper importHelper;
  private final AmazonPhotosTransmogrificationConfig transmogrificationConfig =
      new AmazonPhotosTransmogrificationConfig();
  private final IdempotentImportExecutor retryingIdempotentExecutor;
  private final boolean enableRetrying;

  public AmazonPhotosImporter(Monitor monitor, String clientId, String clientSecret,
                              TemporaryPerJobDataStore dataStore,
                              IdempotentImportExecutor retryingIdempotentExecutor,
                              boolean enableRetrying) {
    this.importHelper = new AmazonImportHelper(dataStore, clientId, clientSecret, monitor);
    this.retryingIdempotentExecutor = retryingIdempotentExecutor;
    this.enableRetrying = enableRetrying;
  }

  AmazonPhotosImporter(Monitor monitor, TemporaryPerJobDataStore dataStore,
                       AmazonPhotosInterface client) {
    this(monitor, dataStore, client, null, false);
  }

  AmazonPhotosImporter(Monitor monitor, TemporaryPerJobDataStore dataStore,
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
                                 PhotosContainerResource data) throws Exception {
    AmazonPhotosInterface client = importHelper.getOrCreateClient(jobId, authData);
    data.transmogrify(transmogrificationConfig);

    // Prefer the platform's retrying executor when enabled so transient failures are retried
    // (per the host-configured RetryStrategyLibrary) before being recorded and skipped.
    IdempotentImportExecutor executor =
        (retryingIdempotentExecutor != null && enableRetrying)
            ? retryingIdempotentExecutor
            : idempotentImportExecutor;

    for (PhotoAlbum album : data.getAlbums()) {
      executor.executeAndSwallowIOExceptions(
          album.getId(), album.getName(),
          () -> importHelper.createAlbum(client, album.getId(), album.getName()));
    }

    for (PhotoModel photo : data.getPhotos()) {
      executor.executeAndSwallowIOExceptions(
          photo.getIdempotentId(), photo.getTitle(),
          () -> importHelper.uploadItem(client, jobId, UploadItemRequest.forPhoto(photo), executor));
    }

    return ImportResult.OK;
  }
}
