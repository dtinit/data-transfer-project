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

package org.datatransferproject.transfer.microsoft.media;

import static com.google.common.truth.Truth.assertThat;
import static org.datatransferproject.transfer.microsoft.media.MicrosoftMediaExporter.DRIVE_TOKEN_PREFIX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.google.api.client.json.jackson2.JacksonFactory;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.transfer.microsoft.common.MicrosoftCredentialFactory;
import org.datatransferproject.transfer.microsoft.driveModels.*;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.ContainerResource;
import org.datatransferproject.types.common.models.IdOnlyContainerResource;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests of Microsoft onedrive exporter for Media models. */
public class MicrosoftMediaExporterTest {
  private String PHOTO_URI = "imageDownloadUri";
  private String PHOTO_ID = "uniquePhotoId";
  private String PHOTO_FILENAME = "selfie-taken-by-cat.jpg";
  private String VIDEO_URI = "videoDownloadUri";
  private String VIDEO_ID = "uniqueVideoId";
  private String VIDEO_FILENAME = "cats-dancing.ogg";
  private String FOLDER_ID = "microsoftOneDriveFolderId";
  private String DRIVE_PAGE_URL = "driveToken";
  private UUID uuid = UUID.randomUUID();

  private TokensAndUrlAuthData token;
  private MicrosoftMediaExporter microsoftMediaExporter;
  private MicrosoftMediaInterface mediaInterface;

  private MicrosoftDriveItemsResponse driveItemsResponse;

  @BeforeEach
  public void setUp() throws IOException {
    MicrosoftCredentialFactory credentialFactory = mock(MicrosoftCredentialFactory.class);
    mediaInterface = mock(MicrosoftMediaInterface.class);
    driveItemsResponse = mock(MicrosoftDriveItemsResponse.class);
    Monitor monitor = mock(Monitor.class);

    microsoftMediaExporter = new MicrosoftMediaExporter(
        credentialFactory, new JacksonFactory(), mediaInterface, monitor);

    when(mediaInterface.getDriveItems(any(Optional.class), any(Optional.class)))
        .thenReturn(driveItemsResponse);
    when(mediaInterface.getDriveItemsFromSpecialFolder(
             any(MicrosoftSpecialFolder.FolderType.class)))
        .thenReturn(driveItemsResponse);
    verifyNoInteractions(credentialFactory);
  }

  @Test
  public void exportOneAlbumWithNextPage() throws IOException {
    // Setup
    MicrosoftDriveItem folderItem = setUpSingleAlbum();
    when(driveItemsResponse.getDriveItems()).thenReturn(new MicrosoftDriveItem[] {folderItem});
    when(driveItemsResponse.getNextPageLink()).thenReturn(DRIVE_PAGE_URL);

    // Run
    ExportResult<MediaContainerResource> result = microsoftMediaExporter.exportOneDrivePhotos(
        null, Optional.empty(), Optional.empty(), uuid);

    // Verify method calls
    verify(mediaInterface)
        .getDriveItemsFromSpecialFolder(MicrosoftSpecialFolder.FolderType.photos);
    verify(driveItemsResponse).getDriveItems();

    // Verify pagination token is set
    ContinuationData continuationData = result.getContinuationData();
    StringPaginationToken paginationToken =
        (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationToken.getToken()).isEqualTo(DRIVE_TOKEN_PREFIX + DRIVE_PAGE_URL);

    // Verify one album is ready for import
    Collection<MediaAlbum> actualAlbums = result.getExportedData().getAlbums();
    assertThat(actualAlbums.stream().map(MediaAlbum::getId).collect(Collectors.toList()))
        .containsExactly(FOLDER_ID);

    // Verify photos should be empty (in the root)
    Collection<PhotoModel> actualPhotos = result.getExportedData().getPhotos();
    assertThat(actualPhotos).isEmpty();

    // Verify there is one container ready for sub-processing
    List<ContainerResource> actualResources = continuationData.getContainerResources();
    assertThat(actualResources.stream()
                   .map(a -> ((IdOnlyContainerResource) a).getId())
                   .collect(Collectors.toList()))
        .containsExactly(FOLDER_ID);
  }

  @Test
  public void exportAlbumWithoutNextPage() throws IOException {
    // Setup
    MicrosoftDriveItem folderItem = setUpSingleAlbum();
    when(driveItemsResponse.getDriveItems()).thenReturn(new MicrosoftDriveItem[] {folderItem});
    when(driveItemsResponse.getNextPageLink()).thenReturn(null);
    StringPaginationToken inputPaginationToken =
        new StringPaginationToken(DRIVE_TOKEN_PREFIX + DRIVE_PAGE_URL);

    // Run
    ExportResult<MediaContainerResource> result = microsoftMediaExporter.exportOneDrivePhotos(
        null, Optional.empty(), Optional.of(inputPaginationToken), uuid);

    // Verify method calls
    verify(mediaInterface).getDriveItems(Optional.empty(), Optional.of(DRIVE_PAGE_URL));
    verify(driveItemsResponse).getDriveItems();

    // Verify next pagination token is absent
    ContinuationData continuationData = result.getContinuationData();
    StringPaginationToken paginationToken =
        (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationToken).isEqualTo(null);

    // Verify one album is ready for import
    Collection<MediaAlbum> actualAlbums = result.getExportedData().getAlbums();
    assertThat(actualAlbums.stream().map(MediaAlbum::getId).collect(Collectors.toList()))
        .containsExactly(FOLDER_ID);

    // Verify photos should be empty (in the root)
    Collection<PhotoModel> actualPhotos = result.getExportedData().getPhotos();
    assertThat(actualPhotos).isEmpty();

    // Verify there is one container ready for sub-processing
    List<ContainerResource> actualResources = continuationData.getContainerResources();
    assertThat(actualResources.stream()
                   .map(a -> ((IdOnlyContainerResource) a).getId())
                   .collect(Collectors.toList()))
        .containsExactly(FOLDER_ID);
  }

  @Test
  public void exportMediaWithNextPage() throws IOException {
    // Setup
    when(driveItemsResponse.getNextPageLink()).thenReturn(null);
    when(driveItemsResponse.getDriveItems()).thenReturn(new MicrosoftDriveItem[] {
      setUpSinglePhoto(PHOTO_FILENAME, PHOTO_URI, PHOTO_ID),
      setUpSingleVideo(VIDEO_FILENAME, VIDEO_URI, VIDEO_ID)
    });
    when(driveItemsResponse.getNextPageLink()).thenReturn(DRIVE_PAGE_URL);
    IdOnlyContainerResource idOnlyContainerResource = new IdOnlyContainerResource(FOLDER_ID);

    // Run
    ExportResult<MediaContainerResource> result = microsoftMediaExporter.exportOneDrivePhotos(
        null, Optional.of(idOnlyContainerResource), Optional.empty(), uuid);

    // Verify method calls
    verify(mediaInterface).getDriveItems(Optional.of(FOLDER_ID), Optional.empty());
    verify(driveItemsResponse).getDriveItems();

    // Verify pagination token is set
    ContinuationData continuationData = result.getContinuationData();
    StringPaginationToken paginationToken =
        (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationToken.getToken()).isEqualTo(DRIVE_TOKEN_PREFIX + DRIVE_PAGE_URL);

    // Verify no albums are exported
    Collection<MediaAlbum> actualAlbums = result.getExportedData().getAlbums();
    assertThat(actualAlbums).isEmpty();

    // Verify one photo (in an album) should be exported
    Collection<PhotoModel> actualPhotos = result.getExportedData().getPhotos();
    assertThat(actualPhotos.size()).isEqualTo(1);
    assertThat(actualPhotos.stream().map(PhotoModel::getFetchableUrl).collect(Collectors.toList()))
        .containsExactly(PHOTO_URI);
    assertThat(actualPhotos.stream().map(PhotoModel::getAlbumId).collect(Collectors.toList()))
        .containsExactly(FOLDER_ID);
    assertThat(actualPhotos.stream().map(PhotoModel::getTitle).collect(Collectors.toList()))
        .containsExactly(PHOTO_FILENAME);

    // Verify one video (in an album) should be exported
    Collection<VideoModel> actualVideos = result.getExportedData().getVideos();
    assertThat(actualVideos.size()).isEqualTo(1);
    assertThat(actualVideos.stream().map(VideoModel::getFetchableUrl).collect(Collectors.toList()))
        .containsExactly(VIDEO_URI);
    assertThat(actualVideos.stream().map(VideoModel::getAlbumId).collect(Collectors.toList()))
        .containsExactly(FOLDER_ID);
    assertThat(actualVideos.stream().map(VideoModel::getName).collect(Collectors.toList()))
        .containsExactly(VIDEO_FILENAME);

    // Verify there are no containers ready for sub-processing
    List<ContainerResource> actualResources = continuationData.getContainerResources();
    assertThat(actualResources).isEmpty();
  }

  @Test
  public void exportMediaWithoutNextPage() throws IOException {
    // Setup
    when(driveItemsResponse.getNextPageLink()).thenReturn(null);
    when(driveItemsResponse.getDriveItems()).thenReturn(new MicrosoftDriveItem[] {
      setUpSinglePhoto(PHOTO_FILENAME, PHOTO_URI, PHOTO_ID),
      setUpSingleVideo(VIDEO_FILENAME, VIDEO_URI, VIDEO_ID)
    });
    when(driveItemsResponse.getNextPageLink()).thenReturn(null);
    StringPaginationToken inputPaginationToken =
        new StringPaginationToken(DRIVE_TOKEN_PREFIX + DRIVE_PAGE_URL);
    IdOnlyContainerResource idOnlyContainerResource = new IdOnlyContainerResource(FOLDER_ID);

    // Run
    ExportResult<MediaContainerResource> result = microsoftMediaExporter.exportOneDrivePhotos(
        null, Optional.of(idOnlyContainerResource), Optional.of(inputPaginationToken), uuid);

    // Verify method calls
    verify(mediaInterface).getDriveItems(Optional.of(FOLDER_ID), Optional.of(DRIVE_PAGE_URL));
    verify(driveItemsResponse).getDriveItems();

    // Verify next pagination token is absent
    ContinuationData continuationData = result.getContinuationData();
    StringPaginationToken paginationToken =
        (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationToken).isEqualTo(null);

    // Verify no albums are exported
    Collection<MediaAlbum> actualAlbums = result.getExportedData().getAlbums();
    assertThat(actualAlbums).isEmpty();

    // Verify one photo (in an album) should be exported
    Collection<PhotoModel> actualPhotos = result.getExportedData().getPhotos();
    assertThat(actualPhotos.stream().map(PhotoModel::getFetchableUrl).collect(Collectors.toList()))
        .containsExactly(PHOTO_URI);
    assertThat(actualPhotos.stream().map(PhotoModel::getAlbumId).collect(Collectors.toList()))
        .containsExactly(FOLDER_ID);
    assertThat(actualPhotos.stream().map(PhotoModel::getTitle).collect(Collectors.toList()))
        .containsExactly(PHOTO_FILENAME);

    // Verify there are no containers ready for sub-processing
    List<ContainerResource> actualResources = continuationData.getContainerResources();
    assertThat(actualResources).isEmpty();
  }

  @Test
  public void exportAlbumAndMediaWithNextPage() throws IOException {
    // Setup
    when(driveItemsResponse.getDriveItems())
        .thenReturn(new MicrosoftDriveItem[] {
          setUpSingleAlbum(),
          setUpSinglePhoto(PHOTO_FILENAME, PHOTO_URI, PHOTO_ID)
        });
    when(driveItemsResponse.getNextPageLink()).thenReturn(DRIVE_PAGE_URL);

    // Run
    ExportResult<MediaContainerResource> result = microsoftMediaExporter.exportOneDrivePhotos(
        null, Optional.empty(), Optional.empty(), uuid);

    // Verify method calls
    verify(mediaInterface)
        .getDriveItemsFromSpecialFolder(MicrosoftSpecialFolder.FolderType.photos);
    verify(driveItemsResponse).getDriveItems();

    // Verify pagination token is set
    ContinuationData continuationData = result.getContinuationData();
    StringPaginationToken paginationToken =
        (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationToken.getToken()).isEqualTo(DRIVE_TOKEN_PREFIX + DRIVE_PAGE_URL);

    // Verify one album is ready for import
    Collection<MediaAlbum> actualAlbums = result.getExportedData().getAlbums();
    assertThat(actualAlbums.stream().map(MediaAlbum::getId).collect(Collectors.toList()))
        .containsExactly(FOLDER_ID);

    // Verify one photo should be present (in the root Photos special folder)
    Collection<PhotoModel> actualPhotos = result.getExportedData().getPhotos();
    assertThat(actualPhotos.stream().map(PhotoModel::getFetchableUrl).collect(Collectors.toList()))
        .containsExactly(PHOTO_URI);
    assertThat(actualPhotos.stream().map(PhotoModel::getAlbumId).collect(Collectors.toList()))
        .containsExactly(null);
    assertThat(actualPhotos.stream().map(PhotoModel::getTitle).collect(Collectors.toList()))
        .containsExactly(PHOTO_FILENAME);

    // Verify there is one container ready for sub-processing
    List<ContainerResource> actualResources = continuationData.getContainerResources();
    assertThat(actualResources.stream()
                   .map(a -> ((IdOnlyContainerResource) a).getId())
                   .collect(Collectors.toList()))
        .containsExactly(FOLDER_ID);
  }

  /** Sets up a response with a single album, containing photos and/or videos. */
  private MicrosoftDriveItem setUpSingleAlbum() {
    MicrosoftDriveItem albumEntry = new MicrosoftDriveItem();
    albumEntry.id = FOLDER_ID;
    albumEntry.name = "Title";
    albumEntry.folder = new MicrosoftDriveFolder();
    // TODO(zacsh) remove this childCount setting (or better: set it to a non-sensical value like
    // zero). We clearly don't care about this (our tests don't break if this is incorrect) so even
    // though the upstream APIs provide this, we shouldn't be writing test-doubles that imply that
    // we adhere to this API in anyway.
    albumEntry.folder.childCount = 1;

    return albumEntry;
  }

  /** Sets up a response for a single file. */
  private MicrosoftDriveItem setupSingleFile(String fileName, String downloadUrl, String fileId, String mimeType) {
    MicrosoftDriveItem photoEntry = new MicrosoftDriveItem();
    photoEntry.description = String.format("Description of %s", fileId);
    photoEntry.file = new MicrosoftFileMetadata();
    photoEntry.file.mimeType = mimeType;
    photoEntry.name = fileName;
    photoEntry.id = fileId;
    photoEntry.downloadUrl = downloadUrl;
    photoEntry.photo = new MicrosoftPhotoMetadata();
    return photoEntry;
  }

  /** Sets up a response for a single photo. */
  private MicrosoftDriveItem setUpSinglePhoto(String fileName, String imageUri, String photoId) {
    MicrosoftDriveItem driveItem = setupSingleFile(fileName, imageUri, photoId, "image/jpeg");
    driveItem.photo = new MicrosoftPhotoMetadata();
    return driveItem;
  }

  /** Sets up a response for a single video. */
  private MicrosoftDriveItem setUpSingleVideo(String fileName, String imageUri, String videoId) {
    MicrosoftDriveItem driveItem = setupSingleFile(fileName, imageUri, videoId, "video/ogg");
    driveItem.video = new MicrosoftVideoMetadata();
    return driveItem;
  }
}
