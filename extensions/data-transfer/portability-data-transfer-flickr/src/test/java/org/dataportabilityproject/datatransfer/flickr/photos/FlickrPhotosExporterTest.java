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

import static org.mockito.Mockito.mock;
import static com.google.common.truth.Truth.assertThat;
import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.auth.Permission;
import com.flickr4java.flickr.people.User;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.Size;
import java.util.Collections;
import org.dataportabilityproject.types.transfer.models.photos.PhotoAlbum;
import org.dataportabilityproject.types.transfer.models.photos.PhotoModel;
import org.junit.Test;
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
  private final Logger logger = LoggerFactory.getLogger(FlickrPhotosExporterTest.class);

  private Flickr flickr = mock(Flickr.class);

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

}