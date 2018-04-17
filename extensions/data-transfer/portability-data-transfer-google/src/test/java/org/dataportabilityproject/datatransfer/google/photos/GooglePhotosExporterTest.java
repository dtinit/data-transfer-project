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
package org.dataportabilityproject.datatransfer.google.photos;

import static com.google.common.truth.Truth.assertThat;
import static org.dataportabilityproject.datatransfer.google.photos.GooglePhotosExporter.ALBUM_TOKEN_PREFIX;
import static org.dataportabilityproject.datatransfer.google.photos.GooglePhotosExporter.PHOTO_TOKEN_PREFIX;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.MediaContent;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.AlbumFeed;
import com.google.gdata.data.photos.GphotoEntry;
import com.google.gdata.data.photos.UserFeed;
import com.google.gdata.util.ContentType;
import com.google.gdata.util.ServiceException;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.dataportabilityproject.datatransfer.google.common.GoogleCredentialFactory;
import org.dataportabilityproject.spi.transfer.provider.ExportResult;
import org.dataportabilityproject.spi.transfer.types.ContinuationData;
import org.dataportabilityproject.spi.transfer.types.ExportInformation;
import org.dataportabilityproject.spi.transfer.types.IdOnlyContainerResource;
import org.dataportabilityproject.spi.transfer.types.StringPaginationToken;
import org.dataportabilityproject.types.transfer.models.ContainerResource;
import org.dataportabilityproject.types.transfer.models.photos.PhotoAlbum;
import org.dataportabilityproject.types.transfer.models.photos.PhotoModel;
import org.dataportabilityproject.types.transfer.models.photos.PhotosContainerResource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

public class GooglePhotosExporterTest {

  private String PHOTO_TITLE = "Google photo title";
  private String IMG_URI = "image uri";
  private String JPEG_MEDIA_TYPE = "image/jpeg";
  private String ALBUM_ID = "Album id";

  private UUID uuid = UUID.randomUUID();

  private int MAX_RESULTS = 1;

  private GooglePhotosExporter googlePhotosExporter;

  private GoogleCredentialFactory googleCredentialFactory;

  private PicasawebService photoService;

  private UserFeed usersAlbumsFeed;
  private AlbumFeed albumsPhotoFeed;

  private ContentType contentType;
  private MediaContent mediaContent;

  @Before
  public void setup() throws IOException, ServiceException {
    photoService = mock(PicasawebService.class);

    usersAlbumsFeed = mock(UserFeed.class);
    albumsPhotoFeed = mock(AlbumFeed.class);
    contentType = mock(ContentType.class);
    mediaContent = mock(MediaContent.class);

    googlePhotosExporter = new GooglePhotosExporter(googleCredentialFactory, photoService,
        MAX_RESULTS);

    when(photoService.getFeed(Matchers.any(URL.class), Matchers.eq(UserFeed.class)))
        .thenReturn(usersAlbumsFeed);
    when(photoService.getFeed(Matchers.any(URL.class), Matchers.eq(AlbumFeed.class)))
        .thenReturn(albumsPhotoFeed);
    when(mediaContent.getUri()).thenReturn(IMG_URI);
    when(contentType.getMediaType()).thenReturn(JPEG_MEDIA_TYPE);
    when(mediaContent.getMimeType()).thenReturn(contentType);
  }

  @Test
  public void exportAlbumFirstSet() throws IOException, ServiceException {
    setUpSingleAlbumResponse();
    int start = 1;

    // Run test
    ExportResult<PhotosContainerResource> result = googlePhotosExporter.export(uuid, null);

    // Check results
    // Verify correct methods were called
    URL albumURL = new URL(
        String.format(GooglePhotosExporter.URL_ALBUM_FEED_FORMAT, start, MAX_RESULTS));
    verify(photoService).getFeed(albumURL, UserFeed.class);
    verify(usersAlbumsFeed).getEntries(AlbumEntry.class);

    // Check pagination token
    ContinuationData continuationData = (ContinuationData) result.getContinuationData();
    StringPaginationToken paginationToken = (StringPaginationToken) continuationData
        .getPaginationData();
    assertThat(paginationToken.getToken())
        .isEqualTo(ALBUM_TOKEN_PREFIX + (start + MAX_RESULTS));

    // Check albums field of container
    Collection<PhotoAlbum> actualAlbums = result.getExportedData().getAlbums();
    assertThat(actualAlbums.stream().map(PhotoAlbum::getId).collect(Collectors.toList()))
        .containsExactly(ALBUM_ID);

    // Check photos field of container (should be empty, even though there is a photo in the original album)
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
  public void exportAlbumSubsequentSet() throws IOException, ServiceException {
    setUpSingleAlbumResponse();
    int start = 2;
    StringPaginationToken inputPaginationToken = new StringPaginationToken(
        ALBUM_TOKEN_PREFIX + start);
    ExportInformation inputExportInformation = new ExportInformation(inputPaginationToken,
        null);

    // Run test
    ExportResult<PhotosContainerResource> result = googlePhotosExporter
        .export(uuid, null, inputExportInformation);

    // Check results
    // Verify correct methods were called
    URL albumURL = new URL(
        String.format(GooglePhotosExporter.URL_ALBUM_FEED_FORMAT, start, MAX_RESULTS));
    verify(photoService).getFeed(albumURL, UserFeed.class);
    verify(usersAlbumsFeed).getEntries(AlbumEntry.class);

    // Check pagination token
    ContinuationData continuationData = (ContinuationData) result.getContinuationData();
    StringPaginationToken paginationToken = (StringPaginationToken) continuationData
        .getPaginationData();
    assertThat(paginationToken.getToken())
        .isEqualTo(ALBUM_TOKEN_PREFIX + (start + MAX_RESULTS));
  }

  @Test
  public void exportPhotoFirstSet() throws IOException, ServiceException {
    setUpSinglePhotoResponse();

    int start = 1;
    ContainerResource inputContainerResource = new IdOnlyContainerResource(ALBUM_ID);
    ExportInformation inputExportInformation = new ExportInformation(null, inputContainerResource);

    ExportResult<PhotosContainerResource> result = googlePhotosExporter
        .export(uuid, null, inputExportInformation);

    // Check results
    // Verify correct methods were called
    URL photosURL = new URL(
        String.format(GooglePhotosExporter.URL_PHOTO_FEED_FORMAT, ALBUM_ID, start, MAX_RESULTS));
    verify(photoService).getFeed(photosURL, AlbumFeed.class);
    verify(albumsPhotoFeed).getEntries();

    // Check pagination
    ContinuationData continuationData = (ContinuationData) result.getContinuationData();
    StringPaginationToken paginationToken = (StringPaginationToken) continuationData
        .getPaginationData();
    assertThat(paginationToken.getToken()).isEqualTo(PHOTO_TOKEN_PREFIX + (start + MAX_RESULTS));

    // Check albums field of container (should be empty)
    Collection<PhotoAlbum> actualAlbums = result.getExportedData().getAlbums();
    assertThat(actualAlbums).isEmpty();

    // Check photos field of container
    Collection<PhotoModel> actualPhotos = result.getExportedData().getPhotos();
    assertThat(actualPhotos.stream().map(PhotoModel::getTitle).collect(Collectors.toList()))
        .containsExactly(PHOTO_TITLE);
    assertThat(actualPhotos.stream().map(PhotoModel::getAlbumId).collect(Collectors.toList()))
        .containsExactly(ALBUM_ID);
  }

  @Test
  public void exportPhotoSubsequentSet() throws IOException, ServiceException {
    setUpSinglePhotoResponse();

    int start = 2;
    StringPaginationToken inputPaginationToken = new StringPaginationToken(
        PHOTO_TOKEN_PREFIX + start);
    ContainerResource inputContainerResource = new IdOnlyContainerResource(ALBUM_ID);
    ExportInformation inputExportInformation = new ExportInformation(inputPaginationToken,
        inputContainerResource);

    // Run test
    ExportResult<PhotosContainerResource> result = googlePhotosExporter
        .export(uuid, null, inputExportInformation);

    // Check results
    // Verify correct methods were called
    URL photoURL = new URL(
        String.format(GooglePhotosExporter.URL_PHOTO_FEED_FORMAT, ALBUM_ID, start, MAX_RESULTS));
    verify(photoService).getFeed(photoURL, AlbumFeed.class);
    verify(albumsPhotoFeed).getEntries();

    // Check pagination token
    ContinuationData continuationData = (ContinuationData) result.getContinuationData();
    StringPaginationToken paginationToken = (StringPaginationToken) continuationData
        .getPaginationData();
    assertThat(paginationToken.getToken()).isEqualTo(PHOTO_TOKEN_PREFIX + (start + MAX_RESULTS));
  }

  /**
   * Sets up a response with a single album, containing a single photo
   */
  private void setUpSingleAlbumResponse() {
    AlbumEntry albumEntry = new AlbumEntry();
    albumEntry.setId(ALBUM_ID);
    albumEntry.setDescription(new PlainTextConstruct("Description"));
    albumEntry.setTitle(new PlainTextConstruct("Title"));

    when(usersAlbumsFeed.getEntries(AlbumEntry.class))
        .thenReturn(Collections.singletonList(albumEntry));

    setUpSinglePhotoResponse();
  }

  /**
   * Sets up a response for a single photo
   */
  private void setUpSinglePhotoResponse() {
    GphotoEntry photoEntry = new GphotoEntry();
    photoEntry.setTitle(new PlainTextConstruct(PHOTO_TITLE));
    photoEntry.setContent(mediaContent);
    photoEntry.setDescription(new PlainTextConstruct("Description"));

    when(albumsPhotoFeed.getEntries()).thenReturn(Collections.singletonList(photoEntry));
  }
}
