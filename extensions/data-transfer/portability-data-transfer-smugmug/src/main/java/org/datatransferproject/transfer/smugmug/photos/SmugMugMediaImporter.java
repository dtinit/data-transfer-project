/*
 * Copyright 2022 The Data Transfer Project Authors.
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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
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
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;

public class SmugMugMediaImporter
    implements Importer<TokenSecretAuthData, MediaContainerResource> {

  private final TemporaryPerJobDataStore jobStore;
  private final AppCredentials appCredentials;
  private final ObjectMapper mapper;
  private final Monitor monitor;
  private final SmugMugTransmogrificationConfig transmogrificationConfig;
  private final SmugMugInterface smugMugInterface;
  private static final String DEFAULT_ALBUM_NAME = "Untitled Album";

  public SmugMugMediaImporter(
      TemporaryPerJobDataStore jobStore,
      AppCredentials appCredentials,
      ObjectMapper mapper,
      Monitor monitor) {
    this(null, new SmugMugTransmogrificationConfig(), jobStore, appCredentials, mapper, monitor);
  }

  @VisibleForTesting
  SmugMugMediaImporter(
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
      MediaContainerResource data)
      throws Exception {

    // Make the data smugmug compatible
    data.transmogrify(transmogrificationConfig);

    try {
      SmugMugInterface smugMugInterface = getOrCreateSmugMugInterface(authData);
      for (MediaAlbum album : data.getAlbums()) {
        idempotentExecutor.executeAndSwallowIOExceptions(
            album.getId(),
            album.getName(),
            () -> importSingleAlbum(jobId, album, smugMugInterface));
      }
      if (data.getPhotos() != null) {
        for (PhotoModel photo : data.getPhotos()) {
          idempotentExecutor.executeAndSwallowIOExceptions(
              photo.getAlbumId() + "-" + photo.getDataId(),
              photo.getTitle(),
              () -> importSinglePhoto(jobId, idempotentExecutor, photo, smugMugInterface));
        }
      }
      if (data.getVideos() != null) {
        for (VideoModel video : data.getVideos()) {
          idempotentExecutor.executeAndSwallowIOExceptions(
              video.getAlbumId() + "-" + video.getDataId(),
              video.getName(),
              () -> importSingleVideo(jobId, idempotentExecutor, video, smugMugInterface));
        }
      }

    } catch (IOException e) {
      monitor.severe(() -> "Error importing", e);
      return new ImportResult(e);
    }
    return ImportResult.OK;
  }

  @VisibleForTesting
  String importSingleAlbum(UUID jobId, MediaAlbum inputAlbum, SmugMugInterface smugMugInterface)
      throws IOException {
    String albumName =
        Strings.isNullOrEmpty(inputAlbum.getName())
            ? DEFAULT_ALBUM_NAME
            : inputAlbum.getName();

    SmugMugAlbumResponse albumResponse = smugMugInterface.createAlbum(albumName);
    return albumResponse.getUri();
  }

  @VisibleForTesting
  String importSinglePhoto(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      PhotoModel inputPhoto,
      SmugMugInterface smugMugInterface)
      throws Exception {
    // Get the cached album URI that corresponds to the photo album on the Smugmug end. Albums are
    // created before the photos that are contained in them are listed. In addition each inputPhoto
    // will always have an album id because smugmug does not allow albumless images, before the
    // image reaches here, the transmogrifier assigns the root album id to any images without an
    // album.
    String albumUri = idempotentExecutor.getCachedValue(inputPhoto.getAlbumId());
    Preconditions.checkState(
        !Strings.isNullOrEmpty(albumUri),
        "Cached album URI for %s is null",
        inputPhoto.getAlbumId());

    InputStream inputStream;
    if (inputPhoto.isInTempStore()) {
      inputStream = jobStore.getStream(jobId, inputPhoto.getFetchableUrl()).getStream();
    } else {
      inputStream = smugMugInterface.getImageAsStream(inputPhoto.getFetchableUrl());
    }

    SmugMugImageUploadResponse response =
        smugMugInterface.uploadImage(inputPhoto, albumUri, inputStream);

    return response.toString();
  }

  @VisibleForTesting
  String importSingleVideo(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      VideoModel inputVideo,
      SmugMugInterface smugMugInterface)
      throws Exception {
    // Get the cached album URI that corresponds to the album on the Smugmug end. Albums are
    // created before the items that are contained in them are listed. In addition each inputVideo
    // will always have an album id because smugmug does not allow albumless media, before the
    // video reaches here, the transmogrifier assigns the root album id to any images without an
    // album.
    String albumUri = idempotentExecutor.getCachedValue(inputVideo.getAlbumId());
    Preconditions.checkState(
        !Strings.isNullOrEmpty(albumUri),
        "Cached album URI for %s is null",
        inputVideo.getAlbumId());

    InputStream inputStream;
    if (inputVideo.isInTempStore()) {
      inputStream = jobStore.getStream(jobId, inputVideo.getFetchableUrl()).getStream();
    } else {
      inputStream = smugMugInterface.getImageAsStream(inputVideo.getFetchableUrl());
    }

    SmugMugImageUploadResponse response =
        smugMugInterface.uploadVideo(inputVideo, albumUri, inputStream);

    return response.toString();
  }

  // Returns the provided interface, or a new one specific to the authData provided.
  private SmugMugInterface getOrCreateSmugMugInterface(TokenSecretAuthData authData)
      throws IOException {
    return smugMugInterface == null
        ? new SmugMugInterface(appCredentials, authData, mapper)
        : smugMugInterface;
  }
}
