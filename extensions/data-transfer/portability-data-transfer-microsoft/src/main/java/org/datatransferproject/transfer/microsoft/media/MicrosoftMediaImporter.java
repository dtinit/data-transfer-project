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
package org.datatransferproject.transfer.microsoft.media;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
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
import org.datatransferproject.types.common.DownloadableFile;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Imports albums with their photos and videos to OneDrive using the Microsoft Graph API.
 */
public class MicrosoftMediaImporter
    implements Importer<TokensAndUrlAuthData, MediaContainerResource> {
  private final OkHttpClient client;
  private final ObjectMapper objectMapper;
  private final TemporaryPerJobDataStore jobStore;
  private final Monitor monitor;
  private final MicrosoftCredentialFactory credentialFactory;
  private final MicrosoftTransmogrificationConfig transmogrificationConfig =
      new MicrosoftTransmogrificationConfig();
  private Credential credential;

  private final String createFolderUrl;
  private final String uploadMediaUrlTemplate;
  private final String albumlessMediaUrlTemplate;

  private static final String UPLOAD_PARAMS = "?@microsoft.graph.conflictBehavior=rename";

  public MicrosoftMediaImporter(String baseUrl, OkHttpClient client, ObjectMapper objectMapper,
      TemporaryPerJobDataStore jobStore, Monitor monitor,
      MicrosoftCredentialFactory credentialFactory) {

    createFolderUrl = baseUrl + "/v1.0/me/drive/special/photo-video/children";
    // first param is the folder id, second param is the file name
    // /me/drive/items/{parent-id}:/{filename}:/content;
    uploadMediaUrlTemplate = baseUrl + "/v1.0/me/drive/items/%s:/%s:/createUploadSession%s";
    albumlessMediaUrlTemplate =
        baseUrl + "/v1.0/me/drive/special/photo-video:/%s:/createUploadSession%s";

    this.client = client;
    this.objectMapper = objectMapper;
    this.jobStore = jobStore;
    this.monitor = monitor;
    this.credentialFactory = credentialFactory;
    this.credential = null;
  }

  @Override
  public ImportResult importItem(UUID jobId, IdempotentImportExecutor idempotentImportExecutor,
      TokensAndUrlAuthData authData, MediaContainerResource resource) throws Exception {
    // Ensure credential is populated
    getOrCreateCredential(authData);

    logDebugJobStatus("%s before transmogrification", jobId, resource);

    // Make the data onedrive compatible
    resource.transmogrify(transmogrificationConfig);

    logDebugJobStatus("%s after transmogrification", jobId, resource);

    for (MediaAlbum album : resource.getAlbums()) {
      // Create a OneDrive folder and then save the id with the mapping data
      idempotentImportExecutor.executeAndSwallowIOExceptions(
          album.getId(), album.getName(), () -> createOneDriveFolder(album));
    }

    executeIdempotentImport(jobId, idempotentImportExecutor, resource.getVideos());

    executeIdempotentImport(jobId, idempotentImportExecutor, resource.getPhotos());

    return ImportResult.OK;
  }

  /**
   * Logs brief debugging message describing current state of job given `resource`.
   *
   * @param format a printf-style formatter that exactly one parameter: the string of the job's
   *   current status.
   */
  private void logDebugJobStatus(
      String format,
      UUID jobId,
      MediaContainerResource resource) {
    String statusMessage = String.format(
        "%s: Importing %s albums, %s photos, and %s videos",
        jobId,
        resource.getAlbums().size(),
        resource.getPhotos().size(),
        resource.getVideos().size());
    monitor.debug(() ->  String.format(format, statusMessage));
  }

  @SuppressWarnings("unchecked")
  private String createOneDriveFolder(MediaAlbum album)
      throws IOException, CopyExceptionWithFailureReason {
    Map<String, Object> rawFolder = new LinkedHashMap<>();
    // clean up album name for microsoft specifically
    // Note that MediaAlbum.getName() can return an empty string or null depending
    // on the results of MediaAlbum.cleanName(), e.g. if a Google Photos album has
    // title=" ", its cleaned name will be "". See MediaAlbum.cleanName for further
    // details on what forms the name can take.
    String albumName = Strings.isNullOrEmpty(album.getName()) ? "Untitled" : album.getName();
    rawFolder.put("name", albumName);
    rawFolder.put("folder", new LinkedHashMap());
    rawFolder.put("@microsoft.graph.conflictBehavior", "rename");

    Request.Builder requestBuilder = new Request.Builder().url(createFolderUrl);
    requestBuilder.header("Authorization", "Bearer " + credential.getAccessToken());
    requestBuilder.post(RequestBody.create(
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
        throw new PermissionDeniedException("User access to microsoft onedrive was denied",
            new IOException(
                String.format("Got error code %d  with message: %s", code, response.message())));
      } else if (code < 200 || code > 299) {
        throw new IOException("Got error code: " + code + " message: " + response.message()
            + " body: " + response.body().string());
      } else if (body == null) {
        throw new IOException("Got null body");
      }

      Map<String, Object> responseData = objectMapper.readValue(body.bytes(), Map.class);
      String folderId = (String) responseData.get("id");
      Preconditions.checkState(
          !Strings.isNullOrEmpty(folderId), "Expected id value to be present in %s", responseData);
      return folderId;
    }
  }

  private void executeIdempotentImport(
      UUID jobId,
      IdempotentImportExecutor idempotentImportExecutor,
      Collection<? extends DownloadableFile> downloadableFiles) throws Exception {
    for (DownloadableFile downloadableFile : downloadableFiles) {
      idempotentImportExecutor.executeAndSwallowIOExceptions(
          downloadableFile.getIdempotentId(), downloadableFile.getName(),
          () -> importDownloadableItem(downloadableFile, jobId, idempotentImportExecutor));
    }
  }

  private String importDownloadableItem(
      DownloadableFile item, UUID jobId,
      IdempotentImportExecutor idempotentImportExecutor) throws Exception {
    BufferedInputStream inputStream = null;
    if (item.isInTempStore()) {
      inputStream =
          new BufferedInputStream(jobStore.getStream(jobId, item.getFetchableUrl()).getStream());
    } else if (item.getFetchableUrl() != null) {
      inputStream = new BufferedInputStream(new URL(item.getFetchableUrl()).openStream());
    } else {
      throw new IllegalStateException("Don't know how to get the inputStream for " + item);
    }

    String itemUploadUrl = createUploadSession(item, idempotentImportExecutor);

    // Arrange the data to be uploaded in chunks
    List<DataChunk> chunksToSend = DataChunk.splitData(inputStream);
    inputStream.close();
    final int totalFileSize = chunksToSend.stream().map(DataChunk::getSize).reduce(0, Integer::sum);
    Preconditions.checkState(
        chunksToSend.size() != 0, "Data was split into zero chunks %s.", item.getName());

    Response chunkResponse = null;
    for (DataChunk chunk : chunksToSend) {
      chunkResponse = uploadChunk(chunk, itemUploadUrl, totalFileSize, item.getMimeType());
    }
    if (chunkResponse.code() != 200 && chunkResponse.code() != 201) {
      // Once we upload the last chunk, we should have either 200 or 201.
      // This should change to a precondition check after we debug some more.
      monitor.debug(
          () -> "Received a bad code on completion of uploading chunks", chunkResponse.code());
    }
    // get complete file response
    ResponseBody chunkResponseBody = chunkResponse.body();
    Map<String, Object> chunkResponseData =
        objectMapper.readValue(chunkResponseBody.bytes(), Map.class);
    return (String) chunkResponseData.get("id");
  }

  private Credential getOrCreateCredential(TokensAndUrlAuthData authData) {
    if (this.credential == null) {
      this.credential = this.credentialFactory.createCredential(authData);
    }
    return this.credential;
  }

  private Pair<Request, Response> tryWithCreds(Request.Builder requestBuilder) throws IOException {
    Request request = requestBuilder.build();
    Response response = client.newCall(request).execute();

    // If there was an unauthorized error, then try refreshing the creds
    if (response.code() != 401) {
      return Pair.of(request, response);
    }

    this.credentialFactory.refreshCredential(credential);
    monitor.info(() -> "Refreshed authorization token successfuly");

    requestBuilder.header("Authorization", "Bearer " + credential.getAccessToken());
    request = requestBuilder.build();
    return Pair.of(request, client.newCall(request).execute());
  }

  // Request an upload session to the OneDrive api so that we can upload chunks
  // to the returned URL
  private String createUploadSession(
      DownloadableFile item, IdempotentImportExecutor idempotentImportExecutor)
      throws IOException, CopyExceptionWithFailureReason {
    Request.Builder createSessionRequestBuilder = buildCreateUploadSessionPath(item, idempotentImportExecutor);

    // Auth headers
    createSessionRequestBuilder.header("Authorization", "Bearer " + credential.getAccessToken());
    createSessionRequestBuilder.header("Content-Type", "application/json");

    // Post request with empty body. If you don't include an empty body, you'll have problems
    createSessionRequestBuilder.post(RequestBody.create(
        MediaType.parse("application/json"), objectMapper.writeValueAsString(ImmutableMap.of())));

    // Make the call, we should get an upload url for item data in a 200 response
    Pair<Request, Response> reqResp = tryWithCreds(createSessionRequestBuilder);
    Response response = reqResp.getRight();
    int code = response.code();
    if (code == 403 && response.message().contains("Access Denied")) {
      throw new PermissionDeniedException("User access to Microsoft One Drive was denied",
          new IOException(
              String.format("Got error code %d  with message: %s", code, response.message())));
    } else if (code == 507 && response.message().contains("Insufficient Storage")) {
      throw new DestinationMemoryFullException("Microsoft destination storage limit reached",
          new IOException(
              String.format("Got error code %d  with message: %s", code, response.message())));
    } else if (code < 200 || code > 299) {
      throw new IOException(String.format("Got error code: %s\n"
              + "message: %s\n"
              + "body: %s\n"
              + "request url: %s\n"
              + "bearer token: %s\n"
              + " item: %s\n", // For debugging 404s on upload
          code, response.message(), response.body().string(), reqResp.getLeft().url(),
          credential.getAccessToken(), item));
    } else if (code != 200) {
      monitor.info(() -> String.format("Got an unexpected non-200, non-error response code"));
    }
    ResponseBody responseBody = response.body();
    // make sure we have a non-null response body
    Preconditions.checkState(
        responseBody != null, "Got Null Body when creating item upload session %s", item);
    // convert to a map
    final Map<String, Object> responseData =
        objectMapper.readValue(responseBody.bytes(), Map.class);
    // return the session's upload url
    Preconditions.checkState(responseData.containsKey("uploadUrl"), "No uploadUrl :(");
    return (String) responseData.get("uploadUrl");
  }

  /**
   * Forms the URL to create an upload session.
   *
   * Creates an upload session path for one of two cases:
   * - 1) POST to /me/drive/items/{folder_id}:/{file_name}:/createUploadSession
   * - 2) GET {uploadurl} from /me/drive/items/root:/photos-video/{file_name}:/createUploadSession
   */
  private Request.Builder buildCreateUploadSessionPath(
      DownloadableFile item,
      IdempotentImportExecutor idempotentImportExecutor) {
    String createSessionUrl;
    if (Strings.isNullOrEmpty(item.getFolderId())) {
      createSessionUrl = String.format(albumlessMediaUrlTemplate, item.getName(), UPLOAD_PARAMS);
    } else {
      String oneDriveFolderId = idempotentImportExecutor.getCachedValue(item.getFolderId());
      createSessionUrl =
          String.format(uploadMediaUrlTemplate, oneDriveFolderId, item.getName(), UPLOAD_PARAMS);
    }

    return new Request.Builder().url(createSessionUrl);
  }

  // Uploads a single DataChunk to an upload URL
  // PUT to {photoUploadUrl}
  // HEADERS
  // Content-Length: {chunk size in bytes}
  // Content-Range: bytes {begin}-{end}/{total size}
  // body={bytes}
  private Response uploadChunk(DataChunk chunk, String photoUploadUrl, int totalFileSize,
      String mediaType) throws IOException, DestinationMemoryFullException {
    Request.Builder uploadRequestBuilder = new Request.Builder().url(photoUploadUrl);
    uploadRequestBuilder.header("Authorization", "Bearer " + credential.getAccessToken());

    // put chunk data in
    RequestBody uploadChunkBody =
        RequestBody.create(MediaType.parse(mediaType), chunk.getData(), 0, chunk.getSize());
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
      throw new DestinationMemoryFullException("Microsoft destination storage limit reached",
          new IOException(String.format(
              "Got error code %d  with message: %s", chunkCode, chunkResponse.message())));
    } else if (chunkCode < 200 || chunkCode > 299) {
      throw new IOException("Got error code: " + chunkCode + " message: " + chunkResponse.message()
          + " body: " + chunkResponse.body().string());
    } else if (chunkCode == 200 || chunkCode == 201 || chunkCode == 202) {
      monitor.info(()
                       -> String.format("Uploaded chunk %s-%s successfuly, code %d",
                           chunk.getStart(), chunk.getEnd(), chunkCode));
    }
    return chunkResponse;
  }
}
