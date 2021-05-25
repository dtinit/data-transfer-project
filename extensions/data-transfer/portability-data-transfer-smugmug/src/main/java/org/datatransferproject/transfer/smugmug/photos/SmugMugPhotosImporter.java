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

import static com.google.common.base.Preconditions.checkState;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
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

public class SmugMugPhotosImporter
    implements Importer<TokenSecretAuthData, PhotosContainerResource> {

  private static final String DEFAULT_ALBUM_NAME = "Untitled Album";
  private static final String COPY_PREFIX = "Copy of ";
  private final TemporaryPerJobDataStore jobStore;
  private final AppCredentials appCredentials;
  private final ObjectMapper mapper;
  private final Monitor monitor;
  private final SmugMugTransmogrificationConfig transmogrificationConfig;
  private final SmugMugInterface smugMugInterface;

  public SmugMugPhotosImporter(
      TemporaryPerJobDataStore jobStore,
      AppCredentials appCredentials,
      ObjectMapper mapper,
      Monitor monitor) {
    this(null, new SmugMugTransmogrificationConfig(), jobStore, appCredentials, mapper, monitor);
  }

  @VisibleForTesting
  SmugMugPhotosImporter(
      SmugMugInterface smugMugInterface,
      SmugMugTransmogrificationConfig transmogrificationConfig,
      TemporaryPerJobDataStore jobStore,
      AppCredentials appCredentials,
      ObjectMapper mapper,
      Monitor monitor) {
    this.smugMugInterface = smugMugInterface;
    this.transmogrificationConfig = transmogrificationConfig;
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
      SmugMugInterface smugMugInterface = getOrCreateSmugMugInterface(authData);
      for (PhotoAlbum album : data.getAlbums()) {
        idempotentExecutor.executeAndSwallowIOExceptions(
            album.getId(),
            album.getName(),
            () -> importSingleAlbum(jobId, album, smugMugInterface));
      }
      for (PhotoModel photo : data.getPhotos()) {
        idempotentExecutor.executeAndSwallowIOExceptions(
            photo.getAlbumId() + "-" + photo.getDataId(),
            photo.getTitle(),
            () -> importSinglePhoto(jobId, idempotentExecutor, photo, smugMugInterface));
      }
    } catch (IOException e) {
      monitor.severe(() -> "Error importing", e);
      return new ImportResult(e);
    }
    return ImportResult.OK;
  }

  @VisibleForTesting
  String importSingleAlbum(UUID jobId, PhotoAlbum inputAlbum, SmugMugInterface smugMugInterface)
      throws IOException {
    String albumName =
        Strings.isNullOrEmpty(inputAlbum.getName())
            ? DEFAULT_ALBUM_NAME
            : COPY_PREFIX + inputAlbum.getName();

    SmugMugAlbumResponse albumResponse = smugMugInterface.createAlbum(albumName);
    SmugMugPhotoTempData tempData =
        new SmugMugPhotoTempData(
            inputAlbum.getId(), albumName, inputAlbum.getDescription(), albumResponse.getUri());
    jobStore.create(jobId, getTempDataId(inputAlbum.getId()), tempData);
    return albumResponse.getUri();
  }

  @VisibleForTesting
  String importSinglePhoto(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      PhotoModel inputPhoto,
      SmugMugInterface smugMugInterface)
      throws Exception {
    InputStream inputStream;
    if (inputPhoto.isInTempStore()) {
      inputStream = jobStore.getStream(jobId, inputPhoto.getFetchableUrl()).getStream();
    } else {
      inputStream = smugMugInterface.getImageAsStream(inputPhoto.getFetchableUrl());
    }

    String originalAlbumId = inputPhoto.getAlbumId();
    SmugMugPhotoTempData albumTempData =
        getDestinationAlbumTempData(jobId, idempotentExecutor, originalAlbumId, smugMugInterface);

    SmugMugImageUploadResponse response =
        smugMugInterface.uploadImage(inputPhoto, albumTempData.getAlbumUri(), inputStream);
    albumTempData.incrementPhotoCount();
    jobStore.update(jobId, getTempDataId(albumTempData.getAlbumExportId()), albumTempData);

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
   * Get the proper album upload information for the photo. Takes into account size limits of the
   * albums and completed uploads.
   */
  @VisibleForTesting
  SmugMugPhotoTempData getDestinationAlbumTempData(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      String baseAlbumId,
      SmugMugInterface smugMugInterface)
      throws Exception {
    SmugMugPhotoTempData baseAlbumTempData =
        jobStore.findData(jobId, getTempDataId(baseAlbumId), SmugMugPhotoTempData.class);
    SmugMugPhotoTempData albumTempData = baseAlbumTempData;
    int depth = 0;
    while (albumTempData.getPhotoCount() >= transmogrificationConfig.getAlbumMaxSize()) {
      if (albumTempData.getOverflowAlbumExportId() == null) {
        PhotoAlbum newAlbum =
            createOverflowAlbum(
                baseAlbumTempData.getAlbumExportId(),
                baseAlbumTempData.getAlbumName(),
                baseAlbumTempData.getAlbumDescription(),
                depth + 1);
        // since the album is full and has no overflow, we need to create a new one
        String newUri =
            idempotentExecutor.executeOrThrowException(
                newAlbum.getId(),
                newAlbum.getName(),
                () -> importSingleAlbum(jobId, newAlbum, smugMugInterface));
        albumTempData.setOverflowAlbumExportId(newAlbum.getId());
        jobStore.update(jobId, getTempDataId(albumTempData.getAlbumExportId()), albumTempData);
        albumTempData =
            jobStore.findData(
                jobId,
                getTempDataId(albumTempData.getOverflowAlbumExportId()),
                SmugMugPhotoTempData.class);
      } else {
        albumTempData =
            jobStore.findData(
                jobId,
                getTempDataId(albumTempData.getOverflowAlbumExportId()),
                SmugMugPhotoTempData.class);
      }
      depth += 1;
    }
    return albumTempData;
  }

  private static String getTempDataId(String albumId) {
    return String.format("smugmug-album-temp-data-%s", albumId);
  }

  /**
   * Create an overflow album using the base album's id, name, and description and the overflow
   * album's opy number. E.g. if baseAlbum needs a single overflow album, it will be created with
   * createOverflowAlbum("baseAlbumId", "baseAlbumName", "baseAlbumDescription", 1) and result in an
   * album PhotoAlbum("baseAlbumId-overflow-1", "baseAlbumName (1)", "baseAlbumDescription")
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
