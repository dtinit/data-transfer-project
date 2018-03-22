package org.dataportabilityproject.datatransfer.flickr.photos;

import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.Size;
import com.flickr4java.flickr.photosets.Photoset;

import java.util.Collections;

public class FlickrTestUtils {

  public static Photoset initializePhotoset(String id, String title, String description) {
    Photoset photoset = new Photoset();
    photoset.setId(id);
    photoset.setTitle(title);
    photoset.setDescription(description);
    return photoset;
  }

  public static Photo initializePhoto(String title, String url, String description, String mediaType) {
    Photo photo = new Photo();
    photo.setTitle(title);
    photo.setDescription(description);
    photo.setOriginalFormat(mediaType);
    Size size = new Size();
    size.setSource(url);
    size.setLabel(Size.ORIGINAL);
    photo.setSizes(Collections.singletonList(size));
    return photo;
  }

}
