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
import com.google.api.client.http.HttpTransport;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.spi.transfer.types.TempPhotosData;
import org.datatransferproject.transfer.smugmug.photos.model.ImageUploadResponse;
import org.datatransferproject.transfer.smugmug.photos.model.SmugMugAlbumResponse;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;
import org.datatransferproject.types.transfer.models.photos.PhotoAlbum;
import org.datatransferproject.types.transfer.models.photos.PhotoModel;
import org.datatransferproject.types.transfer.models.photos.PhotosContainerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmugMugPhotosImporter
    implements Importer<TokenSecretAuthData, PhotosContainerResource> {

  private static final Logger LOGGER = LoggerFactory.getLogger(SmugMugPhotosImporter.class);

  private final JobStore jobStore;
  private final AppCredentials appCredentials;
  private final HttpTransport transport;
  private final ObjectMapper mapper;

  private SmugMugInterface smugMugInterface;

  public SmugMugPhotosImporter(
      JobStore jobStore,
      HttpTransport transport,
      AppCredentials appCredentials,
      ObjectMapper mapper) {
    this(null, jobStore, transport, appCredentials, mapper);
  }

  @VisibleForTesting
  SmugMugPhotosImporter(
      SmugMugInterface smugMugInterface,
      JobStore jobStore,
      HttpTransport transport,
      AppCredentials appCredentials,
      ObjectMapper mapper) {
    this.smugMugInterface = smugMugInterface;
    this.jobStore = jobStore;
    this.transport = transport;
    this.appCredentials = appCredentials;
    this.mapper = mapper;
  }

  @Override
  public ImportResult importItem(
      UUID jobId, TokenSecretAuthData authData, PhotosContainerResource data) {
    try {
      SmugMugInterface smugMugInterface = getOrCreateSmugMugInterface(authData);
      for (PhotoAlbum album : data.getAlbums()) {
        importSingleAlbum(jobId, album, smugMugInterface);
      }
      for (PhotoModel photo : data.getPhotos()) {
        importSinglePhoto(jobId, photo, smugMugInterface);
      }
    } catch (IOException e) {
      LOGGER.warn("Error happened while importing: {}", Throwables.getStackTraceAsString(e));
      return new ImportResult(e);
    }
    return ImportResult.OK;
  }

  @VisibleForTesting
  void importSingleAlbum(UUID jobId, PhotoAlbum inputAlbum, SmugMugInterface smugMugInterface)
      throws IOException {
    SmugMugAlbumResponse response = smugMugInterface.createAlbum(inputAlbum.getName());

    // Put new album ID in job store so photos can be assigned to correct album
    // TODO(olsona): thread safety!
    TempPhotosData tempPhotosData = jobStore
        .findData(jobId, createCacheKey(), TempPhotosData.class);
    if (tempPhotosData == null) {
      tempPhotosData = new TempPhotosData(jobId);
      jobStore.create(jobId, createCacheKey(), tempPhotosData);
    }
    tempPhotosData.addAlbumId(inputAlbum.getId(), response.getUri());
  }

  @VisibleForTesting
  void importSinglePhoto(UUID jobId, PhotoModel inputPhoto, SmugMugInterface smugMugInterface)
      throws IOException {
    // Find album to upload photo to
    String newAlbumUri =
        jobStore.findData(jobId, createCacheKey(), TempPhotosData.class)
            .lookupNewAlbumId(inputPhoto.getAlbumId());

    checkState(
        !Strings.isNullOrEmpty(newAlbumUri),
        "Cached album URI for %s is null",
        inputPhoto.getAlbumId());

    InputStream inputStream = getImageAsStream(inputPhoto.getFetchableUrl());
    ImageUploadResponse response = smugMugInterface
        .uploadImage(inputPhoto, newAlbumUri, inputStream);
  }

  // Returns the provided interface, or a new one specific to the authData provided.
  private SmugMugInterface getOrCreateSmugMugInterface(TokenSecretAuthData authData)
      throws IOException {
    return smugMugInterface == null
        ? new SmugMugInterface(transport, appCredentials, authData, mapper)
        : smugMugInterface;
  }

  private InputStream getImageAsStream(String urlStr) throws IOException {
    URL url = new URL(urlStr);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.connect();
    return conn.getInputStream();
  }

  /**
   * Key for cache of album mappings. TODO: Add a method parameter for a {@code key} for fine
   * grained objects.
   */
  private String createCacheKey() {
    // TODO: store objects containing individual mappings instead of single object containing all mappings
    return "tempPhotosData";
  }
}
