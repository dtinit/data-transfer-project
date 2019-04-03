/*
 * Copyright 2018 The Data-Portability Project Authors.
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
package org.datatransferproject.transfer.microsoft.photos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.provider.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.TokenAuthData;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkState;

/**
 * Imports albums and photos to OneDrive using the Microsoft Graph API.
 *
 * <p>The implementation currently uses the Graph Upload API, which has a content size limit of
 * 4MB. In the future, this can be enhanced to support large files (e.g. high resolution images and
 * videos) using the Upload Session API.
 */
public class MicrosoftPhotosImporter implements Importer<TokenAuthData, PhotosContainerResource> {

  private final OkHttpClient client;
  private final ObjectMapper objectMapper;
  private final TemporaryPerJobDataStore jobStore;
  private final Monitor monitor;

  private final String createFolderUrl;
  private final String uploadPhotoUrlTemplate;

  public MicrosoftPhotosImporter(
      String baseUrl,
      OkHttpClient client,
      ObjectMapper objectMapper,
      TemporaryPerJobDataStore jobStore,
      Monitor monitor) {
    createFolderUrl = baseUrl + "/v1.0/me/drive/special/photos/children";

    // first param is the folder id, second param is the file name
    // /me/drive/items/{parent-id}:/{filename}:/content;
    uploadPhotoUrlTemplate = baseUrl + "/v1.0/me/drive/items/%s:/%s:/content";

    this.client = client;
    this.objectMapper = objectMapper;
    this.jobStore = jobStore;
    this.monitor = monitor;
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      TokenAuthData authData,
      PhotosContainerResource resource) throws IOException {

    for (PhotoAlbum album : resource.getAlbums()) {
      // Create a OneDrive folder and then save the id with the mapping data
      idempotentExecutor.execute(
          album.getId(),
          album.getName(),
          () -> createOneDriveFolder(album, authData));
    }

    for (PhotoModel photoModel : resource.getPhotos()) {
      String folderId = idempotentExecutor.getCachedValue(photoModel.getAlbumId());
      idempotentExecutor.execute(
          Integer.toString(photoModel.hashCode()),
          photoModel.getTitle(),
          () -> importPhoto(photoModel, jobId, folderId, authData));
    }
    return ImportResult.OK;
  }

  @SuppressWarnings("unchecked")
  private String createOneDriveFolder(
      PhotoAlbum album, TokenAuthData authData) throws IOException {

    Map<String, Object> rawFolder = new LinkedHashMap<>();
    rawFolder.put("name", album.getName());
    rawFolder.put("folder", new Object());
    rawFolder.put("@microsoft.graph.conflictBehavior", "rename");

    Request.Builder requestBuilder = new Request.Builder().url(createFolderUrl);
    requestBuilder.header("Authorization", "Bearer " + authData.getToken());
    requestBuilder.post(
        RequestBody.create(
            MediaType.parse("application/json"), objectMapper.writeValueAsString(rawFolder)));
    try (Response response = client.newCall(requestBuilder.build()).execute()) {
      int code = response.code();
      if (code >= 200 && code <= 299) {
        ResponseBody body = response.body();
        if (body == null) {
          throw new IOException("Got null body");
        }
        Map<String, Object> responseData = objectMapper.readValue(body.bytes(), Map.class);
        String folderId = (String) responseData.get("id");
        checkState(!Strings.isNullOrEmpty(folderId),
            "Expected id value to be present in %s", responseData);
        return folderId;
      } else {
        throw new IOException("Got response code: " + code);
      }
      // FIXME evaluate HTTP response and return whether to retry
    }
  }

  private String importPhoto(
      PhotoModel photoModel,
      UUID jobId,
      String importedAlbumId,
      TokenAuthData authData) throws IOException {
    InputStream inputStream = null;

    try {
      if (photoModel.getDataId() != null) {
        inputStream = jobStore.getStream(jobId, photoModel.getDataId());
      } else if (photoModel.getFetchableUrl() != null) {
        inputStream = new URL(photoModel.getFetchableUrl()).openStream();
      } else {
        throw new IllegalStateException("Don't know how to get the inputStream for " + photoModel);
      }

      String uploadUrl =
          String.format(uploadPhotoUrlTemplate, importedAlbumId, photoModel.getTitle());

      Request.Builder requestBuilder = new Request.Builder().url(uploadUrl);
      requestBuilder.header("Authorization", "Bearer " + authData.getToken());

      MediaType contentType = MediaType.parse(photoModel.getMediaType());

      StreamingBody body = new StreamingBody(contentType, inputStream);

      // PUT the stream
      requestBuilder.put(body);

      try (Response response = client.newCall(requestBuilder.build()).execute()) {
        int code = response.code();
        if (code < 200 || code > 299) {
          throw new IOException("Got error code: " + code + " message " + response.message());
        }
        // TODO return photo ID
        return "fakeId";
      }
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e1) {
          monitor.info(() -> "Couldn't close input stream");
        }
      }
    }
  }

  private static class StreamingBody extends RequestBody {

    private final MediaType contentType;
    private final InputStream stream;

    public StreamingBody(MediaType contentType, InputStream stream) {
      this.contentType = contentType;
      this.stream = stream;
    }

    @Override
    public MediaType contentType() {
      return contentType;
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public void writeTo(BufferedSink sink) throws IOException {
      Source source = null;
      try {
        source = Okio.source(stream);
        sink.writeAll(source);
      } finally {
        Util.closeQuietly(source);
      }
    }
  }
}
