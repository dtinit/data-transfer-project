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
import static org.datatransferproject.types.common.models.photos.PhotosContainerResource.ALBUMS_COUNT_DATA_NAME;
import static org.datatransferproject.types.common.models.photos.PhotosContainerResource.PHOTOS_COUNT_DATA_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyCollection;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.datatransferproject.datatransfer.apple.constants.ApplePhotosConstants;
import org.datatransferproject.datatransfer.apple.photos.photosproto.PhotosProtocol.CreateAlbumsResponse;
import org.datatransferproject.datatransfer.apple.photos.photosproto.PhotosProtocol.NewPhotoAlbumResponse;
import org.datatransferproject.datatransfer.apple.photos.photosproto.PhotosProtocol.Status;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.errors.ErrorDetail;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

public class ApplePhotosImporterTest extends AppleImporterTestBase {
  private ApplePhotosImporter applePhotosImporter;

  @BeforeEach
  public void setup() throws Exception {
    super.setup();
    applePhotosImporter =
      new ApplePhotosImporter(
        new AppCredentials("key", "secret"), EXPORTING_SERVICE, monitor, factory);
  }

  @Test
  public void importAlbums() throws Exception {
    // set up
    final int albumCount = 1;
    final List<PhotoAlbum> photoAlbums = createTestAlbums(albumCount);
    setUpCreateAlbumsResponse(
        photoAlbums.stream()
            .collect(
                Collectors.toMap(PhotoAlbum::getId, photoAlbum -> SC_OK)));

    // run test
    PhotosContainerResource data = new PhotosContainerResource(photoAlbums, null);
    final ImportResult importResult =
        applePhotosImporter.importItem(uuid, executor, authData, data);

    // verify correct methods were called
    verify(mediaInterface)
        .createAlbums(
            uuid.toString(),
            DataVertical.PHOTOS.getDataType(),
            data.getAlbums().stream()
                .map(MediaAlbum::photoToMediaAlbum)
                .collect(Collectors.toList()));

    // check the result
    assertThat(importResult.getCounts().isPresent());
    assertThat(importResult.getCounts().get().get(ALBUMS_COUNT_DATA_NAME) == albumCount);
    final Map<String, Serializable> expectedKnownValue =
        photoAlbums.stream()
            .collect(
                Collectors.toMap(
                    PhotoAlbum::getId, photoAlbum -> ALBUM_RECORDID_BASE + photoAlbum.getId()));
    checkKnownValues(expectedKnownValue);
  }

  @Test
  public void importAlbumsMultipleBatches() throws Exception {
    // set up
    final int albumCount = ApplePhotosConstants.maxNewAlbumRequests + 1;
    final List<PhotoAlbum> photoAlbums = createTestAlbums(albumCount);
    setUpCreateAlbumsResponse(
        photoAlbums.stream()
            .collect(
                Collectors.toMap(
                    PhotoAlbum::getId, photoAlbum -> SC_OK)));

    // run test
    PhotosContainerResource data = new PhotosContainerResource(photoAlbums, null);
    final ImportResult importResult =
        applePhotosImporter.importItem(uuid, executor, authData, data);

    // verify correct methods were called
    verify(mediaInterface, times(2)).createAlbums(anyString(), anyString(), anyCollection());
    verify(mediaInterface)
        .createAlbums(
            uuid.toString(),
            DataVertical.PHOTOS.getDataType(),
            photoAlbums.subList(0, ApplePhotosConstants.maxNewAlbumRequests).stream()
                .map(MediaAlbum::photoToMediaAlbum)
                .collect(Collectors.toList()));
    verify(mediaInterface)
        .createAlbums(
            uuid.toString(),
            DataVertical.PHOTOS.getDataType(),
            photoAlbums.subList(ApplePhotosConstants.maxNewAlbumRequests, albumCount).stream()
                .map(MediaAlbum::photoToMediaAlbum)
                .collect(Collectors.toList()));

    // check the result
    assertThat(importResult.getCounts().isPresent());
    assertThat(importResult.getCounts().get().get(ALBUMS_COUNT_DATA_NAME) == albumCount);
    final Map<String, Serializable> expectedKnownValue =
        photoAlbums.stream()
            .collect(
                Collectors.toMap(
                    PhotoAlbum::getId, photoAlbum -> ALBUM_RECORDID_BASE + photoAlbum.getId()));
    checkKnownValues(expectedKnownValue);
  }

  @Test
  public void importAlbumsWithFailure() throws Exception {
    // set up
    final int albumCount = ApplePhotosConstants.maxNewAlbumRequests + 1;
    final int errorCount = 10;
    final List<PhotoAlbum> photoAlbums = createTestAlbums(albumCount);
    final Map<String, Integer> datatIdToStatus =
        setUpErrors(
            photoAlbums.stream().map(PhotoAlbum::getId).collect(Collectors.toList()),
            0,
            errorCount);
    setUpCreateAlbumsResponse(datatIdToStatus);

    // run test
    PhotosContainerResource data = new PhotosContainerResource(photoAlbums, null);
    final ImportResult importResult =
        applePhotosImporter.importItem(uuid, executor, authData, data);

    // verify correct methods were called
    verify(mediaInterface, times(2)).createAlbums(anyString(), anyString(), anyCollection());
    verify(mediaInterface)
        .createAlbums(
            uuid.toString(),
            DataVertical.PHOTOS.getDataType(),
            photoAlbums.subList(0, ApplePhotosConstants.maxNewAlbumRequests).stream()
                .map(MediaAlbum::photoToMediaAlbum)
                .collect(Collectors.toList()));
    verify(mediaInterface)
        .createAlbums(
            uuid.toString(),
            DataVertical.PHOTOS.getDataType(),
            photoAlbums.subList(ApplePhotosConstants.maxNewAlbumRequests, albumCount).stream()
                .map(MediaAlbum::photoToMediaAlbum)
                .collect(Collectors.toList()));

    // check the result
    assertThat(importResult.getCounts().isPresent());
    assertThat(
        importResult.getCounts().get().get(ALBUMS_COUNT_DATA_NAME) == albumCount - errorCount);
    final Map<String, Serializable> expectedKnownValue =
        photoAlbums.stream()
            .filter(
                photoAlbum ->
                    datatIdToStatus.get(photoAlbum.getId()) == SC_OK)
            .collect(
                Collectors.toMap(
                    PhotoAlbum::getId, photoAlbum -> ALBUM_RECORDID_BASE + photoAlbum.getId()));
    checkKnownValues(expectedKnownValue);

    // check errors
    List<ErrorDetail> expectedErrors = new ArrayList<>();
    for (int i = 0; i < errorCount; i++) {
      final ErrorDetail errorDetail =
          ErrorDetail.builder()
              .setId(ALBUM_DATAID_BASE + i)
              .setTitle(ALBUM_NAME_BASE + i)
              .setException(
                  String.format(
                      "java.io.IOException: Failed to create album, error code: %d",
                      SC_INTERNAL_SERVER_ERROR))
              .build();
      expectedErrors.add(errorDetail);
    }
    checkErrors(expectedErrors);
    checkRecentErrors(expectedErrors);
  }

  @Test
  public void importSinglePhoto() throws Exception {
    // set up
    final int photoCount = 1;
    final List<PhotoModel> photos = createTestPhotos(photoCount);
    final Map<String, Integer> dataIdToStatus =
        photos.stream()
            .collect(
                Collectors.toMap(
                    PhotoModel::getDataId,
                    photoModel -> SC_OK));
    setUpGetUploadUrlResponse(dataIdToStatus);
    setUpUploadContentResponse(dataIdToStatus);
    setUpCreateMediaResponse(dataIdToStatus);

    // run test
    PhotosContainerResource data = new PhotosContainerResource(null, photos);
    final ImportResult importResult =
        applePhotosImporter.importItem(uuid, executor, authData, data);

    // verify correct methods were called
    final List<String> dataIds =
        photos.stream().map(PhotoModel::getDataId).collect(Collectors.toList());
    verify(mediaInterface)
        .getUploadUrl(
            uuid.toString(), DataVertical.PHOTOS.getDataType(), dataIds);
    verify(mediaInterface).uploadContent(anyMap(), anyList());
    verify(mediaInterface).createMedia(anyString(), anyString(), anyList());

    // check the result
    assertThat(importResult.getCounts().isPresent());
    assertThat(importResult.getCounts().get().get(ALBUMS_COUNT_DATA_NAME) == 0);
    assertThat(importResult.getCounts().get().get(PHOTOS_COUNT_DATA_NAME) == photoCount);
    assertThat(importResult.getBytes().get() == photoCount * PHOTOS_FILE_SIZE);

    final Map<String, Serializable> expectedKnownValue =
        photos.stream()
            .collect(
                Collectors.toMap(
                    photoModel -> photoModel.getAlbumId() + "-" + photoModel.getDataId(),
                    photoModel -> MEDIA_RECORDID_BASE + photoModel.getDataId()));
    checkKnownValues(expectedKnownValue);
  }

  @Test
  public void importPhotosMultipleBatches() throws Exception {
    // set up
    final int photoCount = ApplePhotosConstants.maxNewMediaRequests + 1;
    final List<PhotoModel> photos = createTestPhotos(photoCount);
    final Map<String, Integer> dataIdToStatus =
        photos.stream()
            .collect(
                Collectors.toMap(
                    PhotoModel::getDataId,
                    photoModel -> SC_OK));
    setUpGetUploadUrlResponse(dataIdToStatus);
    setUpUploadContentResponse(dataIdToStatus);
    setUpCreateMediaResponse(dataIdToStatus);

    // run test
    PhotosContainerResource data = new PhotosContainerResource(null, photos);
    final ImportResult importResult =
        applePhotosImporter.importItem(uuid, executor, authData, data);

    // verify correct methods were called
    verify(mediaInterface, times(2)).getUploadUrl(anyString(), anyString(), anyList());
    verify(mediaInterface)
        .getUploadUrl(
            uuid.toString(),
            DataVertical.PHOTOS.getDataType(),
            photos.subList(0, ApplePhotosConstants.maxNewMediaRequests).stream()
                .map(PhotoModel::getDataId)
                .collect(Collectors.toList()));
    verify(mediaInterface)
        .getUploadUrl(
            uuid.toString(),
            DataVertical.PHOTOS.getDataType(),
            photos.subList(ApplePhotosConstants.maxNewMediaRequests, photoCount).stream()
                .map(PhotoModel::getDataId)
                .collect(Collectors.toList()));

    verify(mediaInterface, times(2)).uploadContent(anyMap(), anyList());
    verify(mediaInterface, times(2)).createMedia(anyString(), anyString(), anyList());

    // check the result
    assertThat(importResult.getCounts().isPresent());
    assertThat(importResult.getCounts().get().get(ALBUMS_COUNT_DATA_NAME) == 0);
    assertThat(importResult.getCounts().get().get(PHOTOS_COUNT_DATA_NAME) == photoCount);
    assertThat(importResult.getBytes().get() == photoCount * PHOTOS_FILE_SIZE);

    final Map<String, Serializable> expectedKnownValue =
        photos.stream()
            .collect(
                Collectors.toMap(
                    photoModel -> photoModel.getAlbumId() + "-" + photoModel.getDataId(),
                    photoModel -> MEDIA_RECORDID_BASE + photoModel.getDataId()));
    checkKnownValues(expectedKnownValue);
  }

  @Test
  public void importPhotosWithFailure() throws Exception {
    // set up
    final int photoCount = ApplePhotosConstants.maxNewMediaRequests + 1;
    final List<PhotoModel> photos = createTestPhotos(photoCount);

    // different errors in different steps
    final int errorCountGetUploadURL = 10;
    final int errorCountUploadContent = 10;
    final int errorCountCreateMedia = 10;
    final int successCount =
        photoCount - errorCountGetUploadURL - errorCountUploadContent - errorCountCreateMedia;
    final List<String> dataIds =
        photos.stream().map(PhotoModel::getDataId).collect(Collectors.toList());
    final Map<String, Integer> datatIdToGetUploadURLStatus =
        setUpErrors(dataIds, 0, errorCountGetUploadURL);
    final Map<String, Integer> datatIdToUploadContentStatus =
        setUpErrors(dataIds, errorCountGetUploadURL, errorCountUploadContent);
    final Map<String, Integer> datatIdToCreateMediaStatus =
        setUpErrors(
            dataIds, errorCountGetUploadURL + errorCountUploadContent, errorCountCreateMedia);
    setUpGetUploadUrlResponse(datatIdToGetUploadURLStatus);
    setUpUploadContentResponse(datatIdToUploadContentStatus);
    setUpCreateMediaResponse(datatIdToCreateMediaStatus);

    // run test
    PhotosContainerResource data = new PhotosContainerResource(null, photos);
    ImportResult importResult = applePhotosImporter.importItem(uuid, executor, authData, data);

    // verify correct methods were called
    verify(mediaInterface, times(2)).getUploadUrl(anyString(), anyString(), anyList());
    verify(mediaInterface)
        .getUploadUrl(
            uuid.toString(),
            DataVertical.PHOTOS.getDataType(),
            photos.subList(0, ApplePhotosConstants.maxNewMediaRequests).stream()
                .map(PhotoModel::getDataId)
                .collect(Collectors.toList()));
    verify(mediaInterface)
        .getUploadUrl(
            uuid.toString(),
            DataVertical.PHOTOS.getDataType(),
            photos.subList(ApplePhotosConstants.maxNewMediaRequests, photoCount).stream()
                .map(PhotoModel::getDataId)
                .collect(Collectors.toList()));

    verify(mediaInterface, times(2)).uploadContent(anyMap(), anyList());
    verify(mediaInterface, times(2)).createMedia(anyString(), anyString(), anyList());

    // check the result
    assertThat(importResult.getCounts().isPresent());
    assertThat(importResult.getCounts().get().get(ALBUMS_COUNT_DATA_NAME) == 0);
    assertThat(importResult.getCounts().get().get(PHOTOS_COUNT_DATA_NAME) == successCount);
    assertThat(importResult.getBytes().get() == successCount * PHOTOS_FILE_SIZE);

    final Map<String, Serializable> expectedKnownValue =
        photos.stream()
            .filter(
                photoModel ->
                    datatIdToGetUploadURLStatus.get(photoModel.getDataId())
                        == SC_OK)
            .filter(
                photoModel ->
                    datatIdToUploadContentStatus.get(photoModel.getDataId())
                        == SC_OK)
            .filter(
                photoModel ->
                    datatIdToCreateMediaStatus.get(photoModel.getDataId())
                        == SC_OK)
            .collect(
                Collectors.toMap(
                    photoModel -> photoModel.getAlbumId() + "-" + photoModel.getDataId(),
                    photoModel -> MEDIA_RECORDID_BASE + photoModel.getDataId()));
    checkKnownValues(expectedKnownValue);

    // check errors
    List<ErrorDetail> expectedErrors = new ArrayList<>();
    for (int i = 0;
        i < errorCountGetUploadURL + errorCountUploadContent + errorCountCreateMedia;
        i++) {
      final PhotoModel photoModel = photos.get(i);
      final ErrorDetail.Builder errorDetailBuilder =
          ErrorDetail.builder()
              .setId(photoModel.getIdempotentId())
              .setTitle(photoModel.getTitle())
              .setException(
                  String.format(
                      "java.io.IOException: Fail to get upload url, error code: %d",
                      SC_INTERNAL_SERVER_ERROR));
      if (i < errorCountGetUploadURL) {
        errorDetailBuilder.setException(
            String.format(
                "java.io.IOException: Fail to get upload url, error code: %d",
                SC_INTERNAL_SERVER_ERROR));
      } else if (i < errorCountGetUploadURL + errorCountGetUploadURL) {
        errorDetailBuilder.setException("java.io.IOException: Fail to upload content");
      } else {
        errorDetailBuilder.setException(
            String.format(
                "java.io.IOException: Fail to create media, error code: %d",
                SC_INTERNAL_SERVER_ERROR));
      }
      expectedErrors.add(errorDetailBuilder.build());
    }

    checkErrors(expectedErrors);
    checkRecentErrors(expectedErrors);
  }

  private void setUpCreateAlbumsResponse(@NotNull final Map<String, Integer> datatIdToStatus)
      throws IOException, CopyExceptionWithFailureReason {
    when(mediaInterface.createAlbums(any(String.class), any(String.class), any(Collection.class)))
        .thenAnswer(
            (Answer<CreateAlbumsResponse>)
                invocation -> {
                  Object[] args = invocation.getArguments();
                  final Collection<MediaAlbum> mediaAlbums = (Collection<MediaAlbum>) args[2];
                  final List<NewPhotoAlbumResponse> newPhotoAlbumResponseList =
                      mediaAlbums.stream()
                          .map(
                              mediaAlbum ->
                                  NewPhotoAlbumResponse.newBuilder()
                                      .setRecordId(
                                          ALBUM_RECORDID_BASE + mediaAlbum.getIdempotentId())
                                      .setDataId(mediaAlbum.getIdempotentId())
                                      .setName(mediaAlbum.getName())
                                      .setStatus(
                                          Status.newBuilder()
                                              .setCode(datatIdToStatus.get(mediaAlbum.getId()))
                                              .build())
                                      .build())
                          .collect(Collectors.toList());
                  return CreateAlbumsResponse.newBuilder()
                      .addAllNewPhotoAlbumResponses(newPhotoAlbumResponseList)
                      .build();
                });
  }
}
