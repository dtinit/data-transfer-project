/*
 * Copyright 2019 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.imgur.photos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import okhttp3.*;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

/** Imports albums and photos to Imgur */
public class ImgurPhotosImporter
    implements Importer<TokensAndUrlAuthData, PhotosContainerResource> {

  private final OkHttpClient client;
  private final ObjectMapper objectMapper;
  private final TemporaryPerJobDataStore jobStore;
  private final Monitor monitor;

  private final String CREATE_ALBUM_URL;
  private final String UPLOAD_PHOTO_URL;
  private static final String TEMP_PHOTOS_KEY = "tempPhotosData";

  public ImgurPhotosImporter(
      Monitor monitor,
      OkHttpClient client,
      ObjectMapper objectMapper,
      TemporaryPerJobDataStore jobStore,
      String baseUrl) {
    this.client = client;
    this.objectMapper = objectMapper;
    this.jobStore = jobStore;
    this.monitor = monitor;

    CREATE_ALBUM_URL = baseUrl + "/album";
    UPLOAD_PHOTO_URL = baseUrl + "/image";
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor executor,
      TokensAndUrlAuthData authData,
      PhotosContainerResource resource)
      throws Exception {
    if (resource == null) {
      // Nothing to import
      return ImportResult.OK;
    }

    // Import albums
    for (PhotoAlbum album : resource.getAlbums()) {
      executor.executeAndSwallowIOExceptions(
          album.getId(), album.getName(), () -> importAlbum(album, authData));
    }
    // Import photos
    for (PhotoModel photo : resource.getPhotos()) {
      executor.executeAndSwallowIOExceptions(
          photo.getDataId(),
          photo.getTitle(),
          () -> {
            String albumId;
            if (Strings.isNullOrEmpty(photo.getAlbumId())) {
              albumId = null;
            } else {
              albumId = executor.getCachedValue(photo.getAlbumId());
            }
            return importPhoto(photo, jobId, authData, albumId);
          });
    }

    return new ImportResult(ImportResult.ResultType.OK);
  }

  private String importAlbum(PhotoAlbum album, TokensAndUrlAuthData authData) throws IOException {
    String description = album.getDescription();

    Request.Builder requestBuilder = new Request.Builder().url(CREATE_ALBUM_URL);
    requestBuilder.header("Authorization", "Bearer " + authData.getAccessToken());

    FormBody.Builder builder = new FormBody.Builder().add("title", album.getName());
    if (!Strings.isNullOrEmpty(description)) {
      builder.add("description", description);
    }
    RequestBody formBody = builder.build();
    requestBuilder.post(formBody);

    Response response = client.newCall(requestBuilder.build()).execute();
    int code = response.code();
    Preconditions.checkArgument(
        code >= 200 && code <= 299,
        String.format(
            "Error occurred in request for %s, code: %s, message: %s",
            CREATE_ALBUM_URL, code, response.message()));
    ResponseBody body = response.body();
    Preconditions.checkArgument(body != null, "Didn't get response body!");
    Map<String, Object> responseData = objectMapper.readValue(body.bytes(), Map.class);

    String newAlbumId = (String) ((Map<String, Object>) responseData.get("data")).get("id");
    if (Strings.isNullOrEmpty(newAlbumId)) {
      throw new IOException("Didn't receive new album id");
    }

    return newAlbumId;
  }

  private int importPhoto(
      PhotoModel photoModel, UUID jobId, TokensAndUrlAuthData authData, String newAlbumId)
      throws IOException {
    InputStream inputStream = null;
    String albumId = photoModel.getAlbumId();
    String imageDescription = photoModel.getDescription();

    if (photoModel.isInTempStore()) {
      inputStream = jobStore.getStream(jobId, photoModel.getFetchableUrl()).getStream();
    } else if (photoModel.getFetchableUrl() != null) {
      inputStream = new URL(photoModel.getFetchableUrl()).openStream();
    } else {
      monitor.severe(() -> "Can't get inputStream for a photo");
      return -1;
    }

    byte[] imageBytes = ByteStreams.toByteArray(inputStream);
    String imageData = Base64.getEncoder().encodeToString(imageBytes);

    Request.Builder requestBuilder = new Request.Builder().url(UPLOAD_PHOTO_URL);
    requestBuilder.header("Authorization", "Bearer " + authData.getAccessToken());

    FormBody.Builder builder = new FormBody.Builder().add("image", imageData);

    if (!Strings.isNullOrEmpty(newAlbumId)) {
      builder.add("album", newAlbumId);
    }

    if (!Strings.isNullOrEmpty(imageDescription)) {
      builder.add("description", imageDescription);
    }
    FormBody formBody = builder.build();
    requestBuilder.post(formBody);

    Response response = client.newCall(requestBuilder.build()).execute();
    int code = response.code();
    // Though sometimes it returns error code for success requests
    Preconditions.checkArgument(
        code >= 200 && code <= 299,
        String.format(
            "Error occurred in request for %s, code: %s, message: %s",
            UPLOAD_PHOTO_URL, code, response.message()));
    return response.code();
  }

  /**
   * Key for cache of album mappings. TODO: Add a method parameter for a {@code key} for fine
   * grained objects.
   */
  private String createCacheKey() {
    // TODO: store objects containing individual mappings instead of single object containing all
    // mappings
    return TEMP_PHOTOS_KEY;
  }
}
