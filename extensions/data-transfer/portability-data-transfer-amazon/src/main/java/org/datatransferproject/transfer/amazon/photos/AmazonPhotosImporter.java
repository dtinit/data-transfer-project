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
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.transfer.JobMetadata;
import org.datatransferproject.transfer.amazon.photos.model.AmazonPhotosNode;
import org.datatransferproject.types.common.models.FavoriteInfo;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Imports photos into Amazon Photos from other DTP-supported services.
 *
 * <p>For each photo: downloads to a temp file (computing MD5 in a single pass via
 * DigestInputStream), then uploads via the Upload Service. Duplicate detection and
 * fallback album placement are handled server-side.
 */
public class AmazonPhotosImporter
    implements Importer<TokensAndUrlAuthData, PhotosContainerResource> {

  private final Monitor monitor;
  private final AmazonImportHelper importHelper;
  private final AmazonPhotosTransmogrificationConfig transmogrificationConfig =
      new AmazonPhotosTransmogrificationConfig();
  private final IdempotentImportExecutor retryingIdempotentExecutor;
  private final boolean enableRetrying;

  public AmazonPhotosImporter(Monitor monitor, String clientId, String clientSecret,
                              TemporaryPerJobDataStore dataStore,
                              IdempotentImportExecutor retryingIdempotentExecutor,
                              boolean enableRetrying) {
    this.monitor = monitor;
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
    this.monitor = monitor;
    this.importHelper = new AmazonImportHelper(dataStore, client);
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
          album.getId(), album.getName(), () -> createAlbum(client, album));
    }

    for (PhotoModel photo : data.getPhotos()) {
      executor.executeAndSwallowIOExceptions(
          photo.getIdempotentId(), photo.getTitle(),
          () -> uploadPhoto(client, jobId, photo, executor));
    }

    return ImportResult.OK;
  }

  private String createAlbum(AmazonPhotosInterface client, PhotoAlbum album) throws IOException {
    String albumName = album.getName() + AmazonImportHelper.IMPORTED_SUFFIX + JobMetadata.getExportService();
    AmazonPhotosNode node = client.createAlbum(albumName);
    monitor.info(() -> "Created album " + album.getId() + " -> " + node.getId());
    return node.getId();
  }

  private String uploadPhoto(AmazonPhotosInterface client, UUID jobId, PhotoModel photo,
                             IdempotentImportExecutor executor) throws Exception {
    String targetAlbumId = importHelper.resolveTargetAlbumId(photo.getAlbumId(), executor);
    MessageDigest md5 = importHelper.newMd5Digest();
    File tempFile = importHelper.downloadToTempFile(jobId, photo, photo.getDataId(), md5);

    try {
      String md5Hex = importHelper.toHexString(md5.digest());
      long fileSize = tempFile.length();
      String fallbackContentDate = Optional.ofNullable(photo.getUploadedTime())
          .map(d -> d.toInstant().toString())
          .orElse(Instant.now().toString());
      boolean isFavorite = Optional.ofNullable(photo.getFavoriteInfo())
          .map(FavoriteInfo::getFavorited)
          .orElse(false);

      AmazonPhotosNode uploadedNode = client.uploadContent(
          photo.getTitle(), tempFile, md5Hex,
          fileSize, fallbackContentDate, isFavorite, targetAlbumId);

      return uploadedNode.getId();

    } catch (AmazonPhotosApiException e) {
      if (importHelper.isDuplicate(e)) {
        monitor.info(() -> "Duplicate photo skipped: " + photo.getDataId());
        return photo.getDataId();
      }
      if (importHelper.isStorageQuotaExceeded(e)) {
        throw new DestinationMemoryFullException("Amazon Photos storage full", e);
      }
      throw e;
    } finally {
      tempFile.delete();
      if (photo.isInTempStore()) {
        importHelper.cleanupTempData(jobId, photo.getFetchableUrl());
      }
    }
  }
}
