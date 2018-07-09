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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.gdata.util.ServiceException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.photos.model.AlbumListResponse;
import org.datatransferproject.datatransfer.google.photos.model.GoogleAlbum;
import org.datatransferproject.datatransfer.google.photos.model.GoogleMediaItem;
import org.datatransferproject.datatransfer.google.photos.model.MediaItemSearchResponse;
import org.datatransferproject.datatransfer.google.photos.model.MediaMetadata;
import org.datatransferproject.datatransfer.google.photos.model.Photo;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.spi.transfer.types.ExportInformation;
import org.datatransferproject.spi.transfer.types.IdOnlyContainerResource;
import org.datatransferproject.spi.transfer.types.StringPaginationToken;
import org.datatransferproject.types.transfer.models.ContainerResource;
import org.datatransferproject.types.transfer.models.photos.PhotoAlbum;
import org.datatransferproject.types.transfer.models.photos.PhotoModel;
import org.datatransferproject.types.transfer.models.photos.PhotosContainerResource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

public class GooglePhotosExporterTest {

  private String PHOTO_TITLE = "Google photo title";
  private String IMG_URI = "image uri";
  private String JPEG_MEDIA_TYPE = "image/jpeg";
  private String ALBUM_ID = "GoogleAlbum id";
  private String ALBUM_TOKEN = "album_token";
  private String PHOTO_TOKEN = "photo_token";

  private UUID uuid = UUID.randomUUID();

  private GooglePhotosExporter googlePhotosExporter;

  private GoogleCredentialFactory credentialFactory;
  private GooglePhotosInterface photosInterface;

  private AlbumListResponse albumListResponse;
  private MediaItemSearchResponse mediaItemSearchResponse;

  @Before
  public void setup() throws IOException, ServiceException {
    credentialFactory = mock(GoogleCredentialFactory.class);
    photosInterface = mock(GooglePhotosInterface.class);

    albumListResponse = mock(AlbumListResponse.class);
    mediaItemSearchResponse = mock(MediaItemSearchResponse.class);

    googlePhotosExporter =
        new GooglePhotosExporter(credentialFactory, photosInterface);

    when(photosInterface.listAlbums(Matchers.any(Optional.class)))
        .thenReturn(albumListResponse);
    when(photosInterface.listAlbumContents(Matchers.any(Optional.class), Matchers.any(Optional.class)))
        .thenReturn(mediaItemSearchResponse);

    verifyZeroInteractions(credentialFactory);
  }

  @Test
  public void exportAlbumFirstSet() throws IOException, ServiceException {
    setUpSingleAlbumResponse();

    // Run test
    ExportResult<PhotosContainerResource> result = googlePhotosExporter
        .export(uuid, null, Optional.empty());

    // Check results
    // Verify correct methods were called
    verify(photosInterface).listAlbums(Optional.empty());
    verify(albumListResponse).getAlbums();

    // Check pagination token
    ContinuationData continuationData = (ContinuationData) result.getContinuationData();
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
        actualResources
            .stream()
            .map(a -> ((IdOnlyContainerResource) a).getId())
            .collect(Collectors.toList()))
        .containsExactly(ALBUM_ID, GooglePhotosExporter.DEFAULT_ALBUM_ID);
  }

  @Test
  public void exportAlbumSubsequentSet() throws IOException, ServiceException {
    setUpSingleAlbumResponse();
    StringPaginationToken inputPaginationToken =
        new StringPaginationToken(ALBUM_TOKEN_PREFIX + ALBUM_TOKEN);
    ExportInformation inputExportInformation = new ExportInformation(inputPaginationToken, null);

    // Run test
    ExportResult<PhotosContainerResource> result =
        googlePhotosExporter.export(uuid, null, Optional.of(inputExportInformation));

    // Check results
    // Verify correct methods were called
    verify(photosInterface).listAlbums(Optional.of(ALBUM_TOKEN));
    verify(albumListResponse).getAlbums();

    // Check pagination token
    ContinuationData continuationData = (ContinuationData) result.getContinuationData();
    StringPaginationToken paginationToken =
        (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationToken.getToken()).isEqualTo(ALBUM_TOKEN_PREFIX + ALBUM_TOKEN);
  }

  @Test
  public void exportPhotoFirstSet() throws IOException, ServiceException {
    setUpSinglePhotoResponse();

    ContainerResource inputContainerResource = new IdOnlyContainerResource(ALBUM_ID);
    ExportInformation inputExportInformation = new ExportInformation(null, inputContainerResource);

    ExportResult<PhotosContainerResource> result =
        googlePhotosExporter.export(uuid, null, Optional.of(inputExportInformation));

    // Check results
    // Verify correct methods were called
    verify(photosInterface).listAlbumContents(Optional.of(ALBUM_ID), Optional.empty());
    verify(mediaItemSearchResponse).getMediaItems();

    // Check pagination
    ContinuationData continuationData = (ContinuationData) result.getContinuationData();
    StringPaginationToken paginationToken =
        (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationToken.getToken()).isEqualTo(PHOTO_TOKEN_PREFIX + PHOTO_TOKEN);

    // Check albums field of container (should be empty)
    Collection<PhotoAlbum> actualAlbums = result.getExportedData().getAlbums();
    assertThat(actualAlbums).isEmpty();

    // Check photos field of container
    Collection<PhotoModel> actualPhotos = result.getExportedData().getPhotos();
    assertThat(actualPhotos.stream().map(PhotoModel::getFetchableUrl).collect(Collectors.toList()))
        .containsExactly(IMG_URI);
    assertThat(actualPhotos.stream().map(PhotoModel::getAlbumId).collect(Collectors.toList()))
        .containsExactly(ALBUM_ID);
  }

  @Test
  public void exportPhotoSubsequentSet() throws IOException, ServiceException {
    setUpSinglePhotoResponse();

    StringPaginationToken inputPaginationToken =
        new StringPaginationToken(PHOTO_TOKEN_PREFIX + PHOTO_TOKEN);
    ContainerResource inputContainerResource = new IdOnlyContainerResource(ALBUM_ID);
    ExportInformation inputExportInformation =
        new ExportInformation(inputPaginationToken, inputContainerResource);

    // Run test
    ExportResult<PhotosContainerResource> result =
        googlePhotosExporter.export(uuid, null, Optional.of(inputExportInformation));

    // Check results
    // Verify correct methods were called
    verify(photosInterface).listAlbumContents(Optional.of(ALBUM_ID), Optional.of(PHOTO_TOKEN));
    verify(mediaItemSearchResponse).getMediaItems();

    // Check pagination token
    ContinuationData continuationData = (ContinuationData) result.getContinuationData();
    StringPaginationToken paginationToken =
        (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationToken.getToken()).isEqualTo(PHOTO_TOKEN_PREFIX + PHOTO_TOKEN);
  }

  /**
   * Sets up a response with a single album, containing a single photo
   */
  private void setUpSingleAlbumResponse() {
    GoogleAlbum albumEntry = new GoogleAlbum();
    albumEntry.setId(ALBUM_ID);
    albumEntry.setTitle("Title");

    when(albumListResponse.getAlbums()).thenReturn(new GoogleAlbum[]{albumEntry});
    when(albumListResponse.getNextPageToken()).thenReturn(ALBUM_TOKEN);

    setUpSinglePhotoResponse();
  }

  /**
   * Sets up a response for a single photo
   */
  private void setUpSinglePhotoResponse() {
    GoogleMediaItem photoEntry = new GoogleMediaItem();
    photoEntry.setDescription("Description");
    photoEntry.setMimeType(JPEG_MEDIA_TYPE);
    photoEntry.setBaseUrl(IMG_URI);
    MediaMetadata mediaMetadata = new MediaMetadata();
    mediaMetadata.setPhoto(new Photo());
    photoEntry.setMediaMetadata(mediaMetadata);

    when(mediaItemSearchResponse.getMediaItems()).thenReturn(new GoogleMediaItem[]{photoEntry});
    when(mediaItemSearchResponse.getNextPageToken()).thenReturn(PHOTO_TOKEN);
  }
}
