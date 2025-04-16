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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.datatransferproject.spi.api.transport.DiscardingStreamCounter.discardForLength;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.lang3.tuple.Pair;
import com.google.common.util.concurrent.RateLimiter;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.api.transport.JobFileStream;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.datatransferproject.transfer.microsoft.DataChunk;
import org.datatransferproject.transfer.microsoft.MicrosoftApiResponse;
import org.datatransferproject.transfer.microsoft.MicrosoftTransmogrificationConfig;
import org.datatransferproject.transfer.microsoft.StreamChunker;
import org.datatransferproject.transfer.microsoft.common.MicrosoftCredentialFactory;
import org.datatransferproject.types.common.DownloadableFile;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

/** Imports albums with their photos and videos to OneDrive using the Microsoft Graph API. */
public class MicrosoftMediaImporter
    implements Importer<TokensAndUrlAuthData, MediaContainerResource> {
  /** Max number of bytes to upload to Microsoft's APIs at a time. */
  private static final int MICROSOFT_UPLOAD_CHUNK_BYTE_SIZE = 32000 * 1024; // 32000KiB

  private final OkHttpClient.Builder httpClientBuilder;
  private OkHttpClient client;

  private final ObjectMapper objectMapper;
  private final TemporaryPerJobDataStore jobStore;
  private final Monitor monitor;
  private final MicrosoftCredentialFactory credentialFactory;
  private final JobFileStream jobFileStream;
  private final RateLimiter writeRateLimiter;

  private final MicrosoftTransmogrificationConfig transmogrificationConfig =
      new MicrosoftTransmogrificationConfig();
  private Credential credential;

  private final String createFolderUrl;
  private final String uploadMediaUrlTemplate;
  private final String albumlessMediaUrlTemplate;

  private static final String UPLOAD_PARAMS = "?@microsoft.graph.conflictBehavior=rename";

  public MicrosoftMediaImporter(
      String baseUrl,
      OkHttpClient.Builder httpClientBuilder,
      ObjectMapper objectMapper,
      TemporaryPerJobDataStore jobStore,
      Monitor monitor,
      MicrosoftCredentialFactory credentialFactory,
      JobFileStream jobFileStream,
      double maxWritesPerSecond) {
    // NOTE: "special/photos" is a specific folder in One Drive that corresponds to items that
    // should appear in https://photos.onedrive.com/, for more information see:
    // https://learn.microsoft.com/en-us/onedrive/developer/rest-api/api/drive_get_specialfolder?#special-folder-names
    createFolderUrl = baseUrl + "/v1.0/me/drive/special/photos/children";
    albumlessMediaUrlTemplate =
        baseUrl + "/v1.0/me/drive/special/photos:/%s:/createUploadSession%s";

    // first param is the folder id, second param is the file name
    // /me/drive/items/{parent-id}:/{filename}:/content;
    uploadMediaUrlTemplate = baseUrl + "/v1.0/me/drive/items/%s:/%s:/createUploadSession%s";

    this.httpClientBuilder = httpClientBuilder;
    this.client = httpClientBuilder.build();
    this.objectMapper = objectMapper;
    this.jobStore = jobStore;
    this.monitor = monitor;
    this.credentialFactory = credentialFactory;
    this.credential = null;
    this.jobFileStream = jobFileStream;
    this.writeRateLimiter = RateLimiter.create(maxWritesPerSecond);
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentImportExecutor,
      TokensAndUrlAuthData authData,
      MediaContainerResource resource)
      throws Exception {
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
   *     current status.
   */
  private void logDebugJobStatus(String format, UUID jobId, MediaContainerResource resource) {
    String statusMessage =
        String.format(
            "%s: Importing %s albums, %s photos, and %s videos",
            jobId,
            resource.getAlbums().size(),
            resource.getPhotos().size(),
            resource.getVideos().size());
    monitor.debug(() -> String.format(format, statusMessage));
  }

  /** Returns a folder ID after asking Microsoft APIs to allocate one for the given album. */
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
    requestBuilder.post(
        RequestBody.create(
            MediaType.parse("application/json"), objectMapper.writeValueAsString(rawFolder)));
    return tryWithCredsOrFail(requestBuilder, "id" /*jsonResponseKey*/, "creating empty folder");
  }

  private void executeIdempotentImport(
      UUID jobId,
      IdempotentImportExecutor idempotentImportExecutor,
      Collection<? extends DownloadableFile> downloadableFiles)
      throws Exception {
    for (DownloadableFile downloadableFile : downloadableFiles) {
      idempotentImportExecutor.executeAndSwallowIOExceptions(
          downloadableFile.getIdempotentId(),
          downloadableFile.getName(),
          () -> importDownloadableItem(downloadableFile, jobId, idempotentImportExecutor));
    }
  }

  private String importDownloadableItem(
      DownloadableFile item, UUID jobId, IdempotentImportExecutor idempotentImportExecutor)
      throws Exception {
    final long totalFileSize = discardForLength(jobFileStream.streamFile(item, jobId, jobStore));
    if (totalFileSize <= 0) {
      throw new IOException(
          String.format(
              "jobid %s hit empty unexpectedly empty (bytes=%d) download for file %s",
              jobId, totalFileSize, item.getFetchableUrl()));
    }
    try (InputStream fileStream = jobFileStream.streamFile(item, jobId, jobStore)) {
      String itemUploadUrl = createUploadSession(item, idempotentImportExecutor);

      MicrosoftApiResponse finalChunkResponse =
          uploadStreamInChunks(totalFileSize, itemUploadUrl, item.getMimeType(), fileStream);
      checkState(
          finalChunkResponse.isOkay(),
          "final chunk-upload response should have had an ID, but a non-OK response came back: %s",
          finalChunkResponse.toString());

      // get complete file response
      return finalChunkResponse.getJsonValue(
          objectMapper,
          "id",
          "final chunk-upload response should have had ID, but got empty HTTP response-body");
    }
  }

  /**
   * Depletes input stream, uploading a chunk of the stream at a time, throwing a DTP exception
   * along the way if any unrecoverable errors are encountered.
   */
  private MicrosoftApiResponse uploadStreamInChunks(
      long totalFileSize, String itemUploadUrl, String itemMimeType, InputStream inputStream)
      throws IOException, DestinationMemoryFullException, PermissionDeniedException {
    MicrosoftApiResponse lastChunkResponse = null;
    StreamChunker streamChunker = new StreamChunker(MICROSOFT_UPLOAD_CHUNK_BYTE_SIZE, inputStream);
    Optional<DataChunk> currentChunk;
    while (true) {
      currentChunk = streamChunker.nextChunk();
      if (currentChunk.isEmpty()) {
        break;
      }
      lastChunkResponse =
          uploadChunk(currentChunk.get(), itemUploadUrl, totalFileSize, itemMimeType);

      // Log our progress before continuing to the next chunk.
      final DataChunk lastChunksent = currentChunk.get();
      final int httpStatus = lastChunkResponse.httpStatus();
      monitor.info(
          () ->
              String.format(
                  "Uploaded chunk range %d-%d (of total bytesize: %d) successfuly, HTTP status %d",
                  lastChunksent.streamByteOffset(),
                  lastChunksent.finalByteOffset(),
                  totalFileSize,
                  httpStatus));
    }
    return checkNotNull(
        lastChunkResponse, "bug: empty-stream already checked for yet stream empty now?");
  }

  // Request an upload session to the OneDrive api so that we can upload chunks
  // to the returned URL
  private String createUploadSession(
      DownloadableFile item, IdempotentImportExecutor idempotentImportExecutor)
      throws IOException, CopyExceptionWithFailureReason {
    Request.Builder createSessionRequestBuilder =
        buildCreateUploadSessionPath(item, idempotentImportExecutor);

    // Auth headers
    createSessionRequestBuilder.header("Authorization", "Bearer " + credential.getAccessToken());
    createSessionRequestBuilder.header("Content-Type", "application/json");

    // Post request with empty body. If you don't include an empty body, you'll have problems
    createSessionRequestBuilder.post(
        RequestBody.create(
            MediaType.parse("application/json"),
            objectMapper.writeValueAsString(ImmutableMap.of())));

    return tryWithCredsOrFail(
        createSessionRequestBuilder,
        "uploadUrl" /*jsonResponseKey*/,
        "creating initial upload session");
  }

  /**
   * Forms the URL to create an upload session.
   *
   * <p>Creates an upload session path for one of two cases:
   *
   * <ul>
   *   <li>- 1) POST to /me/drive/items/{folder_id}:/{file_name}:/createUploadSession
   *   <li>- 2) GET {uploadurl} from
   *       /me/drive/items/root:/photos-video/{file_name}:/createUploadSession
   * </ul>
   */
  private Request.Builder buildCreateUploadSessionPath(
      DownloadableFile item, IdempotentImportExecutor idempotentImportExecutor) {
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

  /**
   * Uploads a single DataChunk to an upload URL {@code photoUploadUrl} via PUT
   * request with this composition:
   * <pre>{@code
   *  HEADERS
   *  Content-Length: $CHUNK_SIZE_IN_BYTES
   *  Content-Range: bytes $BEGIN_INDEX-$END_INDEX/$TOTAL_SIZE
   *  body=$BYTES
   * }</pre>
   *
   * <p>NOTE: an access token is purposely not incluced per Microsoft SDK's own instructions:
   * https://learn.microsoft.com/en-us/graph/api/driveitem-createuploadsession?view=graph-rest-1.0#remarks
   *
   *
   * <p>See also:
   * <ul>
   * <li>https://learn.microsoft.com/en-us/graph/api/driveitem-createuploadsession?view=graph-rest-1.0#upload-bytes-to-the-upload-session
   * </ul>
   *
   * Returns a response, which is only expected to have a response-body in the
   * event that this is the final chunk.
   */
  private MicrosoftApiResponse uploadChunk(
      DataChunk chunk, String photoUploadUrl, long totalFileSize, String mediaType)
      throws IOException, DestinationMemoryFullException, PermissionDeniedException {
    Request.Builder uploadRequestBuilder = new Request.Builder().url(photoUploadUrl);

    // put chunk data in
    RequestBody uploadChunkBody =
        RequestBody.create(MediaType.parse(mediaType), chunk.chunk(), 0, chunk.size());
    uploadRequestBuilder.put(uploadChunkBody);

    // set chunk data headers, indicating size and chunk range
    final String contentRange =
        String.format(
            "bytes %d-%d/%d", chunk.streamByteOffset(), chunk.finalByteOffset(), totalFileSize);
    uploadRequestBuilder.header("Content-Range", contentRange);
    uploadRequestBuilder.header("Content-Length", String.format("%d", chunk.size()));

    return tryWithCredsOrFail(
        uploadRequestBuilder,
        String.format(
            "uploading one chunk (%s) mediaType=%s amid %d total bytes",
            contentRange, mediaType, totalFileSize));
  }

  private Credential getOrCreateCredential(TokensAndUrlAuthData authData) {
    if (this.credential == null) {
      this.credential = this.credentialFactory.createCredential(authData);
    }
    return this.credential;
  }

  /** Low-level API call used by other helpers: prefer {@link tryWithCreds} instead. */
  private MicrosoftApiResponse sendMicrosoftRequest(Request.Builder requestBuilder)
      throws IOException {
    // Wait for write permit before making request
    writeRateLimiter.acquire();

    return MicrosoftApiResponse.ofResponse(
        checkNotNull(
            client.newCall(requestBuilder.build()).execute(),
            "null microsoft server response for %s",
            requestBuilder.build().url()));
  }

  /**
   * Try request twice: once and if there's an unauthorized error, then just once more after
   * refreshing creds.
   *
   * <p>Prefer {@link tryWithCredsOrFail} to avoid repetitive error-handling edge-cases (and thus
   * possibly introducing new bugs).
   */
  private Pair<Request, MicrosoftApiResponse> tryWithCreds(Request.Builder requestBuilder)
      throws IOException {
    MicrosoftApiResponse response = sendMicrosoftRequest(requestBuilder);
    if (response.isTokenRefreshRequired()) {
      credentialFactory.refreshCredential(credential);
      client = httpClientBuilder.build(); // reset any old pool of maybe-keepalive connections

      monitor.info(() -> "Refreshed Microsoft authorization token successfuly");
      requestBuilder.header("Authorization", "Bearer " + credential.getAccessToken());
      response = sendMicrosoftRequest(requestBuilder);
    }
    return Pair.of(requestBuilder.build(), response);
  }

  /**
   * Try a request to Microsoft servers or fail with a critical DTP exception, after considering
   * standard token-refresh retry options.
   *
   * <p>Prefer {@link tryWithCredsOrFail(Request.Builder, String, String)} if you ultimately only
   * care about a particular JSON key in the response body. If you don't
   * need/expect a response body (or don't know yet) then this is the right
   * method to call instead (and then call {@link
   * MicrosoftApiResponse#getJsonValue} yourself later).
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * MicrosoftApiResponse resp = tryWithCredsOrFail(request, "creating a folder");
   * checkState(resp.isOkay(), "bug: tryWithCredsOrFail() should have returne healthy resp");
   * // ...carry on as normal with business logic...
   * }</pre>
   *
   * @param causeMessage a contextual message to include as the root cause/context when throwing a
   *     DTP excption.
   * @return server response.
   */
  private MicrosoftApiResponse tryWithCredsOrFail(Request.Builder req, String causeMessage)
      throws IOException, DestinationMemoryFullException, PermissionDeniedException {
    Pair<Request, MicrosoftApiResponse> reqResp = tryWithCreds(req);
    MicrosoftApiResponse response = reqResp.getRight();
    Optional<MicrosoftApiResponse.RecoverableState> recovery = response.recoverableState();
    if (recovery.isPresent()) {
      switch (recovery.get()) {
        case RECOVERABLE_STATE_OKAY:
          return response;
        case RECOVERABLE_STATE_NEEDS_TOKEN_REFRESH:
          throw response.toIoException(
              String.format(
                  "bug! microsoft server needs token refresh immediately after a refreshing: %s",
                  causeMessage));
      }
      throw new AssertionError("exhaustive switch");
    }
    return response.returnConvertDtpException(
        String.format(
            "%s: for request url \"%s\" and bearer token \"%s\"\n",
            causeMessage, reqResp.getLeft().url(), credential.getAccessToken()));
  }

  /**
   * Try a request to Microsoft servers or fail with a critical DTP exception, after considering
   * standard token-refresh retry options.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * @Nonnull String folderId = tryWithCredsOrFail(request, "folderId", "creating a folder");
   * // ...carry on as normal with business logic...
   * }</pre>
   *
   * @param jsonResponseKey the top-level value to extract from the response body.
   * @param causeMessage a contextual message to include as the root cause/context when throwing a
   *     DTP excption.
   * @return expected JSON value that api servers returned.
   */
  @Nonnull
  private String tryWithCredsOrFail(
      Request.Builder requestBuilder, String jsonResponseKey, String causeMessage)
      throws IOException, DestinationMemoryFullException, PermissionDeniedException {
    final MicrosoftApiResponse resp = tryWithCredsOrFail(requestBuilder, causeMessage);
    return resp.getJsonValue(objectMapper, jsonResponseKey, causeMessage);
  }
}
