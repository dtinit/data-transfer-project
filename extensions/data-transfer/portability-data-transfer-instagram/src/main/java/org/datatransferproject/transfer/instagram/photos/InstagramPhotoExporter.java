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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.http.client.utils.URIBuilder;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.transfer.instagram.photos.model.ExchangeTokenResponse;
import org.datatransferproject.transfer.instagram.photos.model.MediaFeedData;
import org.datatransferproject.transfer.instagram.photos.model.MediaResponse;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.PaginationData;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class InstagramPhotoExporter
    implements Exporter<TokensAndUrlAuthData, PhotosContainerResource> {

  private static final String MEDIA_BASE_URL =
      "https://graph.instagram.com/me/media?fields=id,media_url,media_type,caption,timestamp";
  private static final String ACCESS_TOKEN_BASE_URL =
      "https://graph.instagram.com/access_token?grant_type=ig_exchange_token";
  private static final String DEFAULT_ALBUM_ID = "Instagram Photos";

  private final ObjectMapper objectMapper;
  private final HttpTransport httpTransport;
  private final Monitor monitor;
  private final AppCredentials appCredentials;
  private String accessToken;

  public InstagramPhotoExporter(
      ObjectMapper objectMapper,
      HttpTransport httpTransport,
      Monitor monitor,
      AppCredentials appCredentials) {
    this.objectMapper = objectMapper;
    this.httpTransport = httpTransport;
    this.monitor = monitor;
    this.appCredentials = appCredentials;
  }

  @Override
  public ExportResult<PhotosContainerResource> export(
      UUID jobId, TokensAndUrlAuthData authData, Optional<ExportInformation> exportInformation)
      throws IOException {
    if (this.accessToken == null) {
      this.accessToken = getLongLivedAccessToken(authData);
    }

    if (exportInformation.isPresent()) {
      return exportPhotos(exportInformation.get().getPaginationData());
    } else {
      return exportAlbum();
    }
  }

  private String getLongLivedAccessToken(TokensAndUrlAuthData authData) throws IOException {
    try {
      String url =
          new URIBuilder(ACCESS_TOKEN_BASE_URL)
              .setParameter("client_secret", this.appCredentials.getSecret())
              .build()
              .toString();
      ExchangeTokenResponse response =
          makeRequest(url, ExchangeTokenResponse.class, authData.getAccessToken());
      return response.getAccessToken();
    } catch (URISyntaxException e) {
      throw new IOException("Failed to produce instagram long lived token exchange url.", e);
    }
  }

  private ExportResult<PhotosContainerResource> exportAlbum() {
    List<PhotoAlbum> defaultAlbums =
        Arrays.asList(
            new PhotoAlbum(
                DEFAULT_ALBUM_ID, "Imported Instagram Photos", "Photos imported from instagram"));
    return new ExportResult<>(
        ResultType.CONTINUE,
        new PhotosContainerResource(defaultAlbums, null),
        createContinuationData(MEDIA_BASE_URL));
  }

  private ContinuationData createContinuationData(String url) {
    PaginationData nextPageData = new StringPaginationToken(url);
    return new ContinuationData(nextPageData);
  }

  private ExportResult<PhotosContainerResource> exportPhotos(PaginationData pageData) throws IOException {
    Preconditions.checkNotNull(pageData);
    MediaResponse response;
    try {
      StringPaginationToken paginationToken = (StringPaginationToken) pageData;
      String url = paginationToken.getToken();
      response = makeRequest(url, MediaResponse.class, this.accessToken);
    } catch (IOException e) {
      monitor.info(() -> "Failed to get photos from instagram API", e);
      throw e;
    }

    ArrayList<PhotoModel> photos = new ArrayList<>();

    for (MediaFeedData photo : response.getData()) {
      if (!photo.getMediaType().equals("IMAGE")) {
        continue;
      }

      photos.add(
          new PhotoModel(
              String.format("%s.jpg", photo.getId()),
              photo.getMediaUrl(),
              photo.getCaption(),
              "image/jpg",
              photo.getId(),
              DEFAULT_ALBUM_ID,
              false,
              photo.getPublishDate()));
    }

    String next = response.getPaging().getNext();
    if (next != null && !next.isEmpty()) {
      try {
        String after = response.getPaging().getCursors().getAfter();
        String url = new URIBuilder(MEDIA_BASE_URL).setParameter("after", after).build().toString();
        return new ExportResult<>(
            ResultType.CONTINUE,
            new PhotosContainerResource(null, photos),
            createContinuationData(url));
      } catch (URISyntaxException e) {
        throw new IOException("Failed to produce instagram paging url.", e);
      }
    }

    return new ExportResult<>(ResultType.END, new PhotosContainerResource(null, photos));
  }

  private <T> T makeRequest(String url, Class<T> clazz, String accessToken) throws IOException {
    String fullURL;
    try {
      fullURL = new URIBuilder(url).setParameter("access_token", accessToken).build().toString();
    } catch (URISyntaxException e) {
      throw new IOException("Failed to produce instagram api url.", e);
    }

    HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
    HttpRequest getRequest = requestFactory.buildGetRequest(new GenericUrl(fullURL));
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
