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

package org.datatransferproject.datatransfer.google.videos;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.ArrayMap;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.InvalidArgumentException;
import com.google.api.gax.rpc.UnauthenticatedException;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.PhotosLibrarySettings;
import com.google.photos.library.v1.proto.BatchCreateMediaItemsResponse;
import com.google.photos.library.v1.proto.NewMediaItem;
import com.google.photos.library.v1.proto.NewMediaItemResult;
import com.google.photos.library.v1.upload.UploadMediaItemRequest;
import com.google.photos.library.v1.upload.UploadMediaItemResponse.Error;
import com.google.photos.library.v1.upload.UploadMediaItemResponse;
import com.google.photos.library.v1.util.NewMediaItemFactory;
import com.google.rpc.Code;
import com.google.rpc.Status;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.google.common.GooglePhotosImportUtils;
import org.datatransferproject.datatransfer.google.mediaModels.BatchMediaItemResponse;
import org.datatransferproject.datatransfer.google.mediaModels.MediaItemSearchResponse;
import org.datatransferproject.datatransfer.google.mediaModels.NewMediaItemUpload;
import org.datatransferproject.spi.cloud.connection.ConnectionProvider;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.idempotentexecutor.ItemImportResult;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.spi.transfer.types.UploadErrorException;
import org.datatransferproject.types.common.DownloadableItem;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class GoogleVideosInterface {

  private static final int ALBUM_PAGE_SIZE = 20; // TODO
  private static final int MEDIA_PAGE_SIZE = 50; // TODO

  private static final String PAGE_SIZE_KEY = "pageSize";
  private static final String TOKEN_KEY = "pageToken";
  private static final String ALBUM_ID_KEY = "albumId";
  private static final String ACCESS_TOKEN_KEY = "access_token";
  private static final String FILTERS_KEY = "filters";
  private static final String INCLUDE_ARCHIVED_KEY = "includeArchivedMedia";
  private static final String MEDIA_FILTER_KEY = "mediaTypeFilter";
  private static final String BASE_URL = "https://photoslibrary.googleapis.com/v1/";

  private final ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  private final HttpTransport httpTransport = new NetHttpTransport();
  private final Credential credential;
  private JsonFactory jsonFactory;

  GoogleVideosInterface(Credential credential, JsonFactory jsonFactory) {
    this.credential = credential;
    this.jsonFactory = jsonFactory;
  }

  // TODO(aksingh737) probably dead code; seems it's not called or if it is, then reconcile why this
  // exists *and* GoogleVideosInterface#uploadMediaItem internal logic exists, calling entirely
  // different APIs.
  String uploadVideoContent(InputStream inputStream, String filename) throws IOException {
    InputStreamContent content = new InputStreamContent(null, inputStream);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    content.writeTo(outputStream);
    byte[] contentBytes = outputStream.toByteArray();
    HttpContent httpContent = new ByteArrayContent(null, contentBytes);

    return makePostRequest(BASE_URL + "uploads/", Optional.empty(), httpContent, String.class);
  }

  // TODO(aksingh737) probably dead code; seems it's not called; see TODO atop uploadVideoContent
  // for related discrepencies.
  BatchMediaItemResponse createVideo(NewMediaItemUpload newMediaItemUpload) throws IOException {
    HashMap<String, Object> map = createJsonMap(newMediaItemUpload);
    HttpContent httpContent = new JsonHttpContent(this.jsonFactory, map);

    return makePostRequest(
        BASE_URL + "mediaItems:batchCreate",
        Optional.empty(),
        httpContent,
        BatchMediaItemResponse.class);
  }

  MediaItemSearchResponse listVideoItems(Optional<String> pageToken) throws IOException {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put(PAGE_SIZE_KEY, String.valueOf(MEDIA_PAGE_SIZE));

    params.put(
        FILTERS_KEY,
        ImmutableMap.of(
            MEDIA_FILTER_KEY, ImmutableMap.of("mediaTypes", ImmutableList.of("VIDEO"))));

    if (pageToken.isPresent()) {
      params.put(TOKEN_KEY, pageToken.get());
    }
    HttpContent content = new JsonHttpContent(this.jsonFactory, params);
    return makePostRequest(
        BASE_URL + "mediaItems:search", Optional.empty(), content, MediaItemSearchResponse.class);
  }

  <T> T makePostRequest(
      String url, Optional<Map<String, String>> parameters, HttpContent httpContent, Class<T> clazz)
      throws IOException {
    HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
    HttpRequest postRequest =
        requestFactory.buildPostRequest(
            new GenericUrl(url + "?" + generateParamsString(parameters)), httpContent);

    // TODO: Figure out why this is necessary for videos but not for photos
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType("application/octet-stream");
    headers.setAuthorization("Bearer " + this.credential.getAccessToken());
    headers.set("X-Goog-Upload-Protocol", "raw");
    postRequest.setHeaders(headers);

    HttpResponse response = postRequest.execute();
    int statusCode = response.getStatusCode();
    if (statusCode != 200) {
      throw new IOException(
          "Bad status code: " + statusCode + " error: " + response.getStatusMessage());
    }
    String result =
        CharStreams.toString(new InputStreamReader(response.getContent(), Charsets.UTF_8));
    if (clazz.isAssignableFrom(String.class)) {
      return (T) result;
    } else {
      return objectMapper.readValue(result, clazz);
    }
  }

  private String generateParamsString(Optional<Map<String, String>> params) throws IOException {
    Map<String, String> updatedParams = new ArrayMap<>();
    if (params.isPresent()) {
      updatedParams.putAll(params.get());
    }

    // getAccessToken will return null when the token needs to be refreshed
    if (credential.getAccessToken() == null) {
      credential.refreshToken();
    }

    updatedParams.put(ACCESS_TOKEN_KEY, Preconditions.checkNotNull(credential.getAccessToken()));

    List<String> orderedKeys = updatedParams.keySet().stream().collect(Collectors.toList());
    Collections.sort(orderedKeys);

    List<String> paramStrings = new ArrayList<>();
    for (String key : orderedKeys) {
      String k = key.trim();
      String v = updatedParams.get(key).trim();

      paramStrings.add(k + "=" + v);
}
    return String.join("&", paramStrings);
  }

  private HashMap<String, Object> createJsonMap(Object object) throws IOException {
    // JacksonFactory expects to receive a Map, not a JSON-annotated POJO, so we have to convert the
    // NewMediaItemUpload to a Map before making the HttpContent.
    TypeReference<HashMap<String, Object>> typeRef =
        new TypeReference<HashMap<String, Object>>() {};
    return objectMapper.readValue(objectMapper.writeValueAsString(object), typeRef);
  }

  public static PhotosLibraryClient buildPhotosLibraryClient(
      AppCredentials appCredentials,
      TokensAndUrlAuthData authData) throws IOException {
    PhotosLibrarySettings settings =
        PhotosLibrarySettings.newBuilder()
            .setCredentialsProvider(
                FixedCredentialsProvider.create(
                    UserCredentials.newBuilder()
                        .setClientId(appCredentials.getKey())
                        .setClientSecret(appCredentials.getSecret())
                        .setAccessToken(new AccessToken(authData.getAccessToken(), new Date()))
                        .setRefreshToken(authData.getRefreshToken())
                        .build()))
            .build();
    return PhotosLibraryClient.initialize(settings);
  }

//public static long uploadBatchOfVideos() throws Exception {
//}

  /**
   * Uploads `video` via {@link com.google.photos.library.v1.PhotosLibraryClient} APIs.
   *
   * Returns an upload token, and a byte count of the video that was uploaded.
   */
  public static Pair<String, Long> uploadVideo(
      UUID jobId,
      VideoModel video,
      PhotosLibraryClient photosLibraryClient,
      TemporaryPerJobDataStore dataStore,
      ConnectionProvider connectionProvider)
      throws IOException, UploadErrorException, InvalidTokenException {

    final File tmp = createTempFile(
        jobId,
        dataStore,
        connectionProvider,
        video);
    try {
      UploadMediaItemRequest uploadRequest =
          UploadMediaItemRequest.newBuilder()
              .setFileName(video.getName())
              .setDataFile(new RandomAccessFile(tmp, "r"))
              .build();
      UploadMediaItemResponse uploadResponse = photosLibraryClient.uploadMediaItem(uploadRequest);
      String uploadToken;
      if (uploadResponse.getError().isPresent() || !uploadResponse.getUploadToken().isPresent()) {
        Error error = uploadResponse.getError().orElse(null);

        if (error != null) {
          Throwable cause = error.getCause();
          String message = cause.getMessage();
          if (message.contains("The upload url is either finalized or rejected by the server")) {
            throw new UploadErrorException("Upload was terminated because of error", cause);
          } else if (message.contains("invalid_grant")) {
            throw new InvalidTokenException("Token has been expired or revoked", cause);
          }
        }

        throw new IOException(
            "An error was encountered while uploading the video.",
            error != null ? error.getCause() : null);
      } else {
        uploadToken = uploadResponse.getUploadToken().get();
      }
      return Pair.of(uploadToken, tmp.length());
    } catch (ApiException ex) {
      // temp check as exception is not captured and wrapped into UploadMediaItemResponse
      Throwable cause = ex.getCause();
      String message = cause.getMessage();
      if (message.contains("invalid_grant")) {
        throw new InvalidTokenException("Token has been expired or revoked", cause);
      }
      throw new IOException("An error was encountered while uploading the video.", cause);
    } finally {
      //noinspection ResultOfMethodCallIgnored
      tmp.delete();
    }
  }

  private static File createTempFile(
      UUID jobId,
      TemporaryPerJobDataStore dataStore,
      ConnectionProvider connectionProvider,
      DownloadableItem video) throws IOException {
    return createTempFile(
        jobId,
        dataStore,
        connectionProvider,
        video,
        // TODO(aksingh737) should mp4 be hardcoded here?
        "mp4" /*fileSuffix*/);
  }

  // TODO(aksingh737) factor this out into TemporaryPerJobDataStore which already has random/temp-file
  // related logic
  private static File createTempFile(
      UUID jobId,
      TemporaryPerJobDataStore dataStore,
      ConnectionProvider connectionProvider,
      DownloadableItem item,
      String fileSuffix) throws IOException {
    try (InputStream is = connectionProvider.getInputStreamForItem(jobId, item).getStream()) {
      return dataStore.getTempFileFromInputStream(is, item.getName(), fileSuffix);
    }
  }

  // TODO(aksingh737) WARNING: stop maintaining this code here; this needs to be reconciled against
  // a generic version so we don't have feature/bug development drift against our forks; see the
  // slowly-progressing effort to factor this code out with small interfaces, over in
  // GoogleMediaImporter.
  // DO NOT MERGE  - rename this code into GoogleVideosInterface.uploadBatchOfVideos
  @VisibleForTesting
  public static long importVideoBatch(
      UUID jobId,
      List<VideoModel> batchedVideos,
      TemporaryPerJobDataStore dataStore,
      PhotosLibraryClient client,
      IdempotentImportExecutor executor,
      ConnectionProvider connectionProvider,
      Monitor monitor) throws Exception {
    final ArrayListMultimap<String, NewMediaItem> mediaItemsByAlbum = ArrayListMultimap.create();
    final Map<String, VideoModel> uploadTokenToDataId = new HashMap<>();
    final Map<String, Long> uploadTokenToLength = new HashMap<>();

    // The PhotosLibraryClient can throw InvalidArgumentException and this try block wraps the two
    // calls of the client to handle the InvalidArgumentException when the user's storage is full.
    try {
      for (VideoModel video : batchedVideos) {
        try {
          Pair<String, Long> pair = uploadVideo(jobId, video, client, dataStore, connectionProvider);
          final String uploadToken = pair.getLeft();
          final String googleAlbumId =
              Strings.isNullOrEmpty(video.getAlbumId())
                  ? null
                  : executor.getCachedValue(video.getAlbumId());
          mediaItemsByAlbum.put(googleAlbumId, buildMediaItem(video, uploadToken));
          uploadTokenToDataId.put(uploadToken, video);
          uploadTokenToLength.put(uploadToken, pair.getRight());
          if (video.isInTempStore()) {
            dataStore.removeData(jobId, video.getFetchableUrl());
          }
        } catch (IOException e) {
          if (e instanceof FileNotFoundException) {
            // If the video file is no longer available then skip the video. We see this in a small
            // number of videos where the video has been deleted.
            monitor.info(
                () -> String.format("Video resource was missing for id: %s", video.getIdempotentId()), e);
            continue;
          }
          executor.importAndSwallowIOExceptions(
              video,
              videoModel -> ItemImportResult.error(e, null)
          );
        }
      }

      if (mediaItemsByAlbum.isEmpty()) {
        // Either we were not passed in any videos or we failed upload on all of them.
        return 0L;
      }

      final List<NewMediaItemResult> resultsList = mediaItemsByAlbum.keySet().stream()
          .map(k ->
              k == null
                  ? client.batchCreateMediaItems(mediaItemsByAlbum.get(null))
                  : client.batchCreateMediaItems(k, mediaItemsByAlbum.get(k)))
          .map(BatchCreateMediaItemsResponse::getNewMediaItemResultsList)
          .flatMap(Collection::stream)
          .collect(Collectors.toList());

      long bytes = 0L;
      for (NewMediaItemResult result : resultsList) {
        String uploadToken = result.getUploadToken();
        Status status = result.getStatus();

        final VideoModel video = uploadTokenToDataId.get(uploadToken);
        Preconditions.checkNotNull(video);
        final int code = status.getCode();
        Long length = uploadTokenToLength.get(uploadToken);
        if (code == Code.OK_VALUE) {
          executor.importAndSwallowIOExceptions(
              video, videoModel -> ItemImportResult.success(result.getMediaItem().getId(), length)
          );
          if (length != null) {
            bytes += length;
          }
        } else {
          executor.importAndSwallowIOExceptions(
              video,
              videoModel -> ItemImportResult.error(new IOException(
                  String.format(
                      "Video item could not be created. Code: %d Message: %s",
                      code, result.getStatus().getMessage())), length)
          );
        }
        uploadTokenToDataId.remove(uploadToken);
      }
      if (!uploadTokenToDataId.isEmpty()) {
        for (Entry<String, VideoModel> entry : uploadTokenToDataId.entrySet()) {
          VideoModel video = entry.getValue();
          String uploadToken = entry.getKey();
          executor.importAndSwallowIOExceptions(
              video,
              videoModel -> ItemImportResult.error(
                  new IOException("Video item was missing from results list."),
                  uploadTokenToLength.get(uploadToken))
          );
        }
      }
      return bytes;
    } catch (InvalidArgumentException e) {
      if (e.getMessage().contains("The remaining storage in the user's account is not enough")) {
        throw new DestinationMemoryFullException("Google destination storage full", e);
      } else {
        throw e;
      }
    } catch (UnauthenticatedException e) {
      throw new InvalidTokenException("Token has been expired or revoked", e);
    }
  }

  @VisibleForTesting
  public static NewMediaItem buildMediaItem(VideoModel inputVideo, String uploadToken) {
    NewMediaItem newMediaItem;
    String videoDescription = inputVideo.getDescription();
    if (Strings.isNullOrEmpty(videoDescription)) {
      newMediaItem = NewMediaItemFactory.createNewMediaItem(uploadToken);
    } else {
      videoDescription = GooglePhotosImportUtils.cleanDescription(videoDescription);
      newMediaItem = NewMediaItemFactory.createNewMediaItem(uploadToken, videoDescription);
    }
    return newMediaItem;
  }
}
