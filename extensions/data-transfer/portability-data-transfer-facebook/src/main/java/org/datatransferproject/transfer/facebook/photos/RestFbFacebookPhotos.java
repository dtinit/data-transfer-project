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

import com.restfb.DefaultFacebookClient;
import com.restfb.Parameter;
import com.restfb.Version;
import com.restfb.types.Album;
import com.restfb.types.Photo;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.util.List;

public class RestFbFacebookPhotos implements FacebookPhotosInterface {
  private DefaultFacebookClient client;

  RestFbFacebookPhotos(TokensAndUrlAuthData authData, AppCredentials appCredentials) {
    client =
        new DefaultFacebookClient(
            authData.getAccessToken(), appCredentials.getSecret(), Version.VERSION_3_0);
  }

  public Iterable<List<Album>> getAlbums() {
    return client.fetchConnection(
        "me/albums", Album.class, Parameter.with("fields", "description,name"));
  }

  public Iterable<List<Photo>> getPhotos(String albumId) {
    return client.fetchConnection(
        String.format("%s/photos", albumId), Photo.class, Parameter.with("fields", "name,images"));
  }
}
