/*
 * Copyright 2018 The Data Transfer Project Authors.
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
package org.dataportabilityproject.datatransfer.google.photos;

import com.google.api.client.auth.oauth2.Credential;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.MediaContent;
import com.google.gdata.data.photos.AlbumFeed;
import com.google.gdata.data.photos.GphotoEntry;
import com.google.gdata.data.photos.UserFeed;
import com.google.gdata.util.ServiceException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.dataportabilityproject.datatransfer.google.common.GoogleCredentialFactory;
import org.dataportabilityproject.datatransfer.google.common.GoogleStaticObjects;
import org.dataportabilityproject.spi.transfer.provider.ExportResult;
import org.dataportabilityproject.spi.transfer.provider.ExportResult.ResultType;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.types.ContinuationData;
import org.dataportabilityproject.spi.transfer.types.ExportInformation;
import org.dataportabilityproject.spi.transfer.types.IdOnlyContainerResource;
import org.dataportabilityproject.spi.transfer.types.PaginationData;
import org.dataportabilityproject.spi.transfer.types.StringPaginationToken;
import org.dataportabilityproject.types.transfer.auth.TokensAndUrlAuthData;
import org.dataportabilityproject.types.transfer.models.photos.PhotoAlbum;
import org.dataportabilityproject.types.transfer.models.photos.PhotoModel;
import org.dataportabilityproject.types.transfer.models.photos.PhotosContainerResource;

public class GooglePhotosExporter
    implements Exporter<TokensAndUrlAuthData, PhotosContainerResource> {

  static final String ALBUM_TOKEN_PREFIX = "album:";
  static final String PHOTO_TOKEN_PREFIX = "photo:";

  // TODO(olsona): figure out optimal value here
  static final int MAX_RESULTS_DEFAULT = 100;

  static final String URL_ALBUM_FEED_FORMAT =
      "https://picasaweb.google.com/data/feed/api/user/default?kind=album&start-index=%d&max-results=%d";
  // imgmax=d gets the original image as per
  // https://developers.google.com/picasa-web/docs/3.0/reference
  static final String URL_PHOTO_FEED_FORMAT =
      "https://picasaweb.google.com/data/feed/api/user/default/albumid/%s?imgmax=d&start-index=%s&max-results=%d";

  private final GoogleCredentialFactory credentialFactory;
  private final int maxResults;
  private volatile PicasawebService photosService;

  public GooglePhotosExporter(GoogleCredentialFactory credentialFactory) {
    this(credentialFactory, null, MAX_RESULTS_DEFAULT);
  }

  @VisibleForTesting
  GooglePhotosExporter(GoogleCredentialFactory credentialFactory, PicasawebService photosService,
      int maxResults) {
    this.credentialFactory = credentialFactory;
    this.photosService = photosService;
    this.maxResults = maxResults;
  }

  @Override
  public ExportResult<PhotosContainerResource> export(UUID jobId, TokensAndUrlAuthData authData) {
    return exportAlbums(authData, Optional.empty());
  }

  @Override
  public ExportResult<PhotosContainerResource> export(
      UUID jobId, TokensAndUrlAuthData authData, ExportInformation exportInformation) {
    StringPaginationToken paginationToken =
        (StringPaginationToken) exportInformation.getPaginationData();
    IdOnlyContainerResource idOnlyContainerResource =
        (IdOnlyContainerResource) exportInformation.getContainerResource();

    if (idOnlyContainerResource != null) {
      // export more photos
      return exportPhotos(
          authData, idOnlyContainerResource.getId(), Optional.ofNullable(paginationToken));
    } else {
      // export more albums if there are no more photos
      return exportAlbums(authData, Optional.ofNullable(paginationToken));
    }
  }

  private ExportResult<PhotosContainerResource> exportAlbums(
      TokensAndUrlAuthData authData, Optional<PaginationData> paginationData) {
    int startItem = 1;
    if (paginationData.isPresent()) {
      String token = ((StringPaginationToken) paginationData.get()).getToken();
      Preconditions.checkArgument(
          token.startsWith(ALBUM_TOKEN_PREFIX), "Invalid pagination token " + token);
      startItem = Integer.parseInt(token.substring(ALBUM_TOKEN_PREFIX.length()));
    }

    URL albumUrl;
    UserFeed albumFeed;

    try {
      albumUrl = new URL(String.format(URL_ALBUM_FEED_FORMAT, startItem, maxResults));
      albumFeed = getOrCreatePhotosService(authData).getFeed(albumUrl, UserFeed.class);
    } catch (ServiceException | IOException e) {
      return new ExportResult<>(ResultType.ERROR, e.getMessage());
    }

    PaginationData nextPageData = null;
    if (albumFeed.getEntries().size() == maxResults) {
      int nextPageStart = startItem + maxResults;
      nextPageData = new StringPaginationToken(ALBUM_TOKEN_PREFIX + nextPageStart);
    }
    ContinuationData continuationData = new ContinuationData(nextPageData);

    List<PhotoAlbum> albums = new ArrayList<>(albumFeed.getEntries().size());

    for (GphotoEntry googleAlbum : albumFeed.getEntries()) {
      // Add album info to list so album can be recreated later
      albums.add(
          new PhotoAlbum(
              googleAlbum.getGphotoId(),
              googleAlbum.getTitle().getPlainText(),
              googleAlbum.getDescription().getPlainText()));

      // Add album id to continuation data
      continuationData.addContainerResource(new IdOnlyContainerResource(googleAlbum.getGphotoId()));
    }

    ResultType resultType = ResultType.CONTINUE;
    if (nextPageData == null || continuationData.getContainerResources().isEmpty()) {
      resultType = ResultType.END;
    }
    PhotosContainerResource containerResource = new PhotosContainerResource(albums, null);
    return new ExportResult<>(resultType, containerResource, continuationData);
  }

  private ExportResult<PhotosContainerResource> exportPhotos(
      TokensAndUrlAuthData authData, String albumId, Optional<PaginationData> paginationData) {

    int startItem = 1;
    if (paginationData.isPresent()) {
      String token = ((StringPaginationToken) paginationData.get()).getToken();
      Preconditions.checkArgument(
          token.startsWith(PHOTO_TOKEN_PREFIX), "Invalid pagination token " + token);
      startItem = Integer.parseInt(token.substring(PHOTO_TOKEN_PREFIX.length()));
    }

    URL photosUrl;
    AlbumFeed photoFeed;

    try {
      photosUrl = new URL(String.format(URL_PHOTO_FEED_FORMAT, albumId, startItem, maxResults));
      photoFeed = getOrCreatePhotosService(authData).getFeed(photosUrl, AlbumFeed.class);

    } catch (ServiceException | IOException e) {
      return new ExportResult<>(ResultType.ERROR, e.getMessage());
    }

    PaginationData nextPageData = null;
    if (photoFeed.getEntries().size() == maxResults) {
      int nextPageStart = startItem + maxResults;
      nextPageData = new StringPaginationToken(PHOTO_TOKEN_PREFIX + nextPageStart);
    }
    ContinuationData continuationData = new ContinuationData(nextPageData);

    List<PhotoModel> photos = new ArrayList<>(photoFeed.getEntries().size());
    for (GphotoEntry photo : photoFeed.getEntries()) {
      MediaContent mediaContent = (MediaContent) photo.getContent();
      photos.add(
          new PhotoModel(
              photo.getTitle().getPlainText(),
              mediaContent.getUri(),
              photo.getDescription().getPlainText(),
              mediaContent.getMimeType().getMediaType(),
              albumId));
    }

    PhotosContainerResource containerResource = new PhotosContainerResource(null, photos);

    ResultType resultType = ResultType.CONTINUE;
    if (nextPageData == null) {
      resultType = ResultType.END;
    }
    return new ExportResult<>(resultType, containerResource, continuationData);
  }

  private PicasawebService getOrCreatePhotosService(TokensAndUrlAuthData authData) {
    return photosService == null ? makePhotosService(authData) : photosService;
  }

  private synchronized PicasawebService makePhotosService(TokensAndUrlAuthData authData) {
    Credential credential = credentialFactory.createCredential(authData);
    PicasawebService service = new PicasawebService(GoogleStaticObjects.APP_NAME);
    service.setOAuth2Credentials(credential);
    return service;
  }
}
