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

package org.datatransferproject.transfer.smugmug.photos;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.smugmug.SmugMugTransmogrificationConfig;
import org.datatransferproject.transfer.smugmug.photos.model.SmugMugAlbumResponse;
import org.datatransferproject.transfer.smugmug.photos.model.SmugMugImageUploadResponse;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkState;

public class SmugMugPhotosImporter
    implements Importer<TokenSecretAuthData, PhotosContainerResource> {

  private final TemporaryPerJobDataStore jobStore;
  private final AppCredentials appCredentials;
  private final ObjectMapper mapper;
  private final Monitor monitor;
  private final SmugMugTransmogrificationConfig transmogrificationConfig =
      new SmugMugTransmogrificationConfig();

  private SmugMugInterface smugMugInterface;

  public SmugMugPhotosImporter(
      TemporaryPerJobDataStore jobStore,
      AppCredentials appCredentials,
      ObjectMapper mapper,
      Monitor monitor) {
    this(null, jobStore, appCredentials, mapper, monitor);
  }

  @VisibleForTesting
  SmugMugPhotosImporter(
      SmugMugInterface smugMugInterface,
      TemporaryPerJobDataStore jobStore,
      AppCredentials appCredentials,
      ObjectMapper mapper,
      Monitor monitor) {
    this.smugMugInterface = smugMugInterface;
    this.jobStore = jobStore;
    this.appCredentials = appCredentials;
    this.mapper = mapper;
    this.monitor = monitor;
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      TokenSecretAuthData authData,
      PhotosContainerResource data)
      throws Exception {

    // Make the data smugmug compatible
    data.transmogrify(transmogrificationConfig);

    try {
      this.smugMugInterface = getOrCreateSmugMugInterface(authData);
      for (PhotoAlbum album : data.getAlbums()) {
        String albumUri =
            idempotentExecutor.executeAndSwallowIOExceptions(
                album.getId(),
                album.getName(),
                () -> importSingleAlbum(jobId, album));
      }
      for (PhotoModel photo : data.getPhotos()) {
        idempotentExecutor.executeAndSwallowIOExceptions(
            photo.getAlbumId() + "-" + photo.getDataId(),
            photo.getTitle(),
            () -> importSinglePhoto(jobId, idempotentExecutor, photo));
      }
    } catch (IOException e) {
      monitor.severe(() -> "Error importing", e);
      return new ImportResult(e);
    }
    return ImportResult.OK;
  }

  @VisibleForTesting
  String importSingleAlbum(UUID jobId, PhotoAlbum inputAlbum)
      throws IOException {
    SmugMugAlbumResponse albumResponse = smugMugInterface.createAlbum(inputAlbum.getName());
    SmugMugPhotoTempData tempData =
        new SmugMugPhotoTempData(
            inputAlbum.getId(), inputAlbum.getName(), inputAlbum.getDescription());
    jobStore.create(jobId, inputAlbum.getId(), tempData);
    return albumResponse.getUri();
  }

  @VisibleForTesting
  String importSinglePhoto(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      PhotoModel inputPhoto)
      throws Exception {
    SmugMugPhotoTempData albumTempData =
        getAlbumTempData(jobId, idempotentExecutor, inputPhoto.getAlbumId());
    inputPhoto.reassignToAlbum(albumTempData.getAlbumId());
    String albumUri = idempotentExecutor.getCachedValue(inputPhoto.getAlbumId());

    InputStream inputStream;
    if (inputPhoto.isInTempStore()) {
      inputStream = jobStore.getStream(jobId, inputPhoto.getFetchableUrl()).getStream();
    } else {
      inputStream = smugMugInterface.getImageAsStream(inputPhoto.getFetchableUrl());
    }
    SmugMugImageUploadResponse response =
        smugMugInterface.uploadImage(inputPhoto, albumUri, inputStream);
    albumTempData.incrementPhotoCount();
    jobStore.update(jobId, albumTempData.getAlbumId(), albumTempData);
    return response.toString();
  }

  // Returns the provided interface, or a new one specific to the authData provided.
  private SmugMugInterface getOrCreateSmugMugInterface(TokenSecretAuthData authData)
      throws IOException {
    return smugMugInterface == null
        ? new SmugMugInterface(appCredentials, authData, mapper)
        : smugMugInterface;
  }

  /**
   * Key for cache of album mappings. TODO: Add a method parameter for a {@code key} for fine
   * grained objects.
   */
  private String createCacheKey() {
    // TODO: store objects containing individual mappings instead of single object containing all
    // mappings
    return "tempPhotosData";
  }

  /**
   * Get the proper album upload information for the photo. Takes into account size limits of the
   * albums and completed uploads.
   */
  private SmugMugPhotoTempData getAlbumTempData(
      UUID jobId, IdempotentImportExecutor idempotentExecutor, String baseAlbumId)
      throws Exception {
    SmugMugPhotoTempData baseAlbumTempData =
        jobStore.findData(jobId, baseAlbumId, SmugMugPhotoTempData.class);
    SmugMugPhotoTempData albumTempData = baseAlbumTempData;
    int depth = 0;
    while (albumTempData.getPhotoCount() >= transmogrificationConfig.getAlbumMaxSize()) {
      if (albumTempData.getOverflowAlbumId() == null) {
        PhotoAlbum newAlbum =
            createOverflowAlbum(
                baseAlbumTempData.getAlbumId(),
                baseAlbumTempData.getAlbumName(),
                baseAlbumTempData.getAlbumDescription(),
                depth + 1);
        String newUri =
            idempotentExecutor.executeOrThrowException(
                newAlbum.getId(),
                newAlbum.getName(),
                () -> importSingleAlbum(jobId, newAlbum, smugMugInterface));
        albumTempData.setOverflowAlbumId(newAlbum.getId());
        jobStore.update(jobId, albumTempData.getAlbumId(), albumTempData);
        albumTempData =
            jobStore.findData(
                jobId, albumTempData.getOverflowAlbumId(), SmugMugPhotoTempData.class);
      } else {
        albumTempData =
            jobStore.findData(
                jobId, albumTempData.getOverflowAlbumId(), SmugMugPhotoTempData.class);
      }
      depth += 1;
    }
    return albumTempData;
  }

  /**
   * Create an overflow album id -> {baseAlbumId}-overflow-{copyNumber} name -> {baseAlbumName}
   * ({copyNumber}) description -> {baseAlbumDescription}
   */
  private static PhotoAlbum createOverflowAlbum(
      String baseAlbumId, String baseAlbumName, String baseAlbumDescription, int copyNumber)
      throws Exception {
    checkState(copyNumber > 0, "copyNumber should be > 0");
    return new PhotoAlbum(
        String.format("%s-overflow-%d", baseAlbumId, copyNumber),
        String.format("%s (%d)", baseAlbumName, copyNumber),
        baseAlbumDescription);
  }

}
