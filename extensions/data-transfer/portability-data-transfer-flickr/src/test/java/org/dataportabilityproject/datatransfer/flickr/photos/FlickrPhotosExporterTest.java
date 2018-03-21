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

package org.dataportabilityproject.datatransfer.flickr.photos;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.auth.AuthInterface;
import com.flickr4java.flickr.auth.Permission;
import com.flickr4java.flickr.people.User;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.PhotoList;
import com.flickr4java.flickr.photos.PhotosInterface;
import com.flickr4java.flickr.photos.Size;
import com.flickr4java.flickr.photosets.Photoset;
import com.flickr4java.flickr.photosets.Photosets;
import com.flickr4java.flickr.photosets.PhotosetsInterface;
import java.util.Collection;
import java.util.Collections;
import org.dataportabilityproject.spi.transfer.provider.ExportResult;
import org.dataportabilityproject.spi.transfer.types.ContinuationData;
import org.dataportabilityproject.spi.transfer.types.ExportInformation;
import org.dataportabilityproject.spi.transfer.types.IdOnlyContainerResource;
import org.dataportabilityproject.spi.transfer.types.IntPaginationToken;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.auth.TokenSecretAuthData;
import org.dataportabilityproject.types.transfer.models.ContainerResource;
import org.dataportabilityproject.types.transfer.models.photos.PhotoAlbum;
import org.dataportabilityproject.types.transfer.models.photos.PhotoModel;
import org.dataportabilityproject.types.transfer.models.photos.PhotosContainerResource;
import org.junit.Test;
import org.scribe.model.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlickrPhotosExporterTest {
  private static final String PHOTO_TITLE = "Title";
  private static final String FETCHABLE_URL = "fetchable_url";
  private static final String PHOTO_DESCRIPTION = "Description";
  private static final String MEDIA_TYPE = "jpeg";
  private static final String ALBUM_ID = "Album ID";
  private static final String ALBUM_NAME = "Album name";
  private static final String ALBUM_DESCRIPTION = "Album description";
  private static final PhotoModel PHOTO_MODEL =
      new PhotoModel(PHOTO_TITLE, FETCHABLE_URL, PHOTO_DESCRIPTION, MEDIA_TYPE, ALBUM_ID);
  private static final PhotoAlbum PHOTO_ALBUM =
      new PhotoAlbum(ALBUM_ID, ALBUM_NAME, ALBUM_DESCRIPTION);
  private static final String FLICKR_PHOTO_ID = "flickrPhotoId";
  private static final String FLICKR_ALBUM_ID = "flickrAlbumId";
  private static final Logger logger = LoggerFactory.getLogger(FlickrPhotosExporterTest.class);

  private Flickr flickr = mock(Flickr.class);
  private PhotosInterface photosInterface = mock(PhotosInterface.class);
  private PhotosetsInterface photosetsInterface = mock(PhotosetsInterface.class);
  private AuthInterface authInterface = mock(AuthInterface.class);

  private User user = mock(User.class);
  private Auth auth = new Auth(Permission.WRITE, user);

  private static Photo initializePhoto(String title, String url, String description) {
    Photo photo = new Photo();
    photo.setTitle(title);
    photo.setDescription(description);
    photo.setOriginalFormat(MEDIA_TYPE);
    Size size = new Size();
    size.setSource(url);
    size.setLabel(Size.ORIGINAL);
    photo.setSizes(Collections.singletonList(size));
    return photo;
  }

  private static Photoset initializePhotoset(String id, String title, String description) {
    Photoset photoset = new Photoset();
    photoset.setId(id);
    photoset.setTitle(title);
    photoset.setDescription(description);

    return photoset;
  }

  @Test
  public void toCommonPhoto() {
    Photo photo = initializePhoto(PHOTO_TITLE, FETCHABLE_URL, PHOTO_DESCRIPTION);
    PhotoModel photoModel = FlickrPhotosExporter.toCommonPhoto(photo, ALBUM_ID);

    assertThat(photoModel.getAlbumId()).isEqualTo(ALBUM_ID);
    assertThat(photoModel.getFetchableUrl()).isEqualTo(FETCHABLE_URL);
    assertThat(photoModel.getTitle()).isEqualTo(PHOTO_TITLE);
    assertThat(photoModel.getDescription()).isEqualTo(PHOTO_DESCRIPTION);
    assertThat(photoModel.getMediaType()).isEqualTo("image/jpeg");
  }

  @Test
  public void getMimeType() {
    assertThat(FlickrPhotosExporter.toMimeType("jpeg")).isEqualTo("image/jpeg");
    assertThrows(IllegalArgumentException.class, () -> FlickrPhotosExporter.toMimeType("png"));
  }

  @Test
  public void exportAlbumInitial() throws FlickrException {
    // set up auth, flickr service
    when(user.getId()).thenReturn("userId");
    when(authInterface.checkToken(any(Token.class))).thenReturn(auth);
    when(flickr.getPhotosetsInterface()).thenReturn(photosetsInterface);
    when(flickr.getPhotosInterface()).thenReturn(photosInterface);
    when(flickr.getAuthInterface()).thenReturn(authInterface);

    // setup photoset
    Photoset photoset = initializePhotoset("photosetId", "title", "description");

    // setup photoset list (aka album view)
    int page = 1;
    Photosets photosetsList = new Photosets();
    photosetsList.setPage(page);
    photosetsList.setPages(page + 1);
    photosetsList.setPhotosets(Collections.singletonList(photoset));
    when(photosetsInterface.getList(anyString(), anyInt(), anyInt(), anyString()))
        .thenReturn(photosetsList);

    // run test
    FlickrPhotosExporter exporter = new FlickrPhotosExporter(flickr);
    AuthData authData = new TokenSecretAuthData("token", "secret");
    ExportResult<PhotosContainerResource> result = exporter.export(authData);

    // make sure album and photo information is correct
    assertThat(result.getExportedData().getPhotos()).isEmpty();
    Collection<PhotoAlbum> albums = result.getExportedData().getAlbums();
    assertThat(albums.size()).isEqualTo(1);
    assertThat(albums).containsExactly(new PhotoAlbum("photosetId", "title", "description"));

    // check continuation information
    ContinuationData continuationData = (ContinuationData) result.getContinuationData();
    assertThat(continuationData.getPaginationData()).isInstanceOf(IntPaginationToken.class);
    assertThat(((IntPaginationToken) continuationData.getPaginationData()).getStart())
        .isEqualTo(page + 1);

    Collection<? extends ContainerResource> subResources = continuationData.getContainerResources();
    assertThat(subResources.size()).isEqualTo(1);
    assertThat(subResources).containsExactly(new IdOnlyContainerResource("photosetId"));
  }

  @Test
  public void exportPhotosFromPhotoset() throws FlickrException {
    // set up auth, flickr service
    when(user.getId()).thenReturn("userId");
    when(authInterface.checkToken(any(Token.class))).thenReturn(auth);
    when(flickr.getPhotosetsInterface()).thenReturn(photosetsInterface);
    when(flickr.getPhotosInterface()).thenReturn(photosInterface);
    when(flickr.getAuthInterface()).thenReturn(authInterface);

    // getting photos from a set with id photosetsId and page 1
    int page = 1;
    String photosetsId = "photosetsId";
    ExportInformation exportInformation =
        new ExportInformation(null, new IdOnlyContainerResource(photosetsId));

    // make lots of photos and add them to PhotoList (also adding pagination information)
    int numPhotos = 4;
    PhotoList<Photo> photosList = new PhotoList<>();
    for (int i = 0; i < numPhotos; i++) {
      photosList.add(initializePhoto("title" + 1, "url" + i, "description" + i));
    }
    photosList.setPage(page);
    photosList.setPages(page + 1);

    when(photosetsInterface.getPhotos(anyString(), anySet(), anyInt(), anyInt(), anyInt()))
        .thenReturn(photosList);

    // run test
    FlickrPhotosExporter exporter = new FlickrPhotosExporter(flickr);
    ExportResult<PhotosContainerResource> result =
        exporter.export(new TokenSecretAuthData("token", "secret"), exportInformation);
    assertThat(result.getExportedData().getPhotos().size()).isEqualTo(numPhotos);
    assertThat(result.getExportedData().getAlbums()).isEmpty();

    ContinuationData continuationData = (ContinuationData) result.getContinuationData();
    assertThat(continuationData.getContainerResources()).isEmpty();
    assertThat(((IntPaginationToken) continuationData.getPaginationData()).getStart())
        .isEqualTo(page + 1);
  }
}
