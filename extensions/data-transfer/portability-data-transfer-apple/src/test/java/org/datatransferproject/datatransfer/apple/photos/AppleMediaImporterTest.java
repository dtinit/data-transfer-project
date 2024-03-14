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
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.datatransferproject.datatransfer.apple.photos.photosproto.PhotosProtocol;
import org.datatransferproject.spi.transfer.idempotentexecutor.RetryingInMemoryIdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.errors.ErrorDetail;
import org.datatransferproject.types.transfer.retry.NoRetryStrategy;
import org.datatransferproject.types.transfer.retry.RetryMapping;
import org.datatransferproject.types.transfer.retry.RetryStrategyLibrary;
import org.datatransferproject.types.transfer.retry.SkipRetryStrategy;
import org.datatransferproject.types.transfer.retry.UniformRetryStrategy;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

public class AppleMediaImporterTest extends AppleImporterTestBase {
  private AppleMediaImporter appleMediaImporter;
  private RetryingInMemoryIdempotentImportExecutor retryingExecutor;

  @BeforeEach
  public void setup() throws Exception {
    super.setup();

    RetryMapping skipMapping = new RetryMapping(new String[] {".*APPLE PHOTOS IMPORT: Fail to upload content.*"}, new SkipRetryStrategy());

    RetryMapping noRetryMapping = new RetryMapping(new String[] {".*APPLE PHOTOS IMPORT: iCloud Storage is full.*"}, new NoRetryStrategy());

    retryingExecutor = new RetryingInMemoryIdempotentImportExecutor(monitor,
            new RetryStrategyLibrary(
                    ImmutableList.of(skipMapping, noRetryMapping),
                    new UniformRetryStrategy(5, 2L, "identifier"))
    );
    appleMediaImporter =
        new AppleMediaImporter(
            new AppCredentials("key", "secret"), EXPORTING_SERVICE, monitor, factory, retryingExecutor, true);
  }

  @Test
  public void importPhotosVideosAndAlbums() throws Exception {
    // set up albums
    final int albumCount = 1;
    final List<MediaAlbum> mediaAlbums =
        createTestAlbums(albumCount).stream()
            .map(MediaAlbum::photoToMediaAlbum)
            .collect(Collectors.toList());
    setUpCreateAlbumsResponse(
        mediaAlbums.stream()
            .collect(
                Collectors.toMap(MediaAlbum::getId, photoAlbum -> SC_OK)));

    // set up photos
    final int photoCount = 2;
    final List<PhotoModel> photos = createTestPhotos(photoCount);
    final Map<String, Integer> dataIdToStatus =
        photos.stream()
            .collect(
                Collectors.toMap(PhotoModel::getDataId, photoModel -> SC_OK));

    // set up videos
    final int videoCount = 3;
    final List<VideoModel> videos =
        createTestVideos(videoCount).stream().collect(Collectors.toList());
    dataIdToStatus.putAll(
        videos.stream()
            .collect(
                Collectors.toMap(
                    VideoModel::getDataId, videoModel -> SC_OK)));
    setUpGetUploadUrlResponse(dataIdToStatus);
    setUpUploadContentResponse(dataIdToStatus);
    setUpCreateMediaResponse(dataIdToStatus);

    MediaContainerResource mediaData = new MediaContainerResource(mediaAlbums, photos, videos);

    final ImportResult importResult =
        appleMediaImporter.importItem(uuid, executor, authData, mediaData);

    // verify correct methods were called
    final List<String> photosDataIds =
        photos.stream().map(PhotoModel::getDataId).collect(Collectors.toList());
    final List<String> videosDataIds =
        videos.stream().map(VideoModel::getDataId).collect(Collectors.toList());

    verify(mediaInterface)
        .createAlbums(uuid.toString(), DataVertical.MEDIA.getDataType(), mediaAlbums);
    verify(mediaInterface)
        .getUploadUrl(uuid.toString(), DataVertical.MEDIA.getDataType(), photosDataIds);
    verify(mediaInterface)
        .getUploadUrl(uuid.toString(), DataVertical.MEDIA.getDataType(), videosDataIds);
    verify(mediaInterface, times(2)).uploadContent(anyMap(), anyList());
    verify(mediaInterface, times(2)).createMedia(anyString(), anyString(), argThat(newMediaRequestList -> {
      assertThat(newMediaRequestList).isNotNull();
      assertThat(newMediaRequestList.stream().allMatch(newMediaRequest -> newMediaRequest.hasCreationDateInMillis())).isTrue();
      return true;
    }));

    // check the result
    assertThat(importResult.getCounts().isPresent()).isTrue();
    assertThat(
        importResult.getCounts().get().get(PhotosContainerResource.ALBUMS_COUNT_DATA_NAME) == 1).isTrue();
    assertThat(
        importResult.getCounts().get().get(PhotosContainerResource.PHOTOS_COUNT_DATA_NAME)
            == photoCount).isTrue();
    assertThat(
        importResult.getCounts().get().get(VideosContainerResource.VIDEOS_COUNT_DATA_NAME)
            == videoCount).isTrue();

    assertThat(
        importResult.getBytes().get()
            == photoCount * PHOTOS_FILE_SIZE + videoCount * VIDEOS_FILE_SIZE).isTrue();

    final Map<String, Serializable> expectedKnownValue =
        mediaAlbums.stream()
            .collect(
                Collectors.toMap(
                    MediaAlbum::getId, mediaAlbum -> ALBUM_RECORDID_BASE + mediaAlbum.getId()));
    expectedKnownValue.putAll(
        photos.stream()
            .collect(
                Collectors.toMap(
                    photoModel -> photoModel.getAlbumId() + "-" + photoModel.getDataId(),
                    photoModel -> MEDIA_RECORDID_BASE + photoModel.getDataId())));
    expectedKnownValue.putAll(
        videos.stream()
            .collect(
                Collectors.toMap(
                    videoModel -> videoModel.getDataId(),
                    videoModel -> MEDIA_RECORDID_BASE + videoModel.getDataId())));
    checkKnownValues(expectedKnownValue);
  }

  @Test
  public void testUploadContentErrorsSkipped() throws Exception {
    // set up photos
    final int photoCount = 3;
    final List<PhotoModel> photos = createTestPhotos(photoCount);

    final Map<String, Integer> dataIdToStatus =
            photos.stream()
                    .collect(
                            Collectors.toMap(PhotoModel::getDataId, photoModel -> SC_OK));

    // get upload url and create media calls will succeed
    setUpGetUploadUrlResponse(dataIdToStatus);
    setUpCreateMediaResponse(dataIdToStatus);

    // Same fake data set as before, but with 2 of the existing photos failing somewhere in the
    // download/upload sequence.
    ImmutableMap<String, Integer> dataIdToStatusForFailingDownupSeq = new ImmutableMap.Builder<String, Integer>()
        .put(PHOTOS_DATAID_BASE + 0, SC_INTERNAL_SERVER_ERROR)
        .put(PHOTOS_DATAID_BASE + 1, SC_SERVICE_UNAVAILABLE)
        .build();
    ImmutableMap<String, Integer> dataIdToStatusForDownupSeq = new ImmutableMap.Builder<String, Integer>()
        .putAll(dataIdToStatus)
        .putAll(dataIdToStatusForFailingDownupSeq)
        .buildKeepingLast();
    setUpUploadContentResponse(dataIdToStatusForDownupSeq);

    MediaContainerResource mediaData = new MediaContainerResource(new ArrayList<>(), photos, new ArrayList<>());

    final ImportResult importResult =
            appleMediaImporter.importItem(uuid, executor, authData, mediaData);

    // verify correct methods were called
    final List<String> photosDataIds =
            photos.stream().map(PhotoModel::getDataId).collect(Collectors.toList());

    verify(mediaInterface)
            .getUploadUrl(uuid.toString(), DataVertical.MEDIA.getDataType(), photosDataIds);

    verify(mediaInterface).uploadContent(anyMap(), anyList());
    verify(mediaInterface).createMedia(anyString(), anyString(), anyList());

    // check the result
    assertThat(importResult.getCounts().isPresent()).isTrue();
    assertThat(
            importResult.getCounts().get().get(PhotosContainerResource.PHOTOS_COUNT_DATA_NAME)
                    == photoCount - dataIdToStatusForFailingDownupSeq.size()).isTrue();

    assertThat(
            importResult.getBytes().get()
                    == (photoCount - dataIdToStatusForFailingDownupSeq.size()) * PHOTOS_FILE_SIZE).isTrue();

    final Map<String, Serializable> expectedKnownValue =
            photos.stream()
                    .filter(photoModel -> !dataIdToStatusForFailingDownupSeq.containsKey(photoModel.getDataId()))
                    .collect(
                            Collectors.toMap(
                                    photoModel -> photoModel.getAlbumId() + "-" + photoModel.getDataId(),
                                    photoModel -> MEDIA_RECORDID_BASE + photoModel.getDataId()));
    checkKnownValues(expectedKnownValue);

    //check errors
    List<ErrorDetail> expectedErrors = new ArrayList<>();
    for (String errorDataId : dataIdToStatusForFailingDownupSeq.keySet()) {
      final PhotoModel photoModel = photos.stream().filter(p -> p.getDataId().equals(errorDataId)).findFirst().get();
      final ErrorDetail.Builder errorDetailBuilder =
              ErrorDetail.builder()
                      .setId(photoModel.getIdempotentId())
                      .setTitle(photoModel.getTitle())
                      .setException(AppleMediaInterface.getApplePhotosImportThrowingMessage("Fail to upload content"));
      expectedErrors.add(errorDetailBuilder.build());
    }

    checkErrors(expectedErrors);
    checkRecentErrors(new ArrayList<>()); // recent error should be empty to avoid retries in lower stack like data copier

    Assert.assertTrue(retryingExecutor.getRecentErrors().stream().allMatch(ErrorDetail::canSkip));
  }

  @Test
  public void testDestinationFullErrorNonRetryable() throws Exception {
    // set up photos
    final int photoCount = 3;
    final List<PhotoModel> photos = createTestPhotos(photoCount);

    final Map<String, Integer> dataIdToStatus =
            photos.stream()
                    .collect(
                            Collectors.toMap(PhotoModel::getDataId, photoModel -> SC_OK));

    // get upload url and create media calls will succeed
    setUpGetUploadUrlResponse(dataIdToStatus);
    setUpUploadContentResponse(dataIdToStatus);

    when(mediaInterface.createMedia(any(String.class), any(String.class), any(List.class)))
            .thenThrow(new DestinationMemoryFullException(AppleMediaInterface.getApplePhotosImportThrowingMessage("iCloud Storage is full"), new IOException("iCloud Storage is full")));


    MediaContainerResource mediaData = new MediaContainerResource(new ArrayList<>(), photos, new ArrayList<>());

    Assert.assertThrows(AppleMediaInterface.getApplePhotosImportThrowingMessage("iCloud Storage is full"), DestinationMemoryFullException.class,
            () -> appleMediaImporter.importItem(uuid, executor, authData, mediaData));

    // non retriable error will only invoke the API once
    verify(mediaInterface, times(1)).getUploadUrl(anyString(), anyString(), anyList());
    verify(mediaInterface, times(1)).uploadContent(anyMap(), anyList());
    verify(mediaInterface, times(1)).createMedia(anyString(), anyString(), anyList());

    Assert.assertTrue(retryingExecutor.getRecentErrors().stream().allMatch(errorDetail -> !errorDetail.canSkip()));
  }

  @Test
  public void testInternalServerErrorRetryableNotSkippable() throws Exception {
    // set up photos
    final int photoCount = 3;
    final List<PhotoModel> photos = createTestPhotos(photoCount);

    final Map<String, Integer> dataIdToStatus =
            photos.stream()
                    .collect(
                            Collectors.toMap(PhotoModel::getDataId, photoModel -> SC_OK));

    // get upload url and create media calls will succeed
    setUpGetUploadUrlResponse(dataIdToStatus);
    setUpUploadContentResponse(dataIdToStatus);

    when(mediaInterface.createMedia(any(String.class), any(String.class), any(List.class)))
            .thenThrow(new IOException(AppleMediaInterface.getApplePhotosImportThrowingMessage("Internal server error in iCloud Photos service")));

    MediaContainerResource mediaData = new MediaContainerResource(new ArrayList<>(), photos, new ArrayList<>());

    Assert.assertThrows(AppleMediaInterface.getApplePhotosImportThrowingMessage("Internal server error in iCloud Photos service"), IOException.class,
            () -> appleMediaImporter.importItem(uuid, executor, authData, mediaData));

    // retriable error will invoke the API multiple times
    verify(mediaInterface, times(6)).getUploadUrl(anyString(), anyString(), anyList());
    verify(mediaInterface, times(6)).uploadContent(anyMap(), anyList());
    verify(mediaInterface, times(6)).createMedia(anyString(), anyString(), anyList());
    Assert.assertTrue(retryingExecutor.getRecentErrors().stream().allMatch(errorDetail -> !errorDetail.canSkip()));

  }


  @Test
  public void testInternalServerErrorSucceedInRetry() throws Exception {
    // set up photos
    final int photoCount = 3;
    final List<PhotoModel> photos = createTestPhotos(photoCount);

    final Map<String, Integer> dataIdToStatus =
            photos.stream()
                    .collect(
                            Collectors.toMap(PhotoModel::getDataId, photoModel -> SC_OK));

    // get upload url and create media calls will succeed
    setUpGetUploadUrlResponse(dataIdToStatus);
    setUpUploadContentResponse(dataIdToStatus);

    when(mediaInterface.createMedia(any(String.class), any(String.class), any(List.class)))
            .thenThrow(new IOException(AppleMediaInterface.getApplePhotosImportThrowingMessage("Internal server error in iCloud Photos service")))
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
                            });;


    MediaContainerResource mediaData = new MediaContainerResource(new ArrayList<>(), photos, new ArrayList<>());

    final ImportResult importResult =
            appleMediaImporter.importItem(uuid, executor, authData, mediaData);

    // check the result
    assertThat(importResult.getCounts().isPresent()).isTrue();
    assertThat(
            importResult.getCounts().get().get(PhotosContainerResource.PHOTOS_COUNT_DATA_NAME)
                    == photoCount).isTrue();

    assertThat(
            importResult.getBytes().get()
                    == (photoCount) * PHOTOS_FILE_SIZE).isTrue();

    // retriable error will invoke the API multiple times
    verify(mediaInterface, times(2)).getUploadUrl(anyString(), anyString(), anyList());
    verify(mediaInterface, times(2)).uploadContent(anyMap(), anyList());
    verify(mediaInterface, times(2)).createMedia(anyString(), anyString(), anyList());
    Assert.assertTrue(retryingExecutor.getRecentErrors().stream().allMatch(errorDetail -> !errorDetail.canSkip()));

    checkRecentErrors(new ArrayList<>()); // recent error should be empty to avoid additional retries in lower stack like data copier
  }

  private void setUpCreateAlbumsResponse(@NotNull final Map<String, Integer> datatIdToStatus)
      throws IOException, CopyExceptionWithFailureReason {
    when(mediaInterface.createAlbums(any(String.class), any(String.class), any(Collection.class)))
        .thenAnswer(
            (Answer<PhotosProtocol.CreateAlbumsResponse>)
                invocation -> {
                  Object[] args = invocation.getArguments();
                  final Collection<MediaAlbum> mediaAlbums = (Collection<MediaAlbum>) args[2];
                  final List<PhotosProtocol.NewPhotoAlbumResponse> newPhotoAlbumResponseList =
                      mediaAlbums.stream()
                          .map(
                              mediaAlbum ->
                                  PhotosProtocol.NewPhotoAlbumResponse.newBuilder()
                                      .setRecordId(
                                          ALBUM_RECORDID_BASE + mediaAlbum.getIdempotentId())
                                      .setDataId(mediaAlbum.getIdempotentId())
                                      .setName(mediaAlbum.getName())
                                      .setStatus(
                                          PhotosProtocol.Status.newBuilder()
                                              .setCode(datatIdToStatus.get(mediaAlbum.getId()))
                                              .build())
                                      .build())
                          .collect(Collectors.toList());
                  return PhotosProtocol.CreateAlbumsResponse.newBuilder()
                      .addAllNewPhotoAlbumResponses(newPhotoAlbumResponseList)
                      .build();
                });
  }
}
