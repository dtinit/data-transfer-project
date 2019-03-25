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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.HttpTransport;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.provider.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
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

  private final JobStore jobStore;
  private final AppCredentials appCredentials;
  private final HttpTransport transport;
  private final ObjectMapper mapper;
  private final Monitor monitor;

  private SmugMugInterface smugMugInterface;

  public SmugMugPhotosImporter(
      JobStore jobStore,
      HttpTransport transport,
      AppCredentials appCredentials,
      ObjectMapper mapper,
      Monitor monitor) {
    this(null, jobStore, transport, appCredentials, mapper, monitor);
  }

  @VisibleForTesting
  SmugMugPhotosImporter(
      SmugMugInterface smugMugInterface,
      JobStore jobStore,
      HttpTransport transport,
      AppCredentials appCredentials,
      ObjectMapper mapper,
      Monitor monitor) {
    this.smugMugInterface = smugMugInterface;
    this.jobStore = jobStore;
    this.transport = transport;
    this.appCredentials = appCredentials;
    this.mapper = mapper;
    this.monitor = monitor;
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      TokenSecretAuthData authData,
      PhotosContainerResource data) {
    try {
      SmugMugInterface smugMugInterface = getOrCreateSmugMugInterface(authData);
      for (PhotoAlbum album : data.getAlbums()) {
        idempotentExecutor.execute(
            album.getId(),
            album.getName(),
            () -> importSingleAlbum(album, smugMugInterface));
      }
      for (PhotoModel photo : data.getPhotos()) {
        idempotentExecutor.execute(
            photo.getDataId(),
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
  String importSingleAlbum(PhotoAlbum inputAlbum, SmugMugInterface smugMugInterface)
      throws IOException {
    SmugMugAlbumResponse response = smugMugInterface.createAlbum(inputAlbum.getName());
    return response.getUri();
  }

  @VisibleForTesting
  String importSinglePhoto(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      PhotoModel inputPhoto,
      SmugMugInterface smugMugInterface)
      throws IOException {
    String newAlbumUri = idempotentExecutor.getCachedValue(inputPhoto.getAlbumId());
    checkState(
        !Strings.isNullOrEmpty(newAlbumUri),
        "Cached album URI for %s is null",
        inputPhoto.getAlbumId());

    InputStream inputStream;
    if (inputPhoto.isInTempStore()) {
      inputStream = jobStore.getStream(jobId, inputPhoto.getFetchableUrl());
    } else {
      inputStream = smugMugInterface.getImageAsStream(inputPhoto.getFetchableUrl());
    }
    SmugMugImageUploadResponse response =
        smugMugInterface.uploadImage(inputPhoto, newAlbumUri, inputStream);
    return response.toString();
  }

  // Returns the provided interface, or a new one specific to the authData provided.
  private SmugMugInterface getOrCreateSmugMugInterface(TokenSecretAuthData authData)
      throws IOException {
    return smugMugInterface == null
        ? new SmugMugInterface(transport, appCredentials, authData, mapper)
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
}
