/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.serviceProviders.instagram;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import org.dataportabilityproject.dataModels.ExportInformation;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.photos.PhotoAlbum;
import org.dataportabilityproject.dataModels.photos.PhotoModel;
import org.dataportabilityproject.dataModels.photos.PhotosModelWrapper;
import org.dataportabilityproject.serviceProviders.instagram.model.MediaFeedData;
import org.dataportabilityproject.serviceProviders.instagram.model.MediaResponse;

final class InstagramPhotoService implements Exporter<PhotosModelWrapper> {

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private static final String FAKE_ALBUM_ID = "instagramAlbum";

  private final HttpTransport httpTransport;
  private final InstagramOauthData authData;

  InstagramPhotoService(InstagramOauthData authData) {
    try {
      this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
      this.authData = authData;
    } catch (IOException | GeneralSecurityException e) {
      throw new IllegalStateException("Problem getting token", e);
    }
  }

  @Override
  public PhotosModelWrapper export(ExportInformation exportInformation) throws IOException {
    MediaResponse response = makeRequest(
        "https://api.instagram.com/v1/users/self/media/recent",
        MediaResponse.class);

    List<PhotoModel> photos = new ArrayList<>();

    // TODO: check out paging.
    for (MediaFeedData photo : response.getData()) {
      // TODO json mapping is broken.
      String photoId = photo.getId();
      String url = photo.getImages().getStandardResolution().getUrl();
      String text = (photo.getCaption() != null) ? photo.getCaption().getText() : null;
      photos.add(new PhotoModel("Instagram photo: " + photoId,
          url,
          text,
          null,
          FAKE_ALBUM_ID));
    }

    List<PhotoAlbum> albums = new ArrayList<>();

    if (!photos.isEmpty() && !exportInformation.getPaginationInformation().isPresent()) {
      albums.add(new PhotoAlbum(
          FAKE_ALBUM_ID,
          "Imported Instagram Photos",
          "Photos imported from instagram"));
    }

    return new PhotosModelWrapper(albums, photos, null);
  }

  private <T> T makeRequest(String url, Class<T> clazz) throws IOException {
    HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
    HttpRequest getRequest = requestFactory.buildGetRequest(
        new GenericUrl(url + "?access_token=" + authData.accessToken()));
    HttpResponse response = getRequest
        .execute();
    int statusCode = response.getStatusCode();
    if (statusCode != 200) {
      throw new IOException("Bad status code: " + statusCode + " error: "
          + response.getStatusMessage());
    }
    String result = CharStreams.toString(new InputStreamReader(
        response.getContent(), Charsets.UTF_8));
    return MAPPER.readValue(result, clazz);
  }
}
