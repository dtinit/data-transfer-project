/*
Copyright 2018 The Data-Portability Project Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.dataportabilityproject.shared;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.dataportabilityproject.serviceProviders.flickr.FlickrPhotoService;

/**
 * A class containing stream getters for images. Should be used by photo services to get image
 * streams.
 *
 * <p>Initially created to make testing easier.
 */
public class ImageStreamProvider {

  /**
   * Gets an input stream to an image, given its URL. Used by {@link FlickrPhotoService} to upload
   * the image.
   */
  public BufferedInputStream get(String urlStr) throws IOException {
    URL url = new URL(urlStr);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.connect();
    return new BufferedInputStream(conn.getInputStream());
  }
}
