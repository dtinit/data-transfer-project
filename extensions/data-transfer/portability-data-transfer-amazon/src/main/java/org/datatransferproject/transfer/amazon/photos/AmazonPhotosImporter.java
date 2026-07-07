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
import org.datatransferproject.spi.cloud.connection.ConnectionProvider;
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
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

  private static final String IMPORTED_SUFFIX = " - Imported from ";

  private final Monitor monitor;
  private final String clientId;
  private final String clientSecret;
  private final TemporaryPerJobDataStore dataStore;
  private final ConnectionProvider connectionProvider;
  private final AmazonPhotosTransmogrificationConfig transmogrificationConfig =
      new AmazonPhotosTransmogrificationConfig();

  private AmazonPhotosInterface client;

  public AmazonPhotosImporter(Monitor monitor, String clientId, String clientSecret,
                              TemporaryPerJobDataStore dataStore) {
    this.monitor = monitor;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.dataStore = dataStore;
    this.connectionProvider = new ConnectionProvider(dataStore);
  }

  AmazonPhotosImporter(Monitor monitor, TemporaryPerJobDataStore dataStore,
                       AmazonPhotosInterface client) {
    this.monitor = monitor;
    this.clientId = null;
    this.clientSecret = null;
    this.dataStore = dataStore;
    this.connectionProvider = new ConnectionProvider(dataStore);
    this.client = client;
  }

  @Override
  public ImportResult importItem(UUID jobId, IdempotentImportExecutor executor,
                                 TokensAndUrlAuthData authData,
                                 PhotosContainerResource data) throws Exception {
    initializeClient(authData);
    data.transmogrify(transmogrificationConfig);

    for (PhotoAlbum album : data.getAlbums()) {
      executor.executeAndSwallowIOExceptions(
          album.getId(), album.getName(), () -> createAlbum(album));
    }

    for (PhotoModel photo : data.getPhotos()) {
      executor.executeAndSwallowIOExceptions(
          photo.getIdempotentId(), photo.getTitle(),
          () -> uploadPhoto(jobId, photo, executor));
    }

    return ImportResult.OK;
  }

  private String createAlbum(PhotoAlbum album) throws IOException {
    String albumName = album.getName() + IMPORTED_SUFFIX + JobMetadata.getExportService();
    AmazonPhotosNode node = client.createAlbum(albumName);
    monitor.info(() -> "Created album: " + album.getName() + " -> " + node.getId());
    return node.getId();
  }

  private String uploadPhoto(UUID jobId, PhotoModel photo,
                             IdempotentImportExecutor executor) throws Exception {
    String targetAlbumId = resolveTargetAlbumId(photo, executor);
    MessageDigest md5 = newMd5Digest();
    File tempFile = downloadToTempFile(jobId, photo, md5);

    try {
      String md5Hex = toHexString(md5.digest());
      long fileSize = tempFile.length();
      String contentDate = Optional.ofNullable(photo.getUploadedTime())
          .map(d -> d.toInstant().toString())
          .orElse(Instant.now().toString());
      boolean isFavorite = Optional.ofNullable(photo.getFavoriteInfo())
          .map(FavoriteInfo::getFavorited)
          .orElse(false);

      AmazonPhotosNode uploadedNode = client.uploadPhoto(
          photo.getTitle(), tempFile, md5Hex,
          fileSize, contentDate, isFavorite, targetAlbumId);

      return uploadedNode.getId();

    } catch (IOException e) {
      if (e.getMessage() != null && e.getMessage().contains("DuplicatesConflictError")) {
        monitor.info(() -> "Duplicate photo skipped: " + photo.getTitle());
        return photo.getDataId();
      }
      if (isStorageQuotaExceeded(e)) {
        throw new DestinationMemoryFullException("Amazon Photos storage full", e);
      }
      throw e;
    } finally {
      tempFile.delete();
      if (photo.isInTempStore()) {
        dataStore.removeData(jobId, photo.getFetchableUrl());
      }
    }
  }

  private String resolveTargetAlbumId(PhotoModel photo, IdempotentImportExecutor executor)
      throws Exception {
    if (photo.getAlbumId() != null && executor.isKeyCached(photo.getAlbumId())) {
      return executor.getCachedValue(photo.getAlbumId());
    }
    return null;
  }

  private void initializeClient(TokensAndUrlAuthData authData) throws IOException {
    if (client == null) {
      client = new AmazonPhotosClient(
          new okhttp3.OkHttpClient.Builder().build(),
          authData.getAccessToken(), authData.getRefreshToken(), clientId, clientSecret);
      client.resolveEndpoints();
    }
  }

  private File downloadToTempFile(UUID jobId, PhotoModel photo, MessageDigest md5)
      throws IOException {
    String prefix = photo.getDataId().replaceAll("[/\\\\]", "_");
    try (InputStream raw = connectionProvider.getInputStreamForItem(jobId, photo).getStream();
         DigestInputStream dis = new DigestInputStream(raw, md5)) {
      return dataStore.getTempFileFromInputStream(dis, prefix, ".tmp");
    }
  }

  private static MessageDigest newMd5Digest() {
    try {
      return MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("MD5 algorithm not available", e);
    }
  }

  private static String toHexString(byte[] bytes) {
    StringBuilder hex = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      hex.append(String.format("%02x", b));
    }
    return hex.toString();
  }

  private static boolean isStorageQuotaExceeded(IOException e) {
    String message = e.getMessage();
    return message != null && message.contains("InsufficientStorage");
  }
}
