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

package org.datatransferproject.transfer.imgur.photos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import okhttp3.OkHttpClient;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.imgur.photos.ImgurPhotosExporter;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.IntPaginationToken;
import org.datatransferproject.types.common.models.IdOnlyContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class ImgurPhotoExporterTest {

  String albumsResponse;
  String album1ImagesResponse;
  String allImagesResponse;
  String page1Response;
  String page2Response;

  {
    try {
      albumsResponse = Resources.toString(Resources.getResource("albums.json"), Charsets.UTF_8).trim();
      album1ImagesResponse =
          Resources.toString(Resources.getResource("album_1_images.json"), Charsets.UTF_8).trim();
      allImagesResponse =
          Resources.toString(Resources.getResource("all_images.json"), Charsets.UTF_8).trim();
      page1Response = Resources.toString(Resources.getResource("page1.json"), Charsets.UTF_8).trim();
      page2Response = Resources.toString(Resources.getResource("page2.json"), Charsets.UTF_8).trim();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private MockWebServer server;
  private OkHttpClient client;
  private ObjectMapper mapper;
  private TokensAndUrlAuthData token;
  private JobStore jobStore;
  private ImgurPhotosExporter exporter;

  Monitor monitor = mock(Monitor.class);

  @Test
  public void testAlbumsExport() throws Exception {
    server.enqueue(new MockResponse().setBody(albumsResponse));

    ExportResult<PhotosContainerResource> result =
        exporter.export(UUID.randomUUID(), token, Optional.empty());

    PhotosContainerResource resource = result.getExportedData();

    assertEquals(2, resource.getAlbums().size());
    assertEquals(0, resource.getPhotos().size());

    PhotoAlbum album1 =
        resource
            .getAlbums()
            .stream()
            .filter(album -> "albumId1".equals(album.getId()))
            .findFirst()
            .get();
    assertEquals("Album 1", album1.getName());

    PhotoAlbum album2 =
        resource
            .getAlbums()
            .stream()
            .filter(album -> "albumId2".equals(album.getId()))
            .findFirst()
            .get();
    assertEquals("Album 2", album2.getName());
  }

  @Test
  public void testAlbumPhotosExport() throws Exception {
    server.enqueue(new MockResponse().setBody(albumsResponse));
    server.enqueue(new MockResponse().setBody(album1ImagesResponse));

    // export albums
    exporter.export(UUID.randomUUID(), token, Optional.empty());

    // export album photos
    ExportResult<PhotosContainerResource> result =
        exporter.export(
            UUID.randomUUID(),
            token,
            Optional.of(new ExportInformation(null, new IdOnlyContainerResource("albumId1"))));

    PhotosContainerResource resource = result.getExportedData();

    assertEquals(2, resource.getPhotos().size());

    PhotoModel photo1 =
        resource
            .getPhotos()
            .stream()
            .filter(p -> "album1Photo1".equals(p.getDataId()))
            .findFirst()
            .get();
    assertEquals("photo_1_name", photo1.getTitle());
    assertEquals("image/jpeg", photo1.getMediaType());
    assertEquals("albumId1", photo1.getAlbumId());

    PhotoModel photo2 =
        resource
            .getPhotos()
            .stream()
            .filter(p -> "album1Photo2".equals(p.getDataId()))
            .findFirst()
            .get();
    assertEquals(null, photo2.getTitle());
    assertEquals("image/jpeg", photo2.getMediaType());
    assertEquals("albumId1", photo2.getAlbumId());
  }

  @Test
  public void testAlbumAndNonAlbumPhotoExport() throws Exception {
    server.enqueue(new MockResponse().setBody(albumsResponse));
    server.enqueue(new MockResponse().setBody(album1ImagesResponse));
    server.enqueue(new MockResponse().setBody(allImagesResponse));

    // export albums
    exporter.export(UUID.randomUUID(), token, Optional.empty());

    // export album photos
    ExportResult<PhotosContainerResource> albumPhotosResult =
        exporter.export(
            UUID.randomUUID(),
            token,
            Optional.of(new ExportInformation(null, new IdOnlyContainerResource("albumId1"))));

    assertEquals(2, albumPhotosResult.getExportedData().getPhotos().size());

    // export non-album photos
    ExportResult<PhotosContainerResource> nonAlbumPhotosResult =
        exporter.export(
            UUID.randomUUID(),
            token,
            Optional.of(
                new ExportInformation(
                    null, new IdOnlyContainerResource(ImgurPhotosExporter.DEFAULT_ALBUM_ID))));
    PhotosContainerResource resource = nonAlbumPhotosResult.getExportedData();
    assertEquals(1, resource.getPhotos().size());

    PhotoModel photo1 =
        resource
            .getPhotos()
            .stream()
            .filter(p -> "nonAlbumPhoto1".equals(p.getDataId()))
            .findFirst()
            .get();
    assertEquals("non-album-photo-name", photo1.getTitle());
    assertEquals("image/jpeg", photo1.getMediaType());
    assertEquals(ImgurPhotosExporter.DEFAULT_ALBUM_ID, photo1.getAlbumId());
  }

  @Test
  public void testNonAlbumPhotoExport() throws Exception {

    // all photos are non-album
    server.enqueue(new MockResponse().setBody(allImagesResponse));

    ExportResult<PhotosContainerResource> nonAlbumPhotosResult =
        exporter.export(
            UUID.randomUUID(),
            token,
            Optional.of(
                new ExportInformation(
                    null, new IdOnlyContainerResource(ImgurPhotosExporter.DEFAULT_ALBUM_ID))));
    PhotosContainerResource resource = nonAlbumPhotosResult.getExportedData();
    assertEquals(3, resource.getPhotos().size());
  }

  @Test
  public void testPagination() throws Exception {

    server.enqueue(new MockResponse().setBody(page1Response));
    server.enqueue(new MockResponse().setBody(page2Response));
    int page = 0;

    ExportResult<PhotosContainerResource> page1Result =
        exporter.export(
            UUID.randomUUID(),
            token,
            Optional.of(
                new ExportInformation(
                    new IntPaginationToken(page),
                    new IdOnlyContainerResource(ImgurPhotosExporter.DEFAULT_ALBUM_ID))));
    page++;
    PhotosContainerResource page1Resource = page1Result.getExportedData();
    // 1th request returns 10 photos
    assertEquals(10, page1Resource.getPhotos().size());
    assertEquals(
        page,
        ((IntPaginationToken) page1Result.getContinuationData().getPaginationData()).getStart());

    ExportResult<PhotosContainerResource> page2Result =
        exporter.export(
            UUID.randomUUID(),
            token,
            Optional.of(
                new ExportInformation(
                    new IntPaginationToken(page),
                    new IdOnlyContainerResource(ImgurPhotosExporter.DEFAULT_ALBUM_ID))));
    page++;
    PhotosContainerResource page2Resource = page2Result.getExportedData();
    // 2th request returns 2 photos
    assertEquals(2, page2Resource.getPhotos().size());
    assertEquals(
        page,
        ((IntPaginationToken) page2Result.getContinuationData().getPaginationData()).getStart());
  }

  @Before
  public void setUp() throws IOException {
    client = new OkHttpClient.Builder().build();
    mapper = new ObjectMapper();
    token = new TokensAndUrlAuthData("accessToken", "refreshToken", "tokenUrl");
    jobStore = mock(JobStore.class);
    server = new MockWebServer();
    server.start();
    exporter =
        new ImgurPhotosExporter(monitor, client, mapper, jobStore, server.url("").toString());
  }

  @After
  public void tearDown() throws Exception {
    server.shutdown();
  }
}
