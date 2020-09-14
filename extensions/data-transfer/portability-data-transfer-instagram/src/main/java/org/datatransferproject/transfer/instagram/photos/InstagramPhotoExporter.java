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
package org.datatransferproject.transfer.instagram.photos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.PaginationData;
import org.datatransferproject.transfer.instagram.photos.model.MediaFeedData;
import org.datatransferproject.transfer.instagram.photos.model.MediaResponse;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class InstagramPhotoExporter implements
    Exporter<TokensAndUrlAuthData, PhotosContainerResource> {

  private static final String MEDIA_URL = "https://graph.instagram.com/me/media?fields=id,media_url,media_type,caption,timestamp";
  private static final String DEFAULT_ALBUM_ID = "Instagram Photos";

  private final ObjectMapper objectMapper;
  private final HttpTransport httpTransport;
  private final Monitor monitor;

  public InstagramPhotoExporter(ObjectMapper objectMapper, HttpTransport httpTransport, Monitor monitor) {
    this.objectMapper = objectMapper;
    this.httpTransport = httpTransport;
    this.monitor = monitor;
  }

  @Override
  public ExportResult<PhotosContainerResource> export(UUID jobId, TokensAndUrlAuthData authData,
      Optional<ExportInformation> exportInformation) throws IOException {
    if (exportInformation.isPresent()) {
      return exportPhotos(authData, Optional.ofNullable(exportInformation.get().getPaginationData()));
    } else {
      return exportPhotos(authData, Optional.empty());
    }
  }

  private ExportResult<PhotosContainerResource> exportPhotos(TokensAndUrlAuthData authData,
      Optional<PaginationData> pageData) throws IOException{
    Preconditions.checkNotNull(authData);
    MediaResponse response;
    try {
      response = makeRequest(MEDIA_URL, MediaResponse.class, authData);
    } catch (IOException e) {
      monitor.info(() -> "Failed to get photos from instagram API", e);
      throw e;
    }

    ArrayList<PhotoModel> photos = new ArrayList<>();

    // TODO: check out paging.
    for (MediaFeedData photo : response.getData()) {
      if (!photo.getMediaType().equals("IMAGE")) {
        continue;
      }

      photos.add(new PhotoModel(
          String.format("%s.jpg", photo.getId()),
          photo.getMediaUrl(),
          photo.getCaption(),
          "image/jpg",
          photo.getId(),
          DEFAULT_ALBUM_ID,
          false,
          photo.getPublishDate()));
    }

    List<PhotoAlbum> albums = new ArrayList<>();

    if (!photos.isEmpty() && !pageData.isPresent()) {
      albums.add(
          new PhotoAlbum(
              DEFAULT_ALBUM_ID, "Imported Instagram Photos", "Photos imported from instagram"));
    }

    return new ExportResult<>(ResultType.END, new PhotosContainerResource(albums, photos));
  }

  private <T> T makeRequest(String url, Class<T> clazz, TokensAndUrlAuthData authData)
      throws IOException {
    HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
    HttpRequest getRequest =
        requestFactory.buildGetRequest(
            new GenericUrl(url + "&access_token=" + authData.getAccessToken()));
    HttpResponse response = getRequest.execute();
    int statusCode = response.getStatusCode();
    if (statusCode != 200) {
      throw new IOException(
          "Bad status code: " + statusCode + " error: " + response.getStatusMessage());
    }
    String result =
        CharStreams.toString(new InputStreamReader(response.getContent(), Charsets.UTF_8));
    return objectMapper.readValue(result, clazz);
  }
}
