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

package org.datatransferproject.transfer.twitter;

import com.google.api.client.http.InputStreamContent;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.ImportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;
import org.datatransferproject.types.transfer.models.photos.PhotoModel;
import org.datatransferproject.types.transfer.models.photos.PhotosContainerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;

final class TwitterPhotosImporter
    implements Importer<TokenSecretAuthData, PhotosContainerResource> {
  private final Logger logger = LoggerFactory.getLogger(TwitterPhotosImporter.class);
  private final AppCredentials appCredentials;

  public TwitterPhotosImporter(AppCredentials appCredentials) {
    this.appCredentials = appCredentials;
  }

  @Override
  public ImportResult importItem(UUID jobId, TokenSecretAuthData authData,
      PhotosContainerResource data) {
    Twitter twitterApi = TwitterApiWrapper.getInstance(appCredentials, authData);
    // Twitter doesn't support an 'Albums' concept, so that information is just lost.

    for (PhotoModel image : data.getPhotos()) {
      try {
        StatusUpdate update = new StatusUpdate(image.getDescription());
        InputStreamContent content =
            new InputStreamContent(null, getImageAsStream(image.getFetchableUrl()));
        update.media(image.getTitle(), content.getInputStream());

        twitterApi.tweets().updateStatus(update);
      } catch (IOException | TwitterException e) {
        logger.warn("problem importing twitter photo", e);
        return new ImportResult(e);
      }
    }
    return new ImportResult(ResultType.OK);
  }

  private InputStream getImageAsStream(String urlStr) throws IOException {
    URL url = new URL(urlStr);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.connect();
    return conn.getInputStream();
  }
}
