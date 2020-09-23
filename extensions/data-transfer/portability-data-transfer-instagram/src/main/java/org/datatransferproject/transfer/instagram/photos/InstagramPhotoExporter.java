/*
 * Copyright 2020 The Data Transfer Project Authors.
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

import com.google.api.client.http.HttpTransport;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.transfer.instagram.common.InstagramApiClient;
import org.datatransferproject.transfer.instagram.model.Child;
import org.datatransferproject.transfer.instagram.model.MediaFeedData;
import org.datatransferproject.transfer.instagram.model.MediaResponse;
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

  public static final String DEFAULT_ALBUM_ID = "Instagram Photos";
  public static final String DEFAULT_ALBUM_NAME = "Imported Instagram Photos";
  public static final String DEFAULT_ALBUM_DESCRIPTION = "Photos importer from Instagram";

  private final HttpTransport httpTransport;
  private final Monitor monitor;
  private final AppCredentials appCredentials;
  private InstagramApiClient instagramApiClient;

  public InstagramPhotoExporter(
      HttpTransport httpTransport, Monitor monitor, AppCredentials appCredentials) {
    this.httpTransport = httpTransport;
    this.monitor = monitor;
    this.appCredentials = appCredentials;
  }

  @VisibleForTesting
  InstagramPhotoExporter(
      HttpTransport httpTransport,
      Monitor monitor,
      AppCredentials appCredentials,
      InstagramApiClient instagramApiClient) {
    this.httpTransport = httpTransport;
    this.monitor = monitor;
    this.appCredentials = appCredentials;
    this.instagramApiClient = instagramApiClient;
  }

  @Override
  public ExportResult<PhotosContainerResource> export(
      UUID jobId, TokensAndUrlAuthData authData, Optional<ExportInformation> exportInformation)
      throws IOException {
    if (this.instagramApiClient == null) {
      this.instagramApiClient =
          new InstagramApiClient(this.httpTransport, this.monitor, this.appCredentials, authData);
    }

    if (exportInformation.isPresent()) {
      return exportPhotos(exportInformation.get().getPaginationData());
    } else {
      return exportAlbum();
    }
  }

  private ExportResult<PhotosContainerResource> exportAlbum() {
    List<PhotoAlbum> defaultAlbums =
        Arrays.asList(
            new PhotoAlbum(DEFAULT_ALBUM_ID, DEFAULT_ALBUM_NAME, DEFAULT_ALBUM_DESCRIPTION));
    return new ExportResult<>(
        ResultType.CONTINUE,
        new PhotosContainerResource(defaultAlbums, null),
        new ContinuationData(new StringPaginationToken(InstagramApiClient.getMediaBaseUrl())));
  }

  private PhotoModel createPhotoModel(
      String id, String mediaUrl, String caption, Date publishDate) {
    return new PhotoModel(
        String.format("%s.jpg", id),
        mediaUrl,
        caption,
        "image/jpg",
        id,
        DEFAULT_ALBUM_ID,
        false,
        publishDate);
  }

  private void addPhotosInCarouselAlbum(ArrayList<PhotoModel> photos, MediaFeedData data) {
    for (Child photo : data.getChildren().getData()) {
      if (photo.getMediaType().equals("IMAGE")) {
        photos.add(
            createPhotoModel(
                photo.getId(), photo.getMediaUrl(), data.getCaption(), data.getPublishDate()));
      }
    }
  }

  private ExportResult<PhotosContainerResource> exportPhotos(PaginationData pageData)
      throws IOException {
    Preconditions.checkNotNull(pageData);
    MediaResponse response;
    try {
      StringPaginationToken paginationToken = (StringPaginationToken) pageData;
      String url = paginationToken.getToken();
      response = instagramApiClient.makeRequest(url);
    } catch (IOException e) {
      monitor.info(() -> "Failed to get photos from instagram API", e);
      throw e;
    }

    ArrayList<PhotoModel> photos = new ArrayList<>();

    for (MediaFeedData data : response.getData()) {
      if (data.getMediaType().equals("CAROUSEL_ALBUM")) {
        addPhotosInCarouselAlbum(photos, data);
        continue;
      }

      if (data.getMediaType().equals("IMAGE")) {
        photos.add(
            createPhotoModel(
                data.getId(), data.getMediaUrl(), data.getCaption(), data.getPublishDate()));
        continue;
      }
    }

    String url = InstagramApiClient.getContinuationUrl(response);
    if (url == null) {
      return new ExportResult<>(ResultType.END, new PhotosContainerResource(null, photos));
    }

    return new ExportResult<>(
        ResultType.CONTINUE,
        new PhotosContainerResource(null, photos),
        new ContinuationData(new StringPaginationToken(url)));
  }
}
