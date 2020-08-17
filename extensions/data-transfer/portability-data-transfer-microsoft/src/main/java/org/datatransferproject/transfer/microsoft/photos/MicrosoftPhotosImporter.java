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
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.datatransferproject.transfer.microsoft.DataChunk;
import org.datatransferproject.transfer.microsoft.MicrosoftTransmogrificationConfig;
import org.datatransferproject.transfer.microsoft.common.MicrosoftCredentialFactory;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

/**
 * Imports albums and photos to OneDrive using the Microsoft Graph API.
 */
public class MicrosoftPhotosImporter
    implements Importer<TokensAndUrlAuthData, PhotosContainerResource> {

  private final OkHttpClient client;
  private final ObjectMapper objectMapper;
  private final TemporaryPerJobDataStore jobStore;
  private final Monitor monitor;
  private final MicrosoftCredentialFactory credentialFactory;
  private final MicrosoftTransmogrificationConfig transmogrificationConfig =
      new MicrosoftTransmogrificationConfig();
  private Credential credential;

  private final String createFolderUrl;
  private final String uploadPhotoUrlTemplate;
  private final String albumlessPhotoUrlTemplate;

  private static final String UPLOAD_PARAMS = "?@microsoft.graph.conflictBehavior=rename";

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
    uploadPhotoUrlTemplate = baseUrl + "/v1.0/me/drive/items/%s:/%s:/createUploadSession%s";
    albumlessPhotoUrlTemplate =
        baseUrl + "/v1.0/me/drive/special/photos:/%s:/createUploadSession%s";

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

    monitor.debug(
      () -> String
      .format("%s: Importing %s albums and %s photos before transmogrification", jobId,
              resource.getAlbums().size(), resource.getPhotos().size()));


    // Make the data onedrive compatible
    resource.transmogrify(transmogrificationConfig);
    monitor.debug(
      () -> String.format("%s: Importing %s albums and %s photos after transmogrification", jobId,
                          resource.getAlbums().size(), resource.getPhotos().size()));


    for (PhotoAlbum album : resource.getAlbums()) {
      // Create a OneDrive folder and then save the id with the mapping data
      idempotentImportExecutor.executeAndSwallowIOExceptions(
        album.getId(), album.getName(), () -> createOneDriveFolder(album));
    }

    for (PhotoModel photoModel : resource.getPhotos()) {
      idempotentImportExecutor.executeAndSwallowIOExceptions(
        photoModel.getAlbumId() + "-" + photoModel.getDataId(),
        photoModel.getTitle(),
        () -> importSinglePhoto(photoModel, jobId, idempotentImportExecutor));
    }
    return ImportResult.OK;
  }

  @SuppressWarnings("unchecked")
  private String createOneDriveFolder(PhotoAlbum album) throws IOException, CopyExceptionWithFailureReason {

    Map<String, Object> rawFolder = new LinkedHashMap<>();
    // clean up album name for microsoft specifically
    // Note that PhotoAlbum.getName() can return an empty string or null depending
    // on the results of PhotoAlbum.cleanName(), e.g. if a Google Photos album has
    // title=" ", its cleaned name will be "". See PhotoAlbum.cleanName for further
    // details on what forms the name can take.
    String albumName = Strings.isNullOrEmpty(album.getName()) ? "Untitled" : album.getName();
    rawFolder.put("name", albumName);
    rawFolder.put("folder", new LinkedHashMap());
    rawFolder.put("@microsoft.graph.conflictBehavior", "rename");

    Request.Builder requestBuilder = new Request.Builder().url(createFolderUrl);
    requestBuilder.header("Authorization", "Bearer " + credential.getAccessToken());
    requestBuilder.post(
      RequestBody.create(
        MediaType.parse("application/json"), objectMapper.writeValueAsString(rawFolder)));
    try (Response response = client.newCall(requestBuilder.build()).execute()) {
      int code = response.code();
      ResponseBody body = response.body();
      if (code == 401) {
        // If there was an unauthorized error, then try refreshing the creds
        credentialFactory.refreshCredential(credential);
        monitor.info(() -> "Refreshed authorization token successfuly");

        requestBuilder.header("Authorization", "Bearer " + credential.getAccessToken());
        Response newResponse = client.newCall(requestBuilder.build()).execute();
        code = newResponse.code();
        body = newResponse.body();
      }

      if (code == 403 && response.message().contains("Access Denied")) {
        throw new PermissionDeniedException(
            "User access to microsoft onedrive was denied",
            new IOException(
                String.format("Got error code %d  with message: %s", code, response.message())));
      } else if (code < 200 || code > 299) {
        throw new IOException(
            "Got error code: "
                + code
                + " message: "
                + response.message()
                + " body: "
                + response.body().string());
      } else if (body == null) {
        throw new IOException("Got null body");
      }

      Map<String, Object> responseData = objectMapper.readValue(body.bytes(), Map.class);
      String folderId = (String) responseData.get("id");
      Preconditions.checkState(!Strings.isNullOrEmpty(folderId),
                 "Expected id value to be present in %s", responseData);
      return folderId;
    }
  }

  private String importSinglePhoto(
    PhotoModel photo,
    UUID jobId,
    IdempotentImportExecutor idempotentImportExecutor) throws Exception {
    BufferedInputStream inputStream = null;
    if (photo.isInTempStore()) {
      inputStream = new BufferedInputStream(jobStore.getStream(jobId, photo.getFetchableUrl()).getStream());
    } else if (photo.getFetchableUrl() != null) {
      inputStream = new BufferedInputStream(new URL(photo.getFetchableUrl()).openStream());
    } else {
      throw new IllegalStateException("Don't know how to get the inputStream for " + photo);
    }

    String photoUploadUrl = createUploadSession(photo, idempotentImportExecutor);

    // Arrange the data to be uploaded in chunks
    List<DataChunk> chunksToSend = DataChunk.splitData(inputStream);
    inputStream.close();
    final int totalFileSize = chunksToSend.stream().map(DataChunk::getSize).reduce(0, Integer::sum);
    Preconditions.checkState(
        chunksToSend.size() != 0, "Data was split into zero chunks %s.", photo.getTitle());

    Response chunkResponse = null;
    for (DataChunk chunk : chunksToSend) {
      chunkResponse = uploadChunk(chunk, photoUploadUrl, totalFileSize, photo.getMediaType());
    }
    if (chunkResponse.code() != 200 && chunkResponse.code() != 201) {
      // Once we upload the last chunk, we should have either 200 or 201.
      // This should change to a precondition check after we debug some more.
      monitor.debug(
          () -> "Received a bad code on completion of uploading chunks", chunkResponse.code());
    }
    // get complete file response
    ResponseBody chunkResponseBody = chunkResponse.body();
    Map<String, Object> chunkResponseData = objectMapper.readValue(chunkResponseBody.bytes(), Map.class);
    return (String) chunkResponseData.get("id");
  }

  private Credential getOrCreateCredential(TokensAndUrlAuthData authData) {
    if (this.credential == null) {
      this.credential = this.credentialFactory.createCredential(authData);
    }
    return this.credential;
  }

  // Request an upload session to the OneDrive api so that we can upload chunks
  // to the returned URL
  private String createUploadSession(PhotoModel photo, IdempotentImportExecutor idempotentImportExecutor) throws
          IOException, CopyExceptionWithFailureReason {

    // Forming the URL to create an upload session
    String createSessionUrl;
    if (Strings.isNullOrEmpty(photo.getAlbumId())) {
      createSessionUrl = String.format(albumlessPhotoUrlTemplate, photo.getTitle(), UPLOAD_PARAMS);

    } else {
      String oneDriveFolderId = idempotentImportExecutor.getCachedValue(photo.getAlbumId());
      createSessionUrl =
        String.format(
          uploadPhotoUrlTemplate, oneDriveFolderId, photo.getTitle(), UPLOAD_PARAMS);
    }

    // create upload session
    // POST to /me/drive/items/{folder_id}:/{file_name}:/createUploadSession OR /me/drive/items/root:/Photos/{file_name}:/createUploadSession
    // get {uploadurl} from response
    Request.Builder createSessionRequestBuilder = new Request.Builder().url(createSessionUrl);

    // Auth headers
    createSessionRequestBuilder.header("Authorization", "Bearer " + credential.getAccessToken());
    createSessionRequestBuilder.header("Content-Type", "application/json");

    // Post request with empty body. If you don't include an empty body, you'll have problems
    createSessionRequestBuilder.post(
      RequestBody.create(
        MediaType.parse("application/json"), objectMapper.writeValueAsString(ImmutableMap.of())));

    // Make the call, we should get an upload url for photo data in a 200 response
    Response response = client.newCall(createSessionRequestBuilder.build()).execute();
    int code = response.code();
    ResponseBody responseBody = response.body();

    // If there was an unauthorized error, then try refreshing the creds
    if (code == 401) {
      this.credentialFactory.refreshCredential(credential);
      monitor.info(() -> "Refreshed authorization token successfuly");

      createSessionRequestBuilder.header("Authorization", "Bearer " + credential.getAccessToken());
      Response newResponse = client.newCall(createSessionRequestBuilder.build()).execute();
      code = newResponse.code();
      responseBody = newResponse.body();
    }

    if (code == 403 && response.message().contains("Access Denied")) {
      throw new PermissionDeniedException(
          "User access to Microsoft One Drive was denied",
          new IOException(
              String.format("Got error code %d  with message: %s", code, response.message())));
    } else if (code == 507 && response.message().contains("Insufficient Storage")) {
      throw new DestinationMemoryFullException(
          "Microsoft destination storage limit reached",
          new IOException(
              String.format("Got error code %d  with message: %s", code, response.message())));
    } else if (code < 200 || code > 299) {
      throw new IOException(
          String.format(
              "Got error code: %s\n"
                  + "message: %s\n"
                  + "body: %s\n"
                  + "request url: %s\n"
                  + "bearer token: %s\n"
                  + " photo: %s\n", // For debugging 404s on upload
              code,
              response.message(),
              response.body().string(),
              createSessionUrl,
              credential.getAccessToken(),
              photo));
    } else if (code != 200) {
      monitor.info(() -> String.format("Got an unexpected non-200, non-error response code"));
    }
    // make sure we have a non-null response body
    Preconditions.checkState(
        responseBody != null, "Got Null Body when creating photo upload session %s", photo);
    // convert to a map
    final Map<String, Object> responseData = objectMapper.readValue(responseBody.bytes(), Map.class);
    // return the session's upload url
    Preconditions.checkState(responseData.containsKey("uploadUrl"), "No uploadUrl :(");
    return (String) responseData.get("uploadUrl");
  }

  // Uploads a single DataChunk to an upload URL
  // PUT to {photoUploadUrl}
  // HEADERS
  // Content-Length: {chunk size in bytes}
  // Content-Range: bytes {begin}-{end}/{total size}
  // body={bytes}
  private Response uploadChunk(DataChunk chunk, String photoUploadUrl, int totalFileSize, String mediaType)
          throws IOException, DestinationMemoryFullException {

    Request.Builder uploadRequestBuilder = new Request.Builder().url(photoUploadUrl);
    uploadRequestBuilder.header("Authorization", "Bearer " + credential.getAccessToken());

    // put chunk data in
    RequestBody uploadChunkBody = RequestBody.create(MediaType.parse(mediaType), chunk.getData(), 0, chunk.getSize());
    uploadRequestBuilder.put(uploadChunkBody);

    // set chunk data headers, indicating size and chunk range
    final String contentRange =
        String.format("bytes %d-%d/%d", chunk.getStart(), chunk.getEnd(), totalFileSize);
    uploadRequestBuilder.header("Content-Range", contentRange);
    uploadRequestBuilder.header("Content-Length", String.format("%d", chunk.getSize()));

    // upload the chunk
    Response chunkResponse = client.newCall(uploadRequestBuilder.build()).execute();
    Preconditions.checkNotNull(chunkResponse, "chunkResponse is null");
    if (chunkResponse.code() == 401) {
      // If there was an unauthorized error, then try refreshing the creds
      credentialFactory.refreshCredential(credential);
      monitor.info(() -> "Refreshed authorization token successfuly");

      // update auth info, reupload chunk
      uploadRequestBuilder.header("Authorization", "Bearer " + credential.getAccessToken());
      chunkResponse = client.newCall(uploadRequestBuilder.build()).execute();
    }
    int chunkCode = chunkResponse.code();
    if (chunkCode == 507 && chunkResponse.message().contains("Insufficient Storage")) {
      throw new DestinationMemoryFullException(
          "Microsoft destination storage limit reached",
          new IOException(
              String.format(
                  "Got error code %d  with message: %s", chunkCode, chunkResponse.message())));
    } else if (chunkCode < 200 || chunkCode > 299) {
      throw new IOException(
          "Got error code: "
              + chunkCode
              + " message: "
              + chunkResponse.message()
              + " body: "
              + chunkResponse.body().string());
    } else if (chunkCode == 200 || chunkCode == 201 || chunkCode == 202) {
      monitor.info(
          () ->
              String.format(
                  "Uploaded chunk %s-%s successfuly, code %d",
                  chunk.getStart(), chunk.getEnd(), chunkCode));
    }
    return chunkResponse;
  }
}
