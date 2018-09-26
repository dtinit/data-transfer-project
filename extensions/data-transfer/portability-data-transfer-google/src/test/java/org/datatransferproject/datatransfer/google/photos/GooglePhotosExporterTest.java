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
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.api.client.json.jackson2.JacksonFactory;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.datatransferproject.cloud.local.LocalJobStore;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.photos.model.GoogleAlbumListResponse;
import org.datatransferproject.datatransfer.google.photos.model.GoogleAlbum;
import org.datatransferproject.datatransfer.google.photos.model.GoogleMediaItem;
import org.datatransferproject.datatransfer.google.photos.model.MediaItemListResponse;
import org.datatransferproject.datatransfer.google.photos.model.MediaMetadata;
import org.datatransferproject.datatransfer.google.photos.model.Photo;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.spi.transfer.types.IdOnlyContainerResource;
import org.datatransferproject.spi.transfer.types.PaginationData;
import org.datatransferproject.spi.transfer.types.StringPaginationToken;
import org.datatransferproject.types.transfer.models.ContainerResource;
import org.datatransferproject.types.transfer.models.photos.PhotoAlbum;
import org.datatransferproject.types.transfer.models.photos.PhotoModel;
import org.datatransferproject.types.transfer.models.photos.PhotosContainerResource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

public class GooglePhotosExporterTest {

  private String IMG_URI = "image uri";
  private String ALBUM_ID = "GoogleAlbum id";
  private String ALBUM_TOKEN = "album_token";
  private String PHOTO_TOKEN = "photo_token";

  private UUID uuid = UUID.randomUUID();

  private GooglePhotosExporter googlePhotosExporter;
  private JobStore jobStore;
  private GooglePhotosInterface photosInterface;

  private MediaItemListResponse mediaItemListResponse;
  private GoogleAlbumListResponse googleAlbumListResponse;

  @Before
  public void setup() throws IOException {
    GoogleCredentialFactory credentialFactory = mock(GoogleCredentialFactory.class);
    jobStore = new LocalJobStore();
    photosInterface = mock(GooglePhotosInterface.class);

    googleAlbumListResponse = mock(GoogleAlbumListResponse.class);
    mediaItemListResponse = mock(MediaItemListResponse.class);

    googlePhotosExporter =
        new GooglePhotosExporter(credentialFactory, jobStore, new JacksonFactory(), photosInterface);

    when(photosInterface.listAlbums(Matchers.any(Optional.class)))
        .thenReturn(googleAlbumListResponse);
    when(photosInterface.listAlbumContents(Matchers.any(Optional.class), Matchers.any(Optional.class)))
        .thenReturn(mediaItemListResponse);

    verifyZeroInteractions(credentialFactory);
  }

  @Test
  public void exportAlbumFirstSet() throws IOException {
    setUpSingleAlbum();
    when(googleAlbumListResponse.getNextPageToken()).thenReturn(ALBUM_TOKEN);

    // Run test
    ExportResult<PhotosContainerResource> result = googlePhotosExporter
        .exportAlbums(null, Optional.empty());

    // Check results
    // Verify correct methods were called
    verify(photosInterface).listAlbums(Optional.empty());
    verify(googleAlbumListResponse).getAlbums();

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
        actualResources
            .stream()
            .map(a -> ((IdOnlyContainerResource) a).getId())
            .collect(Collectors.toList()))
        .containsExactly(ALBUM_ID);
  }

  @Test
  public void exportAlbumSubsequentSet() throws IOException {
    setUpSingleAlbum();
    when(googleAlbumListResponse.getNextPageToken()).thenReturn(null);

    StringPaginationToken inputPaginationToken =
        new StringPaginationToken(ALBUM_TOKEN_PREFIX + ALBUM_TOKEN);

    // Run test
    ExportResult<PhotosContainerResource> result = googlePhotosExporter
        .exportAlbums(null, Optional.of(inputPaginationToken));

    // Check results
    // Verify correct methods were called
    verify(photosInterface).listAlbums(Optional.of(ALBUM_TOKEN));
    verify(googleAlbumListResponse).getAlbums();

    // Check pagination token - should be absent
    ContinuationData continuationData = result.getContinuationData();
    PaginationData paginationData = continuationData.getPaginationData();
    assertNull(paginationData);
  }

  @Test
  public void exportPhotoFirstSet() throws IOException {
    setUpSingleAlbum();
    when(googleAlbumListResponse.getNextPageToken()).thenReturn(null);
    GoogleMediaItem mediaItem = setUpSinglePhoto(IMG_URI, ALBUM_ID);
    when(mediaItemListResponse.getMediaItems()).thenReturn(new GoogleMediaItem[]{mediaItem});
    when(mediaItemListResponse.getNextPageToken()).thenReturn(PHOTO_TOKEN);

    ExportResult<PhotosContainerResource> result =
        googlePhotosExporter.exportPhotos(null, ALBUM_ID, Optional.empty());

    // Check results
    // Verify correct methods were called
    verify(photosInterface).listAlbumContents(Optional.of(ALBUM_ID), Optional.empty());
    verify(mediaItemListResponse).getMediaItems();

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
  }

  @Test
  public void exportPhotoSubsequentSet() throws IOException {
    setUpSingleAlbum();
    when(googleAlbumListResponse.getNextPageToken()).thenReturn(null);
    GoogleMediaItem mediaItem = setUpSinglePhoto(IMG_URI, ALBUM_ID);
    when(mediaItemListResponse.getMediaItems()).thenReturn(new GoogleMediaItem[]{mediaItem});
    when(mediaItemListResponse.getNextPageToken()).thenReturn(null);

    StringPaginationToken inputPaginationToken =
        new StringPaginationToken(PHOTO_TOKEN_PREFIX + PHOTO_TOKEN);

    // Run test
    ExportResult<PhotosContainerResource> result = googlePhotosExporter
        .exportPhotos(null, ALBUM_ID, Optional.of(inputPaginationToken));

    // Check results
    // Verify correct methods were called
    verify(photosInterface).listAlbumContents(Optional.of(ALBUM_ID), Optional.of(PHOTO_TOKEN));
    verify(mediaItemListResponse).getMediaItems();

    // Check pagination token
    ContinuationData continuationData = result.getContinuationData();
    PaginationData paginationToken = continuationData.getPaginationData();
    assertNull(paginationToken);
  }

  @Test
  public void exportAlbumLessPhoto() throws IOException {
    // Set up an album with a photo, and then an albumless photo
    setUpSingleAlbum();
    when(googleAlbumListResponse.getNextPageToken()).thenReturn(null);

    MediaItemListResponse albumMediaResponse = mock(MediaItemListResponse.class);
    GoogleMediaItem albumPhoto = setUpSinglePhoto(IMG_URI, ALBUM_ID);
    when(photosInterface
        .listAlbumContents(Matchers.eq(Optional.of(ALBUM_ID)), Matchers.any(Optional.class)))
        .thenReturn(albumMediaResponse);
    when(albumMediaResponse.getMediaItems()).thenReturn(new GoogleMediaItem[]{albumPhoto});
    when(albumMediaResponse.getNextPageToken()).thenReturn(null);

    MediaItemListResponse albumlessMediaResponse = mock(MediaItemListResponse.class);
    String albumlessUri = "albumlessUri";
    GoogleMediaItem albumlessPhoto = setUpSinglePhoto(albumlessUri, null);
    when(photosInterface
        .listAllMediaItems(Matchers.any(Optional.class)))
        .thenReturn(albumlessMediaResponse);
    when(albumlessMediaResponse.getMediaItems()).thenReturn(new GoogleMediaItem[]{albumlessPhoto});
    when(albumlessMediaResponse.getNextPageToken()).thenReturn(null);

    // Run test
    ExportResult<PhotosContainerResource> result =
        googlePhotosExporter.export(uuid, null, Optional.empty());

    // Check result
    assertThat(result.getExportedData()).isNotNull();
    PhotosContainerResource exportedResource = result.getExportedData();

    // Verify correct methods were called
    verify(photosInterface).listAlbumContents(Optional.of(ALBUM_ID), Optional.empty());
    verify(photosInterface).listAllMediaItems(Optional.empty());

    // Check photo content of exportResult
    assertThat(exportedResource.getPhotos()).hasSize(1); // one photo, hopefully albumless
    PhotoModel exportedPhoto = exportedResource.getPhotos().iterator().next();
    assertThat(exportedPhoto.getFetchableUrl()).isEqualTo(albumlessUri + "=d"); // for download

    // Check album content of exportResult
    assertThat(exportedResource.getAlbums()).hasSize(1); // one album
    PhotoAlbum exportedAlbum = exportedResource.getAlbums().iterator().next();
    assertThat(exportedAlbum.getId()).isEqualTo(ALBUM_ID);
  }

  @Test
  public void addPhotosToExportResult() {
    // Set up
    String albumId = "albumId";
    String containerPhotoId = "containerPhotoId";
    String additionalPhotoId = "additionalPhotoId";
    PhotoAlbum album = new PhotoAlbum(albumId, null, null);
    PhotoModel containerPhoto = new PhotoModel(containerPhotoId, null, null, null, null, albumId,
        false);
    PhotoModel additionalPhoto = new PhotoModel(additionalPhotoId, null, null, null, null, null,
        false);

    ResultType resultType = ResultType.CONTINUE;
    ContinuationData continuationData = new ContinuationData(
        new StringPaginationToken("token"));

    ExportResult<PhotosContainerResource> originalExportResult = new ExportResult<>(
        resultType,
        new PhotosContainerResource(Collections.singleton(album),
            Collections.singleton(containerPhoto)),
        continuationData);
    List<PhotoModel> additionalPhotos = Collections.singletonList(additionalPhoto);

    // Run test
    ExportResult<PhotosContainerResource> newExportResult = googlePhotosExporter
        .addMorePhotos(originalExportResult, additionalPhotos);

    // Check results
    assertThat(newExportResult.getType()).isEqualTo(resultType);
    assertThat(newExportResult.getContinuationData()).isEqualTo(continuationData);

    PhotosContainerResource newResource = newExportResult.getExportedData();
    assertThat(newResource.getAlbums()).hasSize(1);
    assertThat(newResource.getAlbums().iterator().next()).isEqualTo(album);
    assertThat(newResource.getPhotos()).hasSize(2);
    assertThat(newResource.getPhotos()).containsExactly(containerPhoto, additionalPhoto);
  }

  /**
   * Sets up a response with a single album, containing a single photo
   */
  private void setUpSingleAlbum() {
    GoogleAlbum albumEntry = new GoogleAlbum();
    albumEntry.setId(ALBUM_ID);
    albumEntry.setTitle("Title");

    when(googleAlbumListResponse.getAlbums()).thenReturn(new GoogleAlbum[]{albumEntry});
  }

  /**
   * Sets up a response for a single photo
   */
  private GoogleMediaItem setUpSinglePhoto(String imageUri, @Nullable String photoToken) {
    GoogleMediaItem photoEntry = new GoogleMediaItem();
    photoEntry.setDescription("Description");
    photoEntry.setMimeType("image/jpeg");
    photoEntry.setBaseUrl(imageUri);
    MediaMetadata mediaMetadata = new MediaMetadata();
    mediaMetadata.setPhoto(new Photo());
    photoEntry.setMediaMetadata(mediaMetadata);

    return photoEntry;
  }

}
