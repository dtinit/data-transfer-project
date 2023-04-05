/*
 * Copyright 2021 The Data-Portability Project Authors.
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

package org.datatransferproject.transfer.daybook.photos;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.idempotentexecutor.ItemImportResult;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

/**
 * Imports albums and photos to Daybook
 */
public class DaybookPhotosImporter
    implements Importer<TokensAndUrlAuthData, PhotosContainerResource> {

  private final OkHttpClient client;
  private final TemporaryPerJobDataStore jobStore;
  private final Monitor monitor;
  private final String baseUrl;
  private final String exportService;

  public DaybookPhotosImporter(
      Monitor monitor,
      OkHttpClient client,
      TemporaryPerJobDataStore jobStore,
      String baseUrl,
      String exportService) {
    this.client = client;
    this.jobStore = jobStore;
    this.monitor = monitor;
    this.baseUrl = baseUrl;
    this.exportService = exportService;
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

    monitor.debug(() -> String.format("Number of Photos: %d", resource.getPhotos().size()));

    // Import albums
    for (PhotoAlbum album : resource.getAlbums()) {
      executor.executeAndSwallowIOExceptions(
          album.getId(), album.getName(), () -> importAlbum(album));
    }

    // Import photos
    for (PhotoModel photo : resource.getPhotos()) {
      executor.importAndSwallowIOExceptions(
          photo,
          photoModel -> {
            String albumId;
            if (Strings.isNullOrEmpty(photo.getAlbumId())) {
              albumId = null;
            } else {
              albumId = executor.getCachedValue(photo.getAlbumId());
            }
            return importPhoto(photo, jobId, authData, albumId);
          });
    }

    // API to trigger daybook API

    return new ImportResult(ImportResult.ResultType.OK);
  }

  private String importAlbum(PhotoAlbum album) {
    String description = album.getDescription();
    String album_name = album.getName();
    monitor.debug(() -> String.format("Album Name: %s", album_name));
    if (!Strings.isNullOrEmpty(description)) {
      monitor.debug(() -> String.format("Album description: %s", description));
    }

    return album_name;
  }

  private ItemImportResult<Integer> importPhoto(
      PhotoModel photoModel, UUID jobId, TokensAndUrlAuthData authData, String newAlbumId)
      throws IOException {
    InputStream inputStream;
    String imageDescription = photoModel.getDescription();
    String title = photoModel.getTitle();

    if (photoModel.isInTempStore()) {
      inputStream = jobStore.getStream(jobId, photoModel.getFetchableUrl()).getStream();
    } else if (photoModel.getFetchableUrl() != null) {
      inputStream = new URL(photoModel.getFetchableUrl()).openStream();
    } else {
      String errorMessage = "Can't get inputStream for a photo";
      monitor.severe(() -> errorMessage);
      return ItemImportResult.error(new IOException(errorMessage), null);
    }

    byte[] imageBytes = ByteStreams.toByteArray(inputStream);
    String imageData = Base64.getEncoder().encodeToString(imageBytes);

    Request.Builder requestBuilder = new Request.Builder().url(baseUrl);
    requestBuilder.header("token", authData.getAccessToken());

    FormBody.Builder builder =
        new FormBody.Builder().add("image", imageData).add("exporter", exportService);

    if (!Strings.isNullOrEmpty(newAlbumId)) {
      builder.add("album", newAlbumId);
    }

    if (!Strings.isNullOrEmpty(title)) {
      builder.add("title", title);
    }

    if (!Strings.isNullOrEmpty(imageDescription)) {
      builder.add("description", imageDescription);
    }
    FormBody formBody = builder.build();
    requestBuilder.post(formBody);

    try (Response response = client.newCall(requestBuilder.build()).execute()) {
      int code = response.code();
      // Though sometimes it returns error code for success requests
      Preconditions.checkArgument(
          code >= 200 && code <= 299,
          String.format(
              "Error occurred in request for %s, code: %s, message: %s",
              baseUrl, code, response.message()));

      if (photoModel.isInTempStore()) {
        jobStore.removeData(jobId, photoModel.getFetchableUrl());
      }
      return ItemImportResult.success(response.code(), (long) imageBytes.length);
    }
  }
}
