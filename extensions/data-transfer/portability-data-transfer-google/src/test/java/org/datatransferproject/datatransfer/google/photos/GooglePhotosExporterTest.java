/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.datatransferproject.datatransfer.google.photos;

import static com.google.common.truth.Truth.assertThat;
import static org.datatransferproject.datatransfer.google.photos.GooglePhotosExporter.ALBUM_TOKEN_PREFIX;
import static org.datatransferproject.datatransfer.google.photos.GooglePhotosExporter.PHOTO_TOKEN_PREFIX;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.json.gson.GsonFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.mediaModels.AlbumListResponse;
import org.datatransferproject.datatransfer.google.mediaModels.GoogleAlbum;
import org.datatransferproject.datatransfer.google.mediaModels.GoogleMediaItem;
import org.datatransferproject.datatransfer.google.mediaModels.MediaItemSearchResponse;
import org.datatransferproject.datatransfer.google.mediaModels.MediaMetadata;
import org.datatransferproject.datatransfer.google.mediaModels.Photo;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore.InputStreamWrapper;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.datatransferproject.spi.transfer.types.TempPhotosData;
import org.datatransferproject.spi.transfer.types.UploadErrorException;
import org.datatransferproject.types.common.PaginationData;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.ContainerResource;
import org.datatransferproject.types.common.models.IdOnlyContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class GooglePhotosExporterTest {

  private String IMG_URI = "image uri";
  private String PHOTO_ID = "photo id";
  private String FILENAME = "filename";
  private String ALBUM_ID = "GoogleAlbum id";
  private String ALBUM_TOKEN = "album_token";
  private String PHOTO_TOKEN = "photo_token";

  private UUID uuid = UUID.randomUUID();

  private GooglePhotosExporter googlePhotosExporter;
  private TemporaryPerJobDataStore jobStore;
  private GooglePhotosInterface photosInterface;

  private MediaItemSearchResponse mediaItemSearchResponse;
  private AlbumListResponse albumListResponse;

  @BeforeEach
  public void setup()
      throws IOException, InvalidTokenException, PermissionDeniedException, UploadErrorException {
    GoogleCredentialFactory credentialFactory = mock(GoogleCredentialFactory.class);
    jobStore = mock(TemporaryPerJobDataStore.class);
    when(jobStore.getStream(any(), anyString())).thenReturn(mock(InputStreamWrapper.class));
    photosInterface = mock(GooglePhotosInterface.class);

    albumListResponse = mock(AlbumListResponse.class);
    mediaItemSearchResponse = mock(MediaItemSearchResponse.class);

    Monitor monitor = mock(Monitor.class);

    googlePhotosExporter =
        new GooglePhotosExporter(
            credentialFactory, jobStore, GsonFactory.getDefaultInstance(), photosInterface, monitor);

    when(photosInterface.listAlbums(any(Optional.class))).thenReturn(albumListResponse);
    when(photosInterface.listMediaItems(any(Optional.class), any(Optional.class)))
        .thenReturn(mediaItemSearchResponse);

    verifyNoInteractions(credentialFactory);
  }

  @Test
  public void exportAlbumFirstSet() throws IOException, InvalidTokenException, PermissionDeniedException {
    setUpSingleAlbum();
    when(albumListResponse.getNextPageToken()).thenReturn(ALBUM_TOKEN);

    // Run test
    ExportResult<PhotosContainerResource> result =
        googlePhotosExporter.exportAlbums(null, Optional.empty(), uuid);

    // Check results
    // Verify correct methods were called
    verify(photosInterface).listAlbums(Optional.empty());
    verify(albumListResponse).getAlbums();

    // Check pagination token
    ContinuationData continuationData = result.getContinuationData();
    StringPaginationToken paginationToken =
        (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationToken.getToken()).isEqualTo(ALBUM_TOKEN_PREFIX + ALBUM_TOKEN);

    // Check albums field of container
    Collection<PhotoAlbum> actualAlbums = result.getExportedData().getAlbums();
    assertThat(actualAlbums.stream().map(PhotoAlbum::getId).collect(Collectors.toList()))
        .containsExactly(ALBUM_ID);

    // Check photos field of container (should be empty, even though there is a photo in the
    // original album)
    Collection<PhotoModel> actualPhotos = result.getExportedData().getPhotos();
    assertThat(actualPhotos).isEmpty();
    // Should be one container in the resource list
    List<ContainerResource> actualResources = continuationData.getContainerResources();
    assertThat(
            actualResources.stream()
                .map(a -> ((IdOnlyContainerResource) a).getId())
                .collect(Collectors.toList()))
        .containsExactly(ALBUM_ID);
  }

  @Test
  public void exportAlbumSubsequentSet() throws IOException, InvalidTokenException, PermissionDeniedException {
    setUpSingleAlbum();
    when(albumListResponse.getNextPageToken()).thenReturn(null);

    StringPaginationToken inputPaginationToken =
        new StringPaginationToken(ALBUM_TOKEN_PREFIX + ALBUM_TOKEN);

    // Run test
    ExportResult<PhotosContainerResource> result =
        googlePhotosExporter.exportAlbums(null, Optional.of(inputPaginationToken), uuid);

    // Check results
    // Verify correct methods were called
    verify(photosInterface).listAlbums(Optional.of(ALBUM_TOKEN));
    verify(albumListResponse).getAlbums();

    // Check pagination token - should be absent
    ContinuationData continuationData = result.getContinuationData();
    StringPaginationToken paginationData =
        (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationData.getToken()).isEqualTo(GooglePhotosExporter.PHOTO_TOKEN_PREFIX);
  }

  @Test
  public void exportPhotoFirstSet()
      throws IOException, InvalidTokenException, PermissionDeniedException, UploadErrorException {
    setUpSingleAlbum();
    when(albumListResponse.getNextPageToken()).thenReturn(null);
    GoogleMediaItem mediaItem = setUpSinglePhoto(IMG_URI, PHOTO_ID);
    when(mediaItemSearchResponse.getMediaItems()).thenReturn(new GoogleMediaItem[] {mediaItem});
    when(mediaItemSearchResponse.getNextPageToken()).thenReturn(PHOTO_TOKEN);

    IdOnlyContainerResource idOnlyContainerResource = new IdOnlyContainerResource(ALBUM_ID);

    ExportResult<PhotosContainerResource> result =
        googlePhotosExporter.exportPhotos(
            null, Optional.of(idOnlyContainerResource), Optional.empty(), uuid);

    // Check results
    // Verify correct methods were called
    verify(photosInterface).listMediaItems(Optional.of(ALBUM_ID), Optional.empty());
    verify(mediaItemSearchResponse).getMediaItems();

    // Check pagination
    ContinuationData continuationData = result.getContinuationData();
    StringPaginationToken paginationToken =
        (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationToken.getToken()).isEqualTo(PHOTO_TOKEN_PREFIX + PHOTO_TOKEN);

    // Check albums field of container (should be empty)
    Collection<PhotoAlbum> actualAlbums = result.getExportedData().getAlbums();
    assertThat(actualAlbums).isEmpty();

    // Check photos field of container
    Collection<PhotoModel> actualPhotos = result.getExportedData().getPhotos();
    assertThat(actualPhotos.stream().map(PhotoModel::getFetchableUrl).collect(Collectors.toList()))
        .containsExactly(IMG_URI + "=d"); // for download
    assertThat(actualPhotos.stream().map(PhotoModel::getAlbumId).collect(Collectors.toList()))
        .containsExactly(ALBUM_ID);
    assertThat(actualPhotos.stream().map(PhotoModel::getTitle).collect(Collectors.toList()))
        .containsExactly(FILENAME);
  }

  @Test
  public void exportPhotoSubsequentSet()
      throws IOException, InvalidTokenException, PermissionDeniedException, UploadErrorException {
    setUpSingleAlbum();
    when(albumListResponse.getNextPageToken()).thenReturn(null);
    GoogleMediaItem mediaItem = setUpSinglePhoto(IMG_URI, PHOTO_ID);
    when(mediaItemSearchResponse.getMediaItems()).thenReturn(new GoogleMediaItem[] {mediaItem});
    when(mediaItemSearchResponse.getNextPageToken()).thenReturn(null);

    StringPaginationToken inputPaginationToken =
        new StringPaginationToken(PHOTO_TOKEN_PREFIX + PHOTO_TOKEN);
    IdOnlyContainerResource idOnlyContainerResource = new IdOnlyContainerResource(ALBUM_ID);

    // Run test
    ExportResult<PhotosContainerResource> result =
        googlePhotosExporter.exportPhotos(
            null, Optional.of(idOnlyContainerResource), Optional.of(inputPaginationToken), uuid);

    // Check results
    // Verify correct methods were called
    verify(photosInterface).listMediaItems(Optional.of(ALBUM_ID), Optional.of(PHOTO_TOKEN));
    verify(mediaItemSearchResponse).getMediaItems();

    // Check pagination token
    ContinuationData continuationData = result.getContinuationData();
    PaginationData paginationToken = continuationData.getPaginationData();
    assertNull(paginationToken);
  }

  @Test
  public void populateContainedPhotosList()
      throws IOException, InvalidTokenException, PermissionDeniedException, UploadErrorException {
    // Set up an album with two photos
    setUpSingleAlbum();
    when(albumListResponse.getNextPageToken()).thenReturn(null);

    MediaItemSearchResponse albumMediaResponse = mock(MediaItemSearchResponse.class);
    GoogleMediaItem firstPhoto = setUpSinglePhoto(IMG_URI, PHOTO_ID);
    String secondUri = "second uri";
    String secondId = "second id";
    GoogleMediaItem secondPhoto = setUpSinglePhoto(secondUri, secondId);

    when(photosInterface.listMediaItems(eq(Optional.of(ALBUM_ID)), any(Optional.class)))
        .thenReturn(albumMediaResponse);
    when(albumMediaResponse.getMediaItems())
        .thenReturn(new GoogleMediaItem[] {firstPhoto, secondPhoto});
    when(albumMediaResponse.getNextPageToken()).thenReturn(null);

    // Run test
    googlePhotosExporter.populateContainedPhotosList(uuid, null);

    // Check contents of job store
    ArgumentCaptor<InputStream> inputStreamArgumentCaptor =
        ArgumentCaptor.forClass(InputStream.class);
    verify(jobStore).create(eq(uuid), eq("tempPhotosData"), inputStreamArgumentCaptor.capture());
    TempPhotosData tempPhotosData =
        new ObjectMapper().readValue(inputStreamArgumentCaptor.getValue(), TempPhotosData.class);
    assertThat(tempPhotosData.lookupContainedPhotoIds()).containsExactly(PHOTO_ID, secondId);
  }

  @Test
  /* Tests that when there is no album information passed along to exportPhotos, only albumless
  photos are exported.
  */
  public void onlyExportAlbumlessPhoto()
      throws IOException, InvalidTokenException, PermissionDeniedException, UploadErrorException {
    // Set up - two photos will be returned by a media item search without an album id, but one of
    // them will have already been put into the list of contained photos
    String containedPhotoUri = "contained photo uri";
    String containedPhotoId = "contained photo id";
    GoogleMediaItem containedPhoto = setUpSinglePhoto(containedPhotoUri, containedPhotoId);
    String albumlessPhotoUri = "albumless photo uri";
    String albumlessPhotoId = "albumless photo id";
    GoogleMediaItem albumlessPhoto = setUpSinglePhoto(albumlessPhotoUri, albumlessPhotoId);
    MediaItemSearchResponse mediaItemSearchResponse = mock(MediaItemSearchResponse.class);

    when(photosInterface.listMediaItems(eq(Optional.empty()), eq(Optional.empty())))
        .thenReturn(mediaItemSearchResponse);
    when(mediaItemSearchResponse.getMediaItems())
        .thenReturn(new GoogleMediaItem[] {containedPhoto, albumlessPhoto});
    when(mediaItemSearchResponse.getNextPageToken()).thenReturn(null);

    TempPhotosData tempPhotosData = new TempPhotosData(uuid);
    tempPhotosData.addContainedPhotoId(containedPhotoId);
    InputStream stream = GooglePhotosExporter.convertJsonToInputStream(tempPhotosData);
    when(jobStore.getStream(uuid, "tempPhotosData")).thenReturn(new InputStreamWrapper(stream));

    // Run test
    ExportResult<PhotosContainerResource> result =
        googlePhotosExporter.exportPhotos(null, Optional.empty(), Optional.empty(), uuid);

    // Check results
    assertThat(
            result.getExportedData().getPhotos().stream()
                .map(PhotoModel::getFetchableUrl)
                .collect(Collectors.toList()))
        .containsExactly(albumlessPhotoUri + "=d"); // download
  }

  /** Sets up a response with a single album, containing a single photo */
  private void setUpSingleAlbum() {
    GoogleAlbum albumEntry = new GoogleAlbum();
    albumEntry.setId(ALBUM_ID);
    albumEntry.setTitle("Title");

    when(albumListResponse.getAlbums()).thenReturn(new GoogleAlbum[] {albumEntry});
  }

  /** Sets up a response for a single photo */
  private GoogleMediaItem setUpSinglePhoto(String imageUri, String photoId) {
    GoogleMediaItem photoEntry = new GoogleMediaItem();
    photoEntry.setDescription("Description");
    photoEntry.setMimeType("image/jpeg");
    photoEntry.setBaseUrl(imageUri);
    photoEntry.setId(photoId);
    photoEntry.setFilename(FILENAME);
    MediaMetadata mediaMetadata = new MediaMetadata();
    mediaMetadata.setPhoto(new Photo());
    photoEntry.setMediaMetadata(mediaMetadata);

    return photoEntry;
  }
}
