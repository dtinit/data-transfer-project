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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.json.gson.GsonFactory;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.google.common.FailedToListAlbumsException;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.mediaModels.AlbumListResponse;
import org.datatransferproject.datatransfer.google.mediaModels.GoogleAlbum;
import org.datatransferproject.datatransfer.google.mediaModels.GoogleMediaItem;
import org.datatransferproject.datatransfer.google.mediaModels.MediaItemSearchResponse;
import org.datatransferproject.datatransfer.google.mediaModels.MediaMetadata;
import org.datatransferproject.datatransfer.google.mediaModels.Photo;
import org.datatransferproject.datatransfer.google.mediaModels.Video;
import org.datatransferproject.datatransfer.google.photos.GooglePhotosInterface;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore.InputStreamWrapper;
import org.datatransferproject.spi.transfer.idempotentexecutor.RetryingInMemoryIdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.datatransferproject.spi.transfer.types.TempMediaData;
import org.datatransferproject.spi.transfer.types.UploadErrorException;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.PaginationData;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.ContainerResource;
import org.datatransferproject.types.common.models.IdOnlyContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.transfer.retry.RetryStrategyLibrary;
import org.datatransferproject.types.transfer.retry.UniformRetryStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.datatransferproject.spi.transfer.idempotentexecutor.RetryingInMemoryIdempotentImportExecutor;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.transfer.errors.ErrorDetail;
import org.datatransferproject.types.transfer.retry.RetryStrategyLibrary;
import org.datatransferproject.types.transfer.retry.UniformRetryStrategy;
import org.mockito.ArgumentCaptor;

public class GoogleMediaExporterTest {

  private static String FILENAME = "filename";
  private static String ALBUM_ID = "GoogleAlbum id";
  private static String ALBUM_TOKEN = "some-upstream-generated-album-token";
  private static String MEDIA_TOKEN = "some-upstream-generated-media-token";
  private static long RETRY_INTERVAL_MILLIS = 100L;
  private static int RETRY_MAX_ATTEMPTS = 1;
  private static UUID uuid = UUID.randomUUID();

  private TokensAndUrlAuthData authData;
  private RetryingInMemoryIdempotentImportExecutor retryingExecutor;
  private GoogleMediaExporter googleMediaExporter;
  private GoogleMediaExporter retryingGoogleMediaExporter;
  private JobStore jobStore;
  private GooglePhotosInterface photosInterface;

  private MediaItemSearchResponse mediaItemSearchResponse;
  private AlbumListResponse albumListResponse;

  @BeforeEach
  public void setup()
      throws IOException, InvalidTokenException, PermissionDeniedException, UploadErrorException {
    GoogleCredentialFactory credentialFactory = mock(GoogleCredentialFactory.class);
    jobStore = mock(JobStore.class);
    when(jobStore.getStream(any(), anyString())).thenReturn(mock(InputStreamWrapper.class));
    photosInterface = mock(GooglePhotosInterface.class);

    albumListResponse = mock(AlbumListResponse.class);
    mediaItemSearchResponse = mock(MediaItemSearchResponse.class);

    Monitor monitor = mock(Monitor.class);
    authData = mock(TokensAndUrlAuthData.class);

    retryingExecutor = new RetryingInMemoryIdempotentImportExecutor(monitor,
        new RetryStrategyLibrary(
            ImmutableList.of(),
            new UniformRetryStrategy(RETRY_MAX_ATTEMPTS, RETRY_INTERVAL_MILLIS, "identifier")
        )
    );

    googleMediaExporter =
        new GoogleMediaExporter(
            credentialFactory, jobStore, GsonFactory.getDefaultInstance(), monitor, photosInterface);

    retryingGoogleMediaExporter = new GoogleMediaExporter(
        credentialFactory,
        jobStore,
        GsonFactory.getDefaultInstance(),
        monitor,
        photosInterface,
        retryingExecutor,
        true);

    when(photosInterface.listAlbums(any(Optional.class))).thenReturn(albumListResponse);
    when(photosInterface.listMediaItems(any(Optional.class), any(Optional.class)))
        .thenReturn(mediaItemSearchResponse);

    verifyNoInteractions(credentialFactory);
  }

  @Test
  public void exportAlbumFirstSet() throws IOException, InvalidTokenException, PermissionDeniedException, FailedToListAlbumsException {
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
  public void exportAlbumSubsequentSet() throws IOException, InvalidTokenException, PermissionDeniedException, FailedToListAlbumsException {
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
          throws IOException, InvalidTokenException, PermissionDeniedException, UploadErrorException, FailedToListAlbumsException {
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

  @Test
  public void testGetGoogleMediaItemSucceeds() throws IOException, InvalidTokenException, PermissionDeniedException {
    String mediaItemID = "media_id";
    MediaMetadata mediaMetadata = new MediaMetadata();
    when(photosInterface.getMediaItem(any())).thenReturn(setUpSingleMediaItem(mediaItemID, mediaItemID, mediaMetadata));

    assertThat(retryingGoogleMediaExporter.getGoogleMediaItem(mediaItemID, mediaItemID, mediaItemID, authData)).isInstanceOf(GoogleMediaItem.class);
  }

  @Test
  public void testExportPhotosContainer_photosRetrying() throws IOException, InvalidTokenException, PermissionDeniedException, UploadErrorException, FailedToListAlbumsException {
    String photoIdToFail1 = "photo3";
    String photoIdToFail2 = "photo5";

    ImmutableList<PhotoAlbum> albums = ImmutableList.of();
    ImmutableList<PhotoModel> photos = ImmutableList.of(
        setUpSinglePhotoModel("", "photo1"),
        setUpSinglePhotoModel("", "photo2"),
        setUpSinglePhotoModel("", photoIdToFail1),
        setUpSinglePhotoModel("", "photo4"),
        setUpSinglePhotoModel("", photoIdToFail2),
        setUpSinglePhotoModel("", "photo6")
    );

    PhotosContainerResource container = new PhotosContainerResource(albums, photos);
    ExportInformation exportInfo = new ExportInformation(null, container);

    MediaMetadata photoMediaMetadata = new MediaMetadata();
    photoMediaMetadata.setPhoto(new Photo());


    // For the photo_id_to_fail photos, throw an exception.
    when(photosInterface.getMediaItem(photoIdToFail1)).thenThrow(IOException.class);
    when(photosInterface.getMediaItem(photoIdToFail2)).thenThrow(IOException.class);
    // For all other photos, return a media item.
    for (PhotoModel photoModel: photos) {
      if (photoModel.getDataId().equals(photoIdToFail1) || photoModel.getDataId().equals(photoIdToFail2)) {
        continue;
      }
      when(photosInterface.getMediaItem(photoModel.getDataId())).thenReturn(
          setUpSingleMediaItem(photoModel.getDataId(), photoModel.getDataId(), photoMediaMetadata)
      );
    }

    ExportResult<MediaContainerResource> result = retryingGoogleMediaExporter.export(
        uuid, authData, Optional.of(exportInfo)
    );
    assertThat(
        result.getExportedData().getPhotos().stream().map(x -> x.getDataId()).collect(Collectors.toList())
    ).isEqualTo(
        photos.stream().map(
            x -> x.getDataId()
        ).filter(
            dataId -> !(dataId.equals(photoIdToFail1) || dataId.equals(photoIdToFail2))
        ).collect(
            Collectors.toList()
        )
    );
    assertThat(result.getExportedData().getPhotos().size()).isEqualTo(photos.size() - 2);
    assertThat(retryingExecutor.getErrors().size()).isEqualTo(2);
    assertThat(retryingExecutor.getErrors().stream().findFirst().toString().contains("IOException")).isTrue();
  }

  @Test
  public void testGetGoogleMediaItemFailed() throws IOException, InvalidTokenException, PermissionDeniedException {
    String mediaItemID = "media_id";
    when(photosInterface.getMediaItem(mediaItemID)).thenThrow(IOException.class);

    long start = System.currentTimeMillis();
    assertThat(retryingGoogleMediaExporter.getGoogleMediaItem(mediaItemID, mediaItemID, mediaItemID, authData)).isNull();
    long end = System.currentTimeMillis();

    // If retrying occurred, then the retry_interval must have been waited at least max_attempts
    // amount of times.
    assertThat(end - start).isAtLeast(RETRY_INTERVAL_MILLIS * RETRY_MAX_ATTEMPTS);
    assertThat(retryingExecutor.getErrors().size()).isEqualTo(1);
    assertThat(retryingExecutor.getErrors().stream().findFirst().toString()).contains("IOException");


    start = System.currentTimeMillis();
    assertThrows(IOException.class, () -> googleMediaExporter.getGoogleMediaItem(mediaItemID, mediaItemID, mediaItemID, authData));
    end = System.currentTimeMillis();

    assertThat(end - start).isLessThan(RETRY_INTERVAL_MILLIS * RETRY_MAX_ATTEMPTS);
  }

  @Test
  public void testExportAlbums_failureInterruptsTransfer() throws Exception {
    String albumIdToFail1 = "albumid3";
    String albumIdToFail2 = "albumid5";

    ImmutableList<PhotoModel> photos = ImmutableList.of();
    ImmutableList<PhotoAlbum> albums = ImmutableList.of(
        setUpSinglePhotoAlbum("albumid1", "album 1`", ""),
        setUpSinglePhotoAlbum("albumid2", "album 2", ""),
        setUpSinglePhotoAlbum(albumIdToFail1, "album 3", ""),
        setUpSinglePhotoAlbum("albumid4", "album 4", ""),
        setUpSinglePhotoAlbum(albumIdToFail2, "album 5", ""),
        setUpSinglePhotoAlbum("albumid6", "album 6", "")
    );

    PhotosContainerResource container = new PhotosContainerResource(albums, photos);
    ExportInformation exportInfo = new ExportInformation(null, container);

    MediaMetadata photoMediaMetadata = new MediaMetadata();
    photoMediaMetadata.setPhoto(new Photo());

    // For the album_id_to_fail albums, throw an exception.
    when(photosInterface.getAlbum(albumIdToFail1)).thenThrow(IOException.class);
    when(photosInterface.getAlbum(albumIdToFail2)).thenThrow(IOException.class);
    // For all other albums, return a GoogleMediaAlbum.
    for (PhotoAlbum photoAlbum: albums) {
      if (photoAlbum.getId().equals(albumIdToFail1) || photoAlbum.getId().equals(albumIdToFail2)) {
        continue;
      }
      when(photosInterface.getAlbum(photoAlbum.getId())).thenReturn(
          setUpGoogleAlbum(Optional.of(photoAlbum.getId()), Optional.of(photoAlbum.getName()))
      );
    }

    assertThrows(IOException.class, () -> googleMediaExporter.export(
        uuid, authData, Optional.of(exportInfo)
    ));
  }

  @Test
  public void testExportAlbums_retryingSkipsFailures() throws Exception {
    String albumIdToFail1 = "albumid3";
    String albumIdToFail2 = "albumid5";

    ImmutableList<PhotoModel> photos = ImmutableList.of();
    ImmutableList<PhotoAlbum> albums = ImmutableList.of(
        setUpSinglePhotoAlbum("albumid1", "album 1`", ""),
        setUpSinglePhotoAlbum("albumid2", "album 2", ""),
        setUpSinglePhotoAlbum(albumIdToFail1, "album 3", ""),
        setUpSinglePhotoAlbum("albumid4", "album 4", ""),
        setUpSinglePhotoAlbum(albumIdToFail2, "album 5", ""),
        setUpSinglePhotoAlbum("albumid6", "album 6", "")
    );

    PhotosContainerResource container = new PhotosContainerResource(albums, photos);
    ExportInformation exportInfo = new ExportInformation(null, container);

    MediaMetadata photoMediaMetadata = new MediaMetadata();
    photoMediaMetadata.setPhoto(new Photo());

    // For the album_id_to_fail albums, throw an exception.
    when(photosInterface.getAlbum(albumIdToFail1)).thenThrow(IOException.class);
    when(photosInterface.getAlbum(albumIdToFail2)).thenThrow(IOException.class);
    // For all other albums, return a GoogleMediaAlbum.
    for (PhotoAlbum photoAlbum: albums) {
      if (photoAlbum.getId().equals(albumIdToFail1) || photoAlbum.getId().equals(albumIdToFail2)) {
        continue;
      }
      when(photosInterface.getAlbum(photoAlbum.getId())).thenReturn(
          setUpGoogleAlbum(Optional.of(photoAlbum.getId()), Optional.of(photoAlbum.getName()))
      );
    }

    ExportResult<MediaContainerResource> result = retryingGoogleMediaExporter.export(
        uuid, authData, Optional.of(exportInfo)
    );

    assertThat(
        result.getExportedData().getAlbums().stream().map(x -> x.getId()).collect(Collectors.toList())
    ).isEqualTo(
        albums.stream().map(
            x -> x.getId()
        ).filter(
            id -> !(id.equals(albumIdToFail1) || id.equals(albumIdToFail2))
        ).collect(
            Collectors.toList()
        )
    );
    assertThat(result.getExportedData().getAlbums().size()).isEqualTo(albums.size() - 2);
    assertThat(retryingExecutor.getErrors().size()).isEqualTo(2);
    assertThat(retryingExecutor.getErrors().stream().findFirst().toString().contains("IOException")).isTrue();
  }

  /** Sets up a response with a single album, containing a single photo */
  private void setUpSingleAlbum() {
    GoogleAlbum albumEntry = new GoogleAlbum();
    albumEntry.setId(ALBUM_ID);
    albumEntry.setTitle("Title");

    when(albumListResponse.getAlbums()).thenReturn(new GoogleAlbum[] {albumEntry});
  }

  private GoogleAlbum setUpGoogleAlbum(Optional<String> albumId, Optional<String> albumTitle) {
    GoogleAlbum album = new GoogleAlbum();
    if (albumId.isPresent()) {
      album.setId(albumId.get());
    }
    if (albumTitle.isPresent()) {
      album.setTitle(albumTitle.get());
    }

    return album;
  }

  private static PhotoAlbum setUpSinglePhotoAlbum(String albumId, String albumName, String description) {
    return new PhotoAlbum(albumId, albumName, description);
  }

  private static PhotoModel setUpSinglePhotoModel(String albumId, String dataId) {
    return new PhotoModel("Title", "fetchableUrl", "description",
        "photo", dataId, albumId, false, new Date(1370420961000L));
  }

  /** Sets up a response for a single photo */
  // TODO(zacsh) delete this helper in favor of explicitly setting the fields that an assertion will
  // _actually_ use (and doing so _inlined, visibly_ in the arrange phase).
  private static GoogleMediaItem setUpSinglePhoto(String imageUri, String photoId) {
    MediaMetadata mediaMetadata = new MediaMetadata();
    mediaMetadata.setPhoto(new Photo());
    mediaMetadata.setCreationTime("2022-09-01T20:25:38Z");
    GoogleMediaItem googleMediaItem =
        setUpSingleMediaItem(imageUri, photoId, mediaMetadata);
    googleMediaItem.setMimeType("image/jpeg");
    return googleMediaItem;
  }

  /** Sets up a response for a single photo */
  private static GoogleMediaItem setUpSingleVideo(String videoUri, String videoId) {
    MediaMetadata mediaMetadata = new MediaMetadata();
    mediaMetadata.setVideo(new Video());
    mediaMetadata.setCreationTime("2022-09-01T20:25:38Z");
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
    mediaMetadata.setCreationTime("2022-09-01T20:25:38Z");
    googleMediaItem.setMediaMetadata(mediaMetadata);
    return googleMediaItem;
  }
}
