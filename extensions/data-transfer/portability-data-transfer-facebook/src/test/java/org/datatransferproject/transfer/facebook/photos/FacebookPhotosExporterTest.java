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

import com.google.common.base.Preconditions;
import com.restfb.types.Album;
import com.restfb.types.Photo;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.transfer.models.photos.PhotoAlbum;
import org.datatransferproject.types.transfer.models.photos.PhotoModel;
import org.datatransferproject.types.transfer.models.photos.PhotosContainerResource;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class FacebookPhotosExporterTest {
  private static final String ALBUM_NAME = "My Album";
  private static final String ALBUM_ID = "1946595";
  private static final String ALBUM_DESCRIPTION = "This is a test album";

  private static final String PHOTO_ID = "937644721";
  private static final String PHOTO_SOURCE = "https://www.example.com/photo.jpg";
  private static final String PHOTO_NAME = "Example photo";

  private FacebookPhotosExporter facebookPhotosExporter;
  private UUID uuid = UUID.randomUUID();

  @Before
  public void setUp() {
    FacebookPhotosInterface photosInterface =
        new FacebookPhotosInterface() {
          @Override
          public Iterable<List<Album>> getAlbums() {
            Album album = new Album();
            album.setId(ALBUM_ID);
            album.setName(ALBUM_NAME);
            album.setDescription(ALBUM_DESCRIPTION);

            ArrayList<Album> innerList = new ArrayList<>();
            innerList.add(album);

            ArrayList<List<Album>> albums = new ArrayList<>();
            albums.add(innerList);

            return albums;
          }

          @Override
          public Iterable<List<Photo>> getPhotos(String albumId) {
            Preconditions.checkArgument(albumId.equals(ALBUM_ID));

            Photo photo = new Photo();
            photo.setId(PHOTO_ID);
            Photo.Image image = new Photo.Image();
            image.setSource(PHOTO_SOURCE);
            photo.addImage(image);
            photo.setName(PHOTO_NAME);

            ArrayList<Photo> innerList = new ArrayList<>();
            innerList.add(photo);

            ArrayList<List<Photo>> photos = new ArrayList<>();
            photos.add(innerList);

            return photos;
          }
        };
    facebookPhotosExporter =
        new FacebookPhotosExporter(new AppCredentials("key", "secret"), photosInterface);
  }

  @Test
  public void testExportSingleAlbum() {
    ExportResult<PhotosContainerResource> result =
        facebookPhotosExporter.export(
            uuid, new TokensAndUrlAuthData("accessToken", null, null), Optional.empty());

    assertEquals(ExportResult.ResultType.END, result.getType());
    PhotosContainerResource exportedData = result.getExportedData();
    assertEquals(1, exportedData.getAlbums().size());
    assertEquals(
        new PhotoAlbum(ALBUM_ID, ALBUM_NAME, ALBUM_DESCRIPTION),
        exportedData.getAlbums().toArray()[0]);
    assertEquals(1, exportedData.getPhotos().size());
    assertEquals(
        new PhotoModel(
            PHOTO_ID + ".jpg", PHOTO_SOURCE, PHOTO_NAME, "image/jpg", PHOTO_ID, ALBUM_ID, false),
        exportedData.getPhotos().toArray()[0]);
  }
}
