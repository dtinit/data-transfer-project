/*
 * Copyright 2018 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.facebook.photos;

import com.google.common.collect.Lists;
import com.restfb.Connection;
import com.restfb.types.Album;
import com.restfb.types.Photo;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.transfer.ImageStreamProvider;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.IdOnlyContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import static com.google.common.truth.Truth.assertThat;
import static org.datatransferproject.transfer.facebook.photos.FacebookPhotosExporter.PHOTO_TOKEN_PREFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FacebookPhotosExporterTest {
  private static final String ALBUM_NAME = "My Album";
  private static final String ALBUM_ID = "1946595";
  private static final String ALBUM_DESCRIPTION = "This is a test album";

  private static final String PHOTO_ID = "937644721";
  private static final String PHOTO_SOURCE = "https://www.example.com/photo.jpg";
  private static final String PHOTO_NAME = "Example photo";
  private static final Date PHOTO_TIME = new Date(1234567890123L);

  private FacebookPhotosExporter facebookPhotosExporter;
  private UUID uuid = UUID.randomUUID();

  @Before
  public void setUp() throws IOException {
    FacebookPhotosInterface photosInterface = mock(FacebookPhotosInterface.class);

    // Set up example album
    Album album = new Album();
    album.setId(ALBUM_ID);
    album.setName(ALBUM_NAME);
    album.setDescription(ALBUM_DESCRIPTION);

    ArrayList<Album> albums = new ArrayList<>();
    albums.add(album);

    @SuppressWarnings("unchecked")
    Connection<Album> albumConnection = mock(Connection.class);
    when(photosInterface.getAlbums(Mockito.any())).thenReturn(albumConnection);
    when(albumConnection.getData()).thenReturn(albums);

    // Set up example photo
    Photo photo = new Photo();
    photo.setId(PHOTO_ID);
    photo.setCreatedTime(PHOTO_TIME);
    Photo.Image image = new Photo.Image();
    image.setSource(PHOTO_SOURCE);
    photo.addImage(image);
    photo.setName(PHOTO_NAME);

    ArrayList<Photo> photos = new ArrayList<>();
    photos.add(photo);

    @SuppressWarnings("unchecked")
    Connection<Photo> photoConnection = mock(Connection.class);

    when(photosInterface.getPhotos(ALBUM_ID, Optional.empty())).thenReturn(photoConnection);
    when(photoConnection.getData()).thenReturn(photos);

    final ImageStreamProvider imageStreamProvider = mock(ImageStreamProvider.class);
    InputStream inputStream = getClass().getClassLoader().getResourceAsStream("test.jpeg");
    HttpURLConnection connection = mock(HttpURLConnection.class);
    when(imageStreamProvider.getConnection(ArgumentMatchers.anyString())).thenReturn(connection);
    when(connection.getInputStream()).thenReturn(inputStream);

    final TemporaryPerJobDataStore store = mock(TemporaryPerJobDataStore.class);

    facebookPhotosExporter =
        new FacebookPhotosExporter(
            new AppCredentials("key", "secret"),
            photosInterface,
            null,
            store,
            imageStreamProvider);
  }

  @Test
  public void testExportAlbum() throws CopyExceptionWithFailureReason {
    ExportResult<PhotosContainerResource> result =
        facebookPhotosExporter.export(
            uuid, new TokensAndUrlAuthData("accessToken", null, null), Optional.empty());

    assertEquals(ExportResult.ResultType.CONTINUE, result.getType());
    PhotosContainerResource exportedData = result.getExportedData();
    assertEquals(1, exportedData.getAlbums().size());
    assertEquals(
        new PhotoAlbum(ALBUM_ID, ALBUM_NAME, ALBUM_DESCRIPTION),
        exportedData.getAlbums().toArray()[0]);
    assertNull(result.getContinuationData().getPaginationData());
    assertThat(result.getContinuationData().getContainerResources())
        .contains(new IdOnlyContainerResource(ALBUM_ID));
  }

  @Test
  public void testExportPhoto() throws CopyExceptionWithFailureReason {
    ExportResult<PhotosContainerResource> result =
        facebookPhotosExporter.export(
            uuid,
            new TokensAndUrlAuthData("accessToken", null, null),
            Optional.of(new ExportInformation(null, new IdOnlyContainerResource(ALBUM_ID))));

    assertEquals(ExportResult.ResultType.END, result.getType());
    PhotosContainerResource exportedData = result.getExportedData();
    assertEquals(1, exportedData.getPhotos().size());
    assertEquals(
        new PhotoModel(
            PHOTO_ID + ".jpg",
            PHOTO_ID,
            PHOTO_NAME,
            "image/jpg",
            PHOTO_ID,
            ALBUM_ID,
            false,
            PHOTO_TIME),
        exportedData.getPhotos().toArray()[0]);
  }

  @Test
  public void testSpecifiedAlbums() throws CopyExceptionWithFailureReason {
    ExportResult<PhotosContainerResource> result =
        facebookPhotosExporter.export(
            uuid,
            new TokensAndUrlAuthData("accessToken", null, null),
            Optional.of(
                new ExportInformation(
                    new StringPaginationToken(PHOTO_TOKEN_PREFIX),
                    new PhotosContainerResource(
                        Lists.newArrayList(new PhotoAlbum(ALBUM_ID, ALBUM_NAME, ALBUM_DESCRIPTION)),
                        new ArrayList<>()))));
    assertEquals(ExportResult.ResultType.CONTINUE, result.getType());
    PhotosContainerResource exportedData = result.getExportedData();
    assertEquals(1, exportedData.getAlbums().size());
    assertEquals(
        new PhotoAlbum(ALBUM_ID, ALBUM_NAME, ALBUM_DESCRIPTION),
        exportedData.getAlbums().toArray()[0]);
    assertNull((result.getContinuationData().getPaginationData()));
    assertThat(result.getContinuationData().getContainerResources())
        .contains(new IdOnlyContainerResource(ALBUM_ID));
  }

  @Test(expected = IllegalStateException.class)
  public void testIllegalExport() throws CopyExceptionWithFailureReason {
    facebookPhotosExporter.export(
        uuid,
        new TokensAndUrlAuthData("accessToken", null, null),
        Optional.of(new ExportInformation(new StringPaginationToken(PHOTO_TOKEN_PREFIX), null)));
  }
}
