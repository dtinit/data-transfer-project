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
package org.dataportabilityproject.transfer.microsoft.photos;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.transfer.provider.ImportResult;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.transfer.microsoft.types.TempPhotoData;
import org.dataportabilityproject.types.transfer.auth.TokenAuthData;
import org.dataportabilityproject.types.transfer.models.photos.PhotoAlbum;
import org.dataportabilityproject.types.transfer.models.photos.PhotoModel;
import org.dataportabilityproject.types.transfer.models.photos.PhotosContainerResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Imports albums and photos to OneDrive using the Microsoft Graph API.
 *
 * <p>The implementation currently uses the Graph Upload API, which has a content size limit of 4MB.
 * In the future, this can be enhanced to support large files (e.g. high resolution images and
 * videos) using the Upload Session API.
 */
public class MicrosoftPhotosImporter implements Importer<TokenAuthData, PhotosContainerResource> {

  private final OkHttpClient client;
  private final ObjectMapper objectMapper;
  private final JobStore jobStore;

  private final String createFolderUrl;
  private final String uploadPhotoUrlTemplate;

  public MicrosoftPhotosImporter(
      String baseUrl, OkHttpClient client, ObjectMapper objectMapper, JobStore jobStore) {
    createFolderUrl = baseUrl + "/v1.0/me/drive/special/photos/children";

    // first param is the folder id, second param is the file name
    // /me/drive/items/{parent-id}:/{filename}:/content;
    uploadPhotoUrlTemplate = baseUrl + "/v1.0/me/drive/items/%s:/%s:/content";

    this.client = client;
    this.objectMapper = objectMapper;
    this.jobStore = jobStore;
  }

  @Override
  public ImportResult importItem(
      UUID jobId, TokenAuthData authData, PhotosContainerResource resource) {
    TempPhotoData photoData = jobStore.findData(jobId, createCacheKey(), TempPhotoData.class);
    if (photoData == null) {
      photoData = new TempPhotoData(jobId.toString());
      try {
        jobStore.create(jobId, createCacheKey(), photoData);
      } catch (IOException e) {
        return new ImportResult(ImportResult.ResultType.ERROR, "Error create temp photo data " + e.getMessage());
      }

    }

    for (PhotoAlbum album : resource.getAlbums()) {
      // Create a OneDrive folder and then save the id with the mapping data
      createOneDriveFolder(album, jobId, authData, photoData);
    }

    for (PhotoModel photoModel : resource.getPhotos()) {
      importPhoto(photoModel, jobId, authData, photoData);
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private void createOneDriveFolder(
      PhotoAlbum album, UUID jobId, TokenAuthData authData, TempPhotoData photoData) {

    try {
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
            // FIXME evaluate HTTP response and return whether to retry
            return;
          }
          Map<String, Object> responseData = objectMapper.readValue(body.bytes(), Map.class);
          String folderId = (String) responseData.get("id");
          if (folderId == null) {
            // this should never happen
            return;
          }
          photoData.addIdMapping(album.getId(), folderId);
          jobStore.update(jobId, createCacheKey(), photoData);
        }
        // FIXME evaluate HTTP response and return whether to retry
      }
    } catch (IOException e) {
      // TODO log
      e.printStackTrace();
    }
  }

  private void importPhoto(
      PhotoModel photoModel, UUID jobId, TokenAuthData authData, TempPhotoData photoData) {
    InputStream inputStream = null;

    try {
      if (photoModel.getDataId() != null) {
        inputStream = jobStore.getStream(jobId, photoModel.getDataId());
      } else if (photoModel.getFetchableUrl() != null) {
        inputStream = new URL(photoModel.getFetchableUrl()).openStream();
      } else {
        return;
      }

      String importedAlbumId = photoData.getImportedId(photoModel.getAlbumId());
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
          // TODO log error
        }
      }
    } catch (IOException e) {
      // TODO log
      e.printStackTrace();
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e1) {
          // TODO log
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

  /** Key for cache of album mappings.
   * TODO: Add a method parameter for a {@code key} for fine grained objects.
   */
  private String createCacheKey() {
    // TODO: store objects containing individual mappings instead of single object containing all mappings
    return "tempPhotosData";
  }
}
