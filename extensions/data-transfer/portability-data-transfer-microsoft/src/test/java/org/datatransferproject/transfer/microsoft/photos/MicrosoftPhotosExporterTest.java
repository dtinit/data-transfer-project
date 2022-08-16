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

package org.datatransferproject.transfer.microsoft.photos;

import com.google.api.client.json.gson.GsonFactory;

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
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.datatransferproject.transfer.microsoft.photos.MicrosoftPhotosExporter.DRIVE_TOKEN_PREFIX;

/** */
public class MicrosoftPhotosExporterTest {

  private String IMAGE_URI = "imageDownloadUri";
  private String PHOTO_ID = "uniquePhotoId";
  private String FOLDER_ID = "microsoftOneDriveFolderId";
  private String DRIVE_PAGE_URL = "driveToken";
  private String FILENAME = "filename";
  private UUID uuid = UUID.randomUUID();

  private TokensAndUrlAuthData token;
  private MicrosoftPhotosExporter microsoftPhotosExporter;
  private MicrosoftPhotosInterface photosInterface;

  private MicrosoftDriveItemsResponse driveItemsResponse;

  @BeforeEach
  public void setUp() throws IOException {
    MicrosoftCredentialFactory credentialFactory = mock(MicrosoftCredentialFactory.class);
    photosInterface = mock(MicrosoftPhotosInterface.class);
    driveItemsResponse = mock(MicrosoftDriveItemsResponse.class);
    Monitor monitor = mock(Monitor.class);

    microsoftPhotosExporter =
        new MicrosoftPhotosExporter(
            credentialFactory, GsonFactory.getDefaultInstance(), photosInterface, monitor);

    when(photosInterface.getDriveItems(any(Optional.class), any(Optional.class)))
        .thenReturn(driveItemsResponse);
    when(photosInterface.getDriveItemsFromSpecialFolder(
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
    ExportResult<PhotosContainerResource> result =
        microsoftPhotosExporter.exportOneDrivePhotos(
            null, Optional.empty(), Optional.empty(), uuid);

    // Verify method calls
    verify(photosInterface)
        .getDriveItemsFromSpecialFolder(MicrosoftSpecialFolder.FolderType.photos);
    verify(driveItemsResponse).getDriveItems();

    // Verify pagination token is set
    ContinuationData continuationData = result.getContinuationData();
    StringPaginationToken paginationToken =
        (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationToken.getToken()).isEqualTo(DRIVE_TOKEN_PREFIX + DRIVE_PAGE_URL);

    // Verify one album is ready for import
    Collection<PhotoAlbum> actualAlbums = result.getExportedData().getAlbums();
    assertThat(actualAlbums.stream().map(PhotoAlbum::getId).collect(Collectors.toList()))
        .containsExactly(FOLDER_ID);

    // Verify photos should be empty (in the root)
    Collection<PhotoModel> actualPhotos = result.getExportedData().getPhotos();
    assertThat(actualPhotos).isEmpty();

    // Verify there is one container ready for sub-processing
    List<ContainerResource> actualResources = continuationData.getContainerResources();
    assertThat(
            actualResources.stream()
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
    ExportResult<PhotosContainerResource> result =
        microsoftPhotosExporter.exportOneDrivePhotos(
            null, Optional.empty(), Optional.of(inputPaginationToken), uuid);

    // Verify method calls
    verify(photosInterface).getDriveItems(Optional.empty(), Optional.of(DRIVE_PAGE_URL));
    verify(driveItemsResponse).getDriveItems();

    // Verify next pagination token is absent
    ContinuationData continuationData = result.getContinuationData();
    StringPaginationToken paginationToken =
        (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationToken).isEqualTo(null);

    // Verify one album is ready for import
    Collection<PhotoAlbum> actualAlbums = result.getExportedData().getAlbums();
    assertThat(actualAlbums.stream().map(PhotoAlbum::getId).collect(Collectors.toList()))
        .containsExactly(FOLDER_ID);

    // Verify photos should be empty (in the root)
    Collection<PhotoModel> actualPhotos = result.getExportedData().getPhotos();
    assertThat(actualPhotos).isEmpty();

    // Verify there is one container ready for sub-processing
    List<ContainerResource> actualResources = continuationData.getContainerResources();
    assertThat(
            actualResources.stream()
                .map(a -> ((IdOnlyContainerResource) a).getId())
                .collect(Collectors.toList()))
        .containsExactly(FOLDER_ID);
  }

  @Test
  public void exportPhotoWithNextPage() throws IOException {

    // Setup
    when(driveItemsResponse.getNextPageLink()).thenReturn(null);
    MicrosoftDriveItem photoItem = setUpSinglePhoto(IMAGE_URI, PHOTO_ID);
    when(driveItemsResponse.getDriveItems()).thenReturn(new MicrosoftDriveItem[] {photoItem});
    when(driveItemsResponse.getNextPageLink()).thenReturn(DRIVE_PAGE_URL);
    IdOnlyContainerResource idOnlyContainerResource = new IdOnlyContainerResource(FOLDER_ID);

    // Run
    ExportResult<PhotosContainerResource> result =
        microsoftPhotosExporter.exportOneDrivePhotos(
            null, Optional.of(idOnlyContainerResource), Optional.empty(), uuid);

    // Verify method calls
    verify(photosInterface).getDriveItems(Optional.of(FOLDER_ID), Optional.empty());
    verify(driveItemsResponse).getDriveItems();

    // Verify pagination token is set
    ContinuationData continuationData = result.getContinuationData();
    StringPaginationToken paginationToken =
        (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationToken.getToken()).isEqualTo(DRIVE_TOKEN_PREFIX + DRIVE_PAGE_URL);

    // Verify no albums are exported
    Collection<PhotoAlbum> actualAlbums = result.getExportedData().getAlbums();
    assertThat(actualAlbums).isEmpty();

    // Verify one photo (in an album) should be exported
    Collection<PhotoModel> actualPhotos = result.getExportedData().getPhotos();
    assertThat(actualPhotos.stream().map(PhotoModel::getFetchableUrl).collect(Collectors.toList()))
        .containsExactly(IMAGE_URI);
    assertThat(actualPhotos.stream().map(PhotoModel::getAlbumId).collect(Collectors.toList()))
        .containsExactly(FOLDER_ID);
    assertThat(actualPhotos.stream().map(PhotoModel::getTitle).collect(Collectors.toList()))
        .containsExactly(FILENAME);

    // Verify there are no containers ready for sub-processing
    List<ContainerResource> actualResources = continuationData.getContainerResources();
    assertThat(actualResources).isEmpty();
  }

  @Test
  public void exportPhotoWithoutNextPage() throws IOException {

    // Setup
    when(driveItemsResponse.getNextPageLink()).thenReturn(null);
    MicrosoftDriveItem photoItem = setUpSinglePhoto(IMAGE_URI, PHOTO_ID);
    when(driveItemsResponse.getDriveItems()).thenReturn(new MicrosoftDriveItem[] {photoItem});
    when(driveItemsResponse.getNextPageLink()).thenReturn(null);
    StringPaginationToken inputPaginationToken =
        new StringPaginationToken(DRIVE_TOKEN_PREFIX + DRIVE_PAGE_URL);
    IdOnlyContainerResource idOnlyContainerResource = new IdOnlyContainerResource(FOLDER_ID);

    // Run
    ExportResult<PhotosContainerResource> result =
        microsoftPhotosExporter.exportOneDrivePhotos(
            null, Optional.of(idOnlyContainerResource), Optional.of(inputPaginationToken), uuid);

    // Verify method calls
    verify(photosInterface).getDriveItems(Optional.of(FOLDER_ID), Optional.of(DRIVE_PAGE_URL));
    verify(driveItemsResponse).getDriveItems();

    // Verify next pagination token is absent
    ContinuationData continuationData = result.getContinuationData();
    StringPaginationToken paginationToken =
        (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationToken).isEqualTo(null);

    // Verify no albums are exported
    Collection<PhotoAlbum> actualAlbums = result.getExportedData().getAlbums();
    assertThat(actualAlbums).isEmpty();

    // Verify one photo (in an album) should be exported
    Collection<PhotoModel> actualPhotos = result.getExportedData().getPhotos();
    assertThat(actualPhotos.stream().map(PhotoModel::getFetchableUrl).collect(Collectors.toList()))
        .containsExactly(IMAGE_URI);
    assertThat(actualPhotos.stream().map(PhotoModel::getAlbumId).collect(Collectors.toList()))
        .containsExactly(FOLDER_ID);
    assertThat(actualPhotos.stream().map(PhotoModel::getTitle).collect(Collectors.toList()))
        .containsExactly(FILENAME);

    // Verify there are no containers ready for sub-processing
    List<ContainerResource> actualResources = continuationData.getContainerResources();
    assertThat(actualResources).isEmpty();
  }

  @Test
  public void exportAlbumAndPhotoWithNextPage() throws IOException {

    // Setup
    MicrosoftDriveItem folderItem = setUpSingleAlbum();
    MicrosoftDriveItem photoItem = setUpSinglePhoto(IMAGE_URI, PHOTO_ID);
    when(driveItemsResponse.getDriveItems())
        .thenReturn(new MicrosoftDriveItem[] {folderItem, photoItem});
    when(driveItemsResponse.getNextPageLink()).thenReturn(DRIVE_PAGE_URL);

    // Run
    ExportResult<PhotosContainerResource> result =
        microsoftPhotosExporter.exportOneDrivePhotos(
            null, Optional.empty(), Optional.empty(), uuid);

    // Verify method calls
    verify(photosInterface)
        .getDriveItemsFromSpecialFolder(MicrosoftSpecialFolder.FolderType.photos);
    verify(driveItemsResponse).getDriveItems();

    // Verify pagination token is set
    ContinuationData continuationData = result.getContinuationData();
    StringPaginationToken paginationToken =
        (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationToken.getToken()).isEqualTo(DRIVE_TOKEN_PREFIX + DRIVE_PAGE_URL);

    // Verify one album is ready for import
    Collection<PhotoAlbum> actualAlbums = result.getExportedData().getAlbums();
    assertThat(actualAlbums.stream().map(PhotoAlbum::getId).collect(Collectors.toList()))
        .containsExactly(FOLDER_ID);

    // Verify one photo should be present (in the root Photos special folder)
    Collection<PhotoModel> actualPhotos = result.getExportedData().getPhotos();
    assertThat(actualPhotos.stream().map(PhotoModel::getFetchableUrl).collect(Collectors.toList()))
        .containsExactly(IMAGE_URI);
    assertThat(actualPhotos.stream().map(PhotoModel::getAlbumId).collect(Collectors.toList()))
        .containsExactly(null);
    assertThat(actualPhotos.stream().map(PhotoModel::getTitle).collect(Collectors.toList()))
        .containsExactly(FILENAME);

    // Verify there is one container ready for sub-processing
    List<ContainerResource> actualResources = continuationData.getContainerResources();
    assertThat(
            actualResources.stream()
                .map(a -> ((IdOnlyContainerResource) a).getId())
                .collect(Collectors.toList()))
        .containsExactly(FOLDER_ID);
  }

  /** Sets up a response with a single album, containing a single photo */
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

  /** Sets up a response for a single photo */
  private MicrosoftDriveItem setUpSinglePhoto(String imageUri, String photoId) {
    MicrosoftDriveItem photoEntry = new MicrosoftDriveItem();
    photoEntry.description = "Description";
    photoEntry.file = new MicrosoftFileMetadata();
    photoEntry.file.mimeType = "image/jpeg";
    photoEntry.name = FILENAME;
    photoEntry.id = photoId;
    photoEntry.downloadUrl = imageUri;
    photoEntry.photo = new MicrosoftPhotoMetadata();

    return photoEntry;
  }
}
