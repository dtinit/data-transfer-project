/*
 * Copyright 2023 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.apple.photos;

import static com.google.common.truth.Truth.assertThat;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.apple.AppleInterfaceFactory;
import org.datatransferproject.datatransfer.apple.photos.photosproto.PhotosProtocol;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.idempotentexecutor.InMemoryIdempotentImportExecutor;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.types.common.models.FavoriteInfo;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.transfer.errors.ErrorDetail;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.commons.util.ReflectionUtils.HierarchyTraversalMode;
import org.mockito.stubbing.Answer;

public class AppleImporterTestBase {

  static final String EXPORTING_SERVICE = "ExportingService";
  protected AppleMediaInterface mediaInterface;
  protected TokensAndUrlAuthData authData;
  protected AppCredentials appCredentials;
  protected Monitor monitor;
  protected AppleInterfaceFactory factory;

  protected static final String ALBUM_NAME_BASE = "albumName";
  protected static final String ALBUM_DESCRIPTION_BASE = "albumDescription";
  protected static final String ALBUM_DATAID_BASE = "albumDataId";
  protected static final String ALBUM_RECORDID_BASE = "albumRecordId";
  protected static final String MEDIA_NAME_BASE = "mediaName";
  protected static final String PHOTOS_DATAID_BASE = "photosDataId";
  protected static final String VIDEOS_DATAID_BASE = "videosDataId";
  protected static final String MEDIA_RECORDID_BASE = "mediaRecordId";
  protected static final Long PHOTOS_FILE_SIZE = 100L;
  protected static final Long VIDEOS_FILE_SIZE = 1000L;
  protected IdempotentImportExecutor executor;
  protected UUID uuid = UUID.randomUUID();

  public void setup() throws Exception {
    monitor = mock(Monitor.class);
    authData = mock(TokensAndUrlAuthData.class);
    appCredentials = mock(AppCredentials.class);
    executor = new InMemoryIdempotentImportExecutor(monitor);
    mediaInterface = setupMediaInterface();
    factory = mock(AppleInterfaceFactory.class);
    when(factory.getOrCreateMediaInterface(any(), any(), any(), anyString(), any()))
      .thenReturn(mediaInterface);
  }

  private AppleMediaInterface setupMediaInterface() throws Exception {
    AppleMediaInterface mediaInterface = mock(AppleMediaInterface.class);
    Map<String, Object> fieldsToInject = new HashMap<>();
    fieldsToInject.put("baseUrl", "https://dummy-apis.photos.apple.com");
    fieldsToInject.put("appCredentials", new AppCredentials("key", "secret"));
    fieldsToInject.put("exportingService", EXPORTING_SERVICE);
    fieldsToInject.put("monitor", monitor);

    fieldsToInject.entrySet()
      .forEach(entry -> {
        try {
          ReflectionUtils.findFields(
              AppleMediaInterface.class,
              f -> f.getName().equals(entry.getKey()),
              HierarchyTraversalMode.TOP_DOWN)
            .stream()
            .findFirst()
            .get()
            .set(mediaInterface, entry.getValue());
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      });

    when(mediaInterface.importAlbums(any(), any(), any(), any())).thenCallRealMethod();
    when(mediaInterface.importAllMedia(any(), any(), any(), any())).thenCallRealMethod();
    when(mediaInterface.importMediaBatch(any(), any(), any(), any())).thenCallRealMethod();
    return mediaInterface;
  }

  protected void setUpGetUploadUrlResponse(@NotNull final Map<String, Integer> datatIdToStatus)
      throws IOException, CopyExceptionWithFailureReason {
    when(mediaInterface.getUploadUrl(any(String.class), any(String.class), any(List.class)))
        .thenAnswer(
            (Answer<PhotosProtocol.GetUploadUrlsResponse>)
                invocation -> {
                  Object[] args = invocation.getArguments();
                  final List<String> dataIds = (List<String>) args[2];
                  final List<PhotosProtocol.AuthorizeUploadResponse> authorizeUploadResponseList =
                      dataIds.stream()
                          .map(
                              dataId ->
                                  PhotosProtocol.AuthorizeUploadResponse.newBuilder()
                                      .setDataId(dataId)
                                      .setUploadUrl("uploadURL")
                                      .setStatus(
                                          PhotosProtocol.Status.newBuilder()
                                              .setCode(datatIdToStatus.get(dataId))
                                              .build())
                                      .build())
                          .collect(Collectors.toList());
                  return PhotosProtocol.GetUploadUrlsResponse.newBuilder()
                      .addAllUrlResponses(authorizeUploadResponseList)
                      .build();
                });
  }

  protected void setUpUploadContentResponse(@NotNull final Map<String, Integer> datatIdToStatus)
      throws IOException, CopyExceptionWithFailureReason {
    when(mediaInterface.uploadContent(any(Map.class), any(List.class)))
        .thenAnswer(
            (Answer<Map<String, DownUpResult>>)
                invocation -> {
                  Object[] args = invocation.getArguments();
                  final List<PhotosProtocol.AuthorizeUploadResponse> authorizeUploadResponseList =
                      (List<PhotosProtocol.AuthorizeUploadResponse>) args[1];
                  final Map<String, DownUpResult> fakeResponse = new HashMap<>();
                  for (PhotosProtocol.AuthorizeUploadResponse authorizeUploadResponse :
                      authorizeUploadResponseList) {
                    int fakeServerHttpStatus =
                        datatIdToStatus.get(authorizeUploadResponse.getDataId());
                    if (fakeServerHttpStatus == SC_OK) {
                      fakeResponse.put(
                          authorizeUploadResponse.getDataId(),
                          DownUpResult.ofDataId(
                              "fake-SingleUploadContentResponse-for-"
                                  + authorizeUploadResponse.getDataId()));
                    } else {
                      fakeResponse.put(
                          authorizeUploadResponse.getDataId(),
                          DownUpResult.ofError(
                              new IOException(
                                  String.format(
                                      "fake server error with status %d", fakeServerHttpStatus))));
                    }
                  }
                  return fakeResponse;
                });
  }

  protected void setUpCreateMediaResponse(@NotNull final Map<String, Integer> dataIdToStatus)
      throws IOException, CopyExceptionWithFailureReason {
    when(mediaInterface.createMedia(any(String.class), any(String.class), any(List.class)))
        .thenAnswer(
            (Answer<PhotosProtocol.CreateMediaResponse>)
                invocation -> {
                  Object[] args = invocation.getArguments();
                  final List<PhotosProtocol.NewMediaRequest> newMediaRequestList =
                      (List<PhotosProtocol.NewMediaRequest>) args[2];
                  final List<PhotosProtocol.NewMediaResponse> newMediaResponseList =
                      newMediaRequestList.stream()
                          .map(
                              newMediaRequest ->
                                  PhotosProtocol.NewMediaResponse.newBuilder()
                                      .setRecordId(
                                          MEDIA_RECORDID_BASE + newMediaRequest.getDataId())
                                      .setDataId(newMediaRequest.getDataId())
                                      .setFilesize(
                                          newMediaRequest.getDataId().startsWith(PHOTOS_DATAID_BASE)
                                              ? PHOTOS_FILE_SIZE
                                              : VIDEOS_FILE_SIZE)
                                      .setStatus(
                                          PhotosProtocol.Status.newBuilder()
                                              .setCode(
                                                  dataIdToStatus.get(newMediaRequest.getDataId()))
                                              .build())
                                      .build())
                          .collect(Collectors.toList());
                  return PhotosProtocol.CreateMediaResponse.newBuilder()
                      .addAllNewMediaResponses(newMediaResponseList)
                      .build();
                });
  }

  protected Map<String, Integer> setUpErrors(
      @NotNull final List<String> dataIds,
      @NotNull final int startIndex,
      @NotNull final int errorCount) {
    final Map<String, Integer> dataIdToStatus = new HashMap<>();
    for (int i = 0; i < dataIds.size(); i++) {
      if (i >= startIndex && i < startIndex + errorCount) {
        dataIdToStatus.put(dataIds.get(i), SC_INTERNAL_SERVER_ERROR);
      } else {
        dataIdToStatus.put(dataIds.get(i), SC_OK);
      }
    }
    return dataIdToStatus;
  }

  protected void checkKnownValues(@NotNull final Map<String, Serializable> expected) {
    assertThat(
        expected.entrySet().stream()
            .allMatch(e -> e.getValue().equals(executor.getCachedValue(e.getKey()))));
  }

  protected void checkErrors(@NotNull final List<ErrorDetail> expected) {
    final Map<String, ErrorDetail> actualIdToErrorDetail =
        executor.getErrors().stream()
            .collect(Collectors.toMap(ErrorDetail::id, errorDetail -> errorDetail));
    assertThat(actualIdToErrorDetail.size() == expected.size()).isTrue();
    for (ErrorDetail expectedErrorDetail : expected) {
      validateError(expectedErrorDetail, actualIdToErrorDetail.get(expectedErrorDetail.id()));
    }
  }

  protected void checkRecentErrors(@NotNull final List<ErrorDetail> expected) {
    final Map<String, ErrorDetail> actualIdToErrorDetail =
        executor.getRecentErrors().stream()
            .collect(Collectors.toMap(ErrorDetail::id, errorDetail -> errorDetail));
    assertThat(actualIdToErrorDetail.size() == expected.size()).isTrue();
    for (ErrorDetail expectedErrorDetail : expected) {
      validateError(expectedErrorDetail, actualIdToErrorDetail.get(expectedErrorDetail.id()));
    }
  }

  protected void validateError(
      @NotNull final ErrorDetail expected, @NotNull final ErrorDetail actual) {
    assertThat(actual.id()).isEqualTo(expected.id());
    assertThat(actual.title()).isEqualTo(expected.title());
    assertThat(actual.exception()).contains(expected.exception()); // the error message is a long stack trace, we just want to make sure
    // we have the right error code and error message
  }

  protected List<PhotoAlbum> createTestAlbums(@NotNull final int count) {
    final List<PhotoAlbum> photoAlbums = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      PhotoAlbum albumModel =
          new PhotoAlbum(ALBUM_DATAID_BASE + i, ALBUM_NAME_BASE + i, ALBUM_DESCRIPTION_BASE + i);
      photoAlbums.add(albumModel);
    }
    return photoAlbums;
  }

  protected List<PhotoModel> createTestPhotos(@NotNull final int count) {
    final List<PhotoModel> photos = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      PhotoModel photoModel =
          new PhotoModel(
              MEDIA_NAME_BASE + i,
              "fetchableUrl",
              "description",
              "mediaType",
              PHOTOS_DATAID_BASE + i,
              ALBUM_DATAID_BASE + i,
              false,
                  null,
                  new Date(),
                  new FavoriteInfo(true, new Date()));
      photos.add(photoModel);
    }
    return photos;
  }

  protected List<VideoModel> createTestVideos(@NotNull final int count) {
    final List<VideoModel> videos = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      VideoModel videoModel =
          new VideoModel(
              MEDIA_NAME_BASE + i,
              "contentUrl",
              "description",
              "encodingFormat",
              VIDEOS_DATAID_BASE + i,
              ALBUM_DATAID_BASE + i,
              false,
                  new Date(),
                  new FavoriteInfo(true, new Date()));
      videos.add(videoModel);
    }
    return videos;
  }
}
