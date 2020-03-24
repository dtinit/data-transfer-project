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

package org.datatransferproject.datatransfer.flickr.photos;

import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.Size;
import com.flickr4java.flickr.photosets.Photoset;
import java.util.Collections;
import java.util.UUID;

class FlickrTestUtils {

  public static Photoset initializePhotoset(String id, String title, String description) {
    Photoset photoset = new Photoset();
    photoset.setId(id);
    photoset.setTitle(title);
    photoset.setDescription(description);
    return photoset;
  }

  public static Photo initializePhoto(
      String title, String url, String description, String mediaType) {
    Photo photo = new Photo();
    photo.setTitle(title);
    photo.setId(UUID.randomUUID().toString());
    photo.setDescription(description);
    photo.setOriginalFormat(mediaType);
    Size size = new Size();
    size.setSource(url);
    size.setLabel(Size.ORIGINAL);
    photo.setSizes(Collections.singletonList(size));
    return photo;
  }
}
