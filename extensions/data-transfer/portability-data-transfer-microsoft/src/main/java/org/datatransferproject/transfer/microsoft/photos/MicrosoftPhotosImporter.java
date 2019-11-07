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
import com.google.api.client.auth.oauth2.Credential;
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
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.microsoft.common.MicrosoftCredentialFactory;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

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
public class MicrosoftPhotosImporter implements Importer<TokensAndUrlAuthData, PhotosContainerResource> {

  private final OkHttpClient client;
  private final ObjectMapper objectMapper;
  private final TemporaryPerJobDataStore jobStore;
  private final Monitor monitor;
  private final MicrosoftCredentialFactory credentialFactory;
  private Credential credential;

  private final String createFolderUrl;
  private final String uploadPhotoUrlTemplate;
  private final String albumlessPhotoUrlTemplate;

  private String UPLOAD_PARAMS = "?@microsoft.graph.conflictBehavior=rename";

  public MicrosoftPhotosImporter(
      String baseUrl,
      OkHttpClient client,
      ObjectMapper objectMapper,
      TemporaryPerJobDataStore jobStore,
      Monitor monitor,
      MicrosoftCredentialFactory credentialFactory) {
    createFolderUrl = baseUrl + "/v1.0/me/drive/special/photos/children";

    // first param is the folder id, second param is the file name
    // /me/drive/items/{parent-id}:/{filename}:/content;
    uploadPhotoUrlTemplate = baseUrl + "/v1.0/me/drive/items/%s:/%s:/content%s";

    albumlessPhotoUrlTemplate = baseUrl + "/v1.0/me/drive/root:/Pictures/%s:/content%s";

    this.client = client;
    this.objectMapper = objectMapper;
    this.jobStore = jobStore;
    this.monitor = monitor;
    this.credentialFactory = credentialFactory;
    this.credential = null;
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentImportExecutor,
      TokensAndUrlAuthData authData,
      PhotosContainerResource resource)
      throws Exception {
    // Ensure credential is populated
    getOrCreateCredential(authData);

    for (PhotoAlbum album : resource.getAlbums()) {
      // Create a OneDrive folder and then save the id with the mapping data
      idempotentImportExecutor.executeAndSwallowIOExceptions(
          album.getId(), album.getName(), () -> createOneDriveFolder(album));
    }

    for (PhotoModel photoModel : resource.getPhotos()) {

      idempotentImportExecutor.executeAndSwallowIOExceptions(
          Integer.toString(photoModel.hashCode()),
          photoModel.getTitle(),
          () -> {
            return importSinglePhoto(photoModel, jobId, idempotentImportExecutor);
          });
    }
    return ImportResult.OK;
  }

  @SuppressWarnings("unchecked")
  private String createOneDriveFolder(PhotoAlbum album) throws IOException {

    Map<String, Object> rawFolder = new LinkedHashMap<>();
    rawFolder.put("name", album.getName());
    rawFolder.put("folder", new LinkedHashMap());
    rawFolder.put("@microsoft.graph.conflictBehavior", "rename");

    Request.Builder requestBuilder = new Request.Builder().url(createFolderUrl);
    requestBuilder.header("Authorization", "Bearer " + credential.getAccessToken());
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

  private String importSinglePhoto(
      PhotoModel photo,
      UUID jobId,
      IdempotentImportExecutor idempotentImportExecutor)
      throws IOException {
    InputStream inputStream = null;

    try {
      String uploadUrl = null;
      if (Strings.isNullOrEmpty(photo.getAlbumId())) {
        uploadUrl = String.format(albumlessPhotoUrlTemplate, photo.getTitle(), UPLOAD_PARAMS);
      } else {
        String oneDriveFolderId = idempotentImportExecutor.getCachedValue(photo.getAlbumId());
        uploadUrl =
            String.format(
                uploadPhotoUrlTemplate, oneDriveFolderId, photo.getTitle(), UPLOAD_PARAMS);
      }

      if (photo.isInTempStore()) {
        inputStream = jobStore.getStream(jobId, photo.getFetchableUrl());
      } else if (photo.getFetchableUrl() != null) {
        inputStream = new URL(photo.getFetchableUrl()).openStream();
      } else {
        throw new IllegalStateException("Don't know how to get the inputStream for " + photo);
      }

      Request.Builder requestBuilder = new Request.Builder().url(uploadUrl);
      requestBuilder.header("Authorization", "Bearer " + credential.getAccessToken());

      MediaType contentType = MediaType.parse(photo.getMediaType());

      StreamingBody body = new StreamingBody(contentType, inputStream);

      // PUT the stream
      requestBuilder.put(body);

      try (Response response = client.newCall(requestBuilder.build()).execute()) {
        int code = response.code();
        ResponseBody responseBody = response.body();
        if (code == 401){
            // If there was an unauthorized error, then try refreshing the creds
            credentialFactory.refreshCredential(credential);
            monitor.info(() -> "Refreshed authorization token successfuly");

            requestBuilder.header("Authorization", "Bearer " + credential.getAccessToken());
            Response newResponse = client.newCall(requestBuilder.build()).execute();
            code = newResponse.code();
            body = newResponse.body();
        }
        if (code < 200 || code > 299) {
          throw new IOException("Got error code: " + code + " message " + response.message());
        }

        // Extract photo ID from response body
        if (body == null) {
          throw new IOException("Got null body");
        }
        Map<String, Object> responseData = objectMapper.readValue(responseBody.bytes(), Map.class);
        String photoId = (String) responseData.get("id");
        checkState(!Strings.isNullOrEmpty(photoId),
            "Expected id value to be present in %s", responseData);
        return photoId;
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

  private Credential getOrCreateCredential(TokensAndUrlAuthData authData){
    if (this.credential == null){
      this.credential = this.credentialFactory.createCredential(authData);
    }
    return this.credential;
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
