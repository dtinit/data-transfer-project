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
package org.datatransferproject.datatransfer.google.media;

import static com.google.common.truth.Truth.assertThat;
import static org.datatransferproject.datatransfer.google.media.GoogleMediaExporter.ALBUM_TOKEN_PREFIX;
import static org.datatransferproject.datatransfer.google.media.GoogleMediaExporter.MEDIA_TOKEN_PREFIX;

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
import org.datatransferproject.datatransfer.google.mediaModels.Video;
import org.datatransferproject.datatransfer.google.photos.GooglePhotosInterface;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore.InputStreamWrapper;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.datatransferproject.spi.transfer.types.TempMediaData;
import org.datatransferproject.spi.transfer.types.UploadErrorException;
import org.datatransferproject.types.common.PaginationData;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.ContainerResource;
import org.datatransferproject.types.common.models.IdOnlyContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.common.models.videos.VideoAlbum;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class GoogleMediaExporterTest {

  private static String FILENAME = "filename";
  private static String ALBUM_ID = "GoogleAlbum id";
  private static String ALBUM_TOKEN = "some-upstream-generated-album-token";
  private static String MEDIA_TOKEN = "some-upstream-generated-media-token";

  private static UUID uuid = UUID.randomUUID();

  private GoogleMediaExporter googleMediaExporter;
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

    googleMediaExporter =
        new GoogleMediaExporter(
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
    ExportResult<MediaContainerResource> result =
        googleMediaExporter.exportAlbums(null /*authData*/, Optional.empty(), uuid);

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
    Collection<MediaAlbum> actualAlbums = result.getExportedData().getAlbums();
    assertThat(actualAlbums.stream().map(MediaAlbum::getId).collect(Collectors.toList()))
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
    ExportResult<MediaContainerResource> result =
        googleMediaExporter.exportAlbums(null /*authData*/, Optional.of(inputPaginationToken), uuid);

    // Check results
    // Verify correct methods were called
    verify(photosInterface).listAlbums(Optional.of(ALBUM_TOKEN));
    verify(albumListResponse).getAlbums();

    // Check pagination token - should be absent
    ContinuationData continuationData = result.getContinuationData();
    StringPaginationToken paginationData =
        (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationData.getToken()).isEqualTo(MEDIA_TOKEN_PREFIX);
  }

  @Test
  public void exportPhotoFirstSet()
      throws IOException, InvalidTokenException, PermissionDeniedException, UploadErrorException {
    setUpSingleAlbum();
    when(albumListResponse.getNextPageToken()).thenReturn(null);
    GoogleMediaItem mediaItem = setUpSinglePhoto("some://fake/gphotoapi/uri", "some-upstream-generated-photo-id");
    when(mediaItemSearchResponse.getMediaItems()).thenReturn(new GoogleMediaItem[] {mediaItem});
    when(mediaItemSearchResponse.getNextPageToken()).thenReturn(MEDIA_TOKEN);

    IdOnlyContainerResource idOnlyContainerResource = new IdOnlyContainerResource(ALBUM_ID);

    ExportResult<MediaContainerResource> result =
        googleMediaExporter.exportMedia(
            null, Optional.of(idOnlyContainerResource), Optional.empty(), uuid);

    // Check results
    // Verify correct methods were called
    verify(photosInterface).listMediaItems(Optional.of(ALBUM_ID), Optional.empty());
    verify(mediaItemSearchResponse).getMediaItems();

    // Check pagination
    ContinuationData continuationData = result.getContinuationData();
    StringPaginationToken paginationToken =
        (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationToken.getToken()).isEqualTo(MEDIA_TOKEN_PREFIX + MEDIA_TOKEN);

    // Check albums field of container (should be empty)
    Collection<MediaAlbum> actualAlbums = result.getExportedData().getAlbums();
    assertThat(actualAlbums).isEmpty();

    // Check photos field of container
    Collection<PhotoModel> actualPhotos = result.getExportedData().getPhotos();
    assertThat(actualPhotos.stream().map(PhotoModel::getFetchableUrl).collect(Collectors.toList()))
        .containsExactly("some://fake/gphotoapi/uri=d"); // for download
    assertThat(actualPhotos.stream().map(PhotoModel::getAlbumId).collect(Collectors.toList()))
        .containsExactly(ALBUM_ID);
    assertThat(actualPhotos.stream().map(PhotoModel::getName).collect(Collectors.toList()))
        .containsExactly(FILENAME);
  }

  @Test
  public void exportVideoFirstSet()
      throws IOException, InvalidTokenException, PermissionDeniedException, UploadErrorException {
    setUpSingleAlbum();
    when(albumListResponse.getNextPageToken()).thenReturn(null);
    GoogleMediaItem mediaItem = setUpSingleVideo(
        "some://fake/gphotoapi/uri", "some-upstream-generated-video-id");
    when(mediaItemSearchResponse.getMediaItems()).thenReturn(new GoogleMediaItem[] {mediaItem});
    when(mediaItemSearchResponse.getNextPageToken()).thenReturn(MEDIA_TOKEN);

    IdOnlyContainerResource idOnlyContainerResource = new IdOnlyContainerResource(ALBUM_ID);

    ExportResult<MediaContainerResource> result =
        googleMediaExporter.exportMedia(
            null, Optional.of(idOnlyContainerResource), Optional.empty(), uuid);

    // Check results
    // Verify correct methods were called
    verify(photosInterface).listMediaItems(Optional.of(ALBUM_ID), Optional.empty());
    verify(mediaItemSearchResponse).getMediaItems();

    // Check pagination
    ContinuationData continuationData = result.getContinuationData();
    StringPaginationToken paginationToken =
        (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationToken.getToken()).isEqualTo(MEDIA_TOKEN_PREFIX + MEDIA_TOKEN);

    // Check albums field of container (should be empty)
    Collection<MediaAlbum> actualAlbums = result.getExportedData().getAlbums();
    assertThat(actualAlbums).isEmpty();

    // Check videos field of container
    Collection<VideoModel> actualVideos = result.getExportedData().getVideos();
    assertThat(actualVideos.stream().map(VideoModel::getFetchableUrl).collect(Collectors.toList()))
        .containsExactly("some://fake/gphotoapi/uri=dv"); // for download
    assertThat(actualVideos.stream().map(VideoModel::getAlbumId).collect(Collectors.toList()))
        .containsExactly(ALBUM_ID);
    assertThat(actualVideos.stream().map(VideoModel::getName).collect(Collectors.toList()))
        .containsExactly(FILENAME);
  }

  @Test
  public void exportPhotoSubsequentSet()
      throws IOException, InvalidTokenException, PermissionDeniedException, UploadErrorException {
    setUpSingleAlbum();
    when(albumListResponse.getNextPageToken()).thenReturn(null);
    GoogleMediaItem mediaItem = setUpSinglePhoto(
        "some://fake/gphotoapi/uri", "some-upstream-generated-photo-id");
    when(mediaItemSearchResponse.getMediaItems()).thenReturn(new GoogleMediaItem[] {mediaItem});
    when(mediaItemSearchResponse.getNextPageToken()).thenReturn(null);

    StringPaginationToken inputPaginationToken =
        new StringPaginationToken(MEDIA_TOKEN_PREFIX + MEDIA_TOKEN);
    IdOnlyContainerResource idOnlyContainerResource = new IdOnlyContainerResource(ALBUM_ID);

    // Run test
    ExportResult<MediaContainerResource> result =
        googleMediaExporter.exportMedia(
            null, Optional.of(idOnlyContainerResource), Optional.of(inputPaginationToken), uuid);

    // Check results
    // Verify correct methods were called
    verify(photosInterface).listMediaItems(Optional.of(ALBUM_ID), Optional.of(MEDIA_TOKEN));
    verify(mediaItemSearchResponse).getMediaItems();

    // Check pagination token
    ContinuationData continuationData = result.getContinuationData();
    PaginationData paginationToken = continuationData.getPaginationData();
    assertNull(paginationToken);
  }

  @Test
  public void populateContainedMediaList()
      throws IOException, InvalidTokenException, PermissionDeniedException, UploadErrorException {
    // Set up an album with two photos
    setUpSingleAlbum();
    when(albumListResponse.getNextPageToken()).thenReturn(null);

    MediaItemSearchResponse albumMediaResponse = mock(MediaItemSearchResponse.class);
    GoogleMediaItem firstPhoto = setUpSinglePhoto("some://fake/gphotoapi/uri", "some-upstream-generated-photo-id");
    String secondUri = "second uri";
    String secondId = "second id";
    GoogleMediaItem secondPhoto = setUpSinglePhoto(secondUri, secondId);

    when(photosInterface.listMediaItems(eq(Optional.of(ALBUM_ID)), any(Optional.class)))
        .thenReturn(albumMediaResponse);
    when(albumMediaResponse.getMediaItems())
        .thenReturn(new GoogleMediaItem[] {firstPhoto, secondPhoto});
    when(albumMediaResponse.getNextPageToken()).thenReturn(null);

    // Run test
    googleMediaExporter.populateContainedMediaList(uuid, null);

    // Check contents of job store
    ArgumentCaptor<InputStream> inputStreamArgumentCaptor =
        ArgumentCaptor.forClass(InputStream.class);
    verify(jobStore).create(eq(uuid), eq("tempMediaData"), inputStreamArgumentCaptor.capture());
    TempMediaData tempMediaData =
        new ObjectMapper().readValue(inputStreamArgumentCaptor.getValue(), TempMediaData.class);
    assertThat(tempMediaData.lookupContainedPhotoIds()).containsExactly("some-upstream-generated-photo-id", secondId);
  }

  @Test
  /* Tests that when there is no album information passed along to exportMedia, only albumless
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

    TempMediaData tempMediaData = new TempMediaData(uuid);
    tempMediaData.addContainedPhotoId(containedPhotoId);
    InputStream stream = GoogleMediaExporter.convertJsonToInputStream(tempMediaData);
    when(jobStore.getStream(uuid, "tempMediaData")).thenReturn(new InputStreamWrapper(stream));

    // Run test
    ExportResult<MediaContainerResource> result =
        googleMediaExporter.exportMedia(null, Optional.empty(), Optional.empty(), uuid);

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
  // TODO(zacsh) delete this helper in favor of explicitly setting the fields that an assertion will
  // _actually_ use (and doing so _inlined, visibly_ in the arrange phase).
  private static GoogleMediaItem setUpSinglePhoto(String imageUri, String photoId) {
    MediaMetadata mediaMetadata = new MediaMetadata();
    mediaMetadata.setPhoto(new Photo());
    GoogleMediaItem googleMediaItem =
        setUpSingleMediaItem(imageUri, photoId, mediaMetadata);
    googleMediaItem.setMimeType("image/jpeg");
    return googleMediaItem;
  }

  /** Sets up a response for a single photo */
  private static GoogleMediaItem setUpSingleVideo(String videoUri, String videoId) {
    MediaMetadata mediaMetadata = new MediaMetadata();
    mediaMetadata.setVideo(new Video());
    GoogleMediaItem googleMediaItem =
        setUpSingleMediaItem(videoUri, videoId, mediaMetadata);
    googleMediaItem.setMimeType("video/mp4");
    return googleMediaItem;
  }

  /** Sets up a response for a single photo */
  private static GoogleMediaItem setUpSingleMediaItem(String mediaUri, String mediaId, MediaMetadata mediaMetadata) {
    GoogleMediaItem googleMediaItem = new GoogleMediaItem();
    googleMediaItem.setDescription("Description");
    googleMediaItem.setBaseUrl(mediaUri);
    googleMediaItem.setId(mediaId);
    googleMediaItem.setFilename(FILENAME);
    googleMediaItem.setMediaMetadata(mediaMetadata);
    return googleMediaItem;
  }
}
