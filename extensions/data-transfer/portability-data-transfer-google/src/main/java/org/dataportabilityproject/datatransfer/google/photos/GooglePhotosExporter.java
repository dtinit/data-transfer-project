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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.MediaContent;
import com.google.gdata.data.photos.AlbumFeed;
import com.google.gdata.data.photos.GphotoEntry;
import com.google.gdata.data.photos.UserFeed;
import com.google.gdata.util.ServiceException;
import org.dataportabilityproject.datatransfer.google.common.GoogleCredentialFactory;
import org.dataportabilityproject.datatransfer.google.common.GoogleStaticObjects;
import org.dataportabilityproject.datatransfer.google.photos.model.AlbumListResponse;
import org.dataportabilityproject.datatransfer.google.photos.model.GoogleAlbum;
import org.dataportabilityproject.datatransfer.google.photos.model.GoogleMediaItem;
import org.dataportabilityproject.datatransfer.google.photos.model.MediaItemSearchResponse;
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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GooglePhotosExporter
    implements Exporter<TokensAndUrlAuthData, PhotosContainerResource> {

  static final String ALBUM_TOKEN_PREFIX = "album:";
  static final String PHOTO_TOKEN_PREFIX = "media:";

  private GooglePhotosInterface photosInterface;

  public GooglePhotosExporter() {
    this(null);
  }

  @VisibleForTesting
  GooglePhotosExporter(GooglePhotosInterface photosInterface) {
    this.photosInterface = photosInterface;
  }

  @Override
  public ExportResult<PhotosContainerResource> export(
      UUID jobId, TokensAndUrlAuthData authData, Optional<ExportInformation> exportInformation) {
    if (!exportInformation.isPresent()) {
      return exportAlbums(authData, Optional.empty());
    } else {
      StringPaginationToken paginationToken =
          (StringPaginationToken) exportInformation.get().getPaginationData();
      IdOnlyContainerResource idOnlyContainerResource =
          (IdOnlyContainerResource) exportInformation.get().getContainerResource();

      if (idOnlyContainerResource != null) {
        // export more photos
        return exportPhotos(
            authData, idOnlyContainerResource.getId(), Optional.ofNullable(paginationToken));
      } else {
        // export more albums if there are no more photos
        return exportAlbums(authData, Optional.ofNullable(paginationToken));
      }
    }
  }

  private ExportResult<PhotosContainerResource> exportAlbums(
      TokensAndUrlAuthData authData, Optional<PaginationData> paginationData) {
    Optional<String> paginationToken = Optional.empty();
    if (paginationData.isPresent()) {
      String token = ((StringPaginationToken) paginationData.get()).getToken();
      Preconditions.checkArgument(
          token.startsWith(ALBUM_TOKEN_PREFIX), "Invalid pagination token " + token);
      paginationToken = Optional.of(token.substring(ALBUM_TOKEN_PREFIX.length()));
    }

    AlbumListResponse albumListResponse;

    try {
      albumListResponse = getOrCreatePhotosInterface(authData).listAlbums(paginationToken);
    } catch (IOException e) {
      return new ExportResult<>(ResultType.ERROR, e.getMessage());
    }

    PaginationData nextPageData = null;
    GoogleAlbum[] googleAlbums = albumListResponse.getAlbums();
    if (!Strings.isNullOrEmpty(albumListResponse.getNextPageToken())) {
      nextPageData = new StringPaginationToken(ALBUM_TOKEN_PREFIX + albumListResponse.getNextPageToken());
    }

    ContinuationData continuationData = new ContinuationData(nextPageData);
    List<PhotoAlbum> albums = new ArrayList<>(googleAlbums.length);

    for (int i = 0; i < googleAlbums.length; i++) {
      GoogleAlbum googleAlbum = googleAlbums[i];
      // Add album info to list so album can be recreated later
      albums.add(
          new PhotoAlbum(
              googleAlbum.getId(),
              googleAlbum.getTitle(),
              null));

      // Add album id to continuation data
      continuationData.addContainerResource(new IdOnlyContainerResource(googleAlbum.getId()));
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
    Optional<String> paginationToken = Optional.empty();
    if (paginationData.isPresent()) {
      String token = ((StringPaginationToken) paginationData.get()).getToken();
      Preconditions.checkArgument(
          token.startsWith(PHOTO_TOKEN_PREFIX), "Invalid pagination token " + token);
      paginationToken = Optional.of(token.substring(PHOTO_TOKEN_PREFIX.length()));
    }

    MediaItemSearchResponse mediaItemSearchResponse;

    try {
      mediaItemSearchResponse = getOrCreatePhotosInterface(authData).listAlbumContents(albumId, paginationToken);
    } catch (IOException e) {
      return new ExportResult<>(ResultType.ERROR, e.getMessage());
    }

    PaginationData nextPageData = null;
    GoogleMediaItem[] googleMediaItems = mediaItemSearchResponse.getMediaItems();
    if (!Strings.isNullOrEmpty(mediaItemSearchResponse.getNextPageToken())) {
      nextPageData = new StringPaginationToken(PHOTO_TOKEN_PREFIX + mediaItemSearchResponse.getNextPageToken());
    }
    ContinuationData continuationData = new ContinuationData(nextPageData);

    List<PhotoModel> photos = new ArrayList<>(googleMediaItems.length);
    for (int i = 0; i < googleMediaItems.length; i++) {
      GoogleMediaItem mediaItem = googleMediaItems[i];
      if (mediaItem.getMediaMetadata().getPhoto() != null) {
        // TODO: address videos later on
        photos.add(
            new PhotoModel(
                "", // TODO: no title?
                mediaItem.getProductUrl(),  // TODO: check this
                mediaItem.getDescription(),
                mediaItem.getMimeType(),
                mediaItem.getId(),
                albumId));
      }
    }

    PhotosContainerResource containerResource = new PhotosContainerResource(null, photos);

    ResultType resultType = ResultType.CONTINUE;
    if (nextPageData == null) {
      resultType = ResultType.END;
    }
    return new ExportResult<>(resultType, containerResource, continuationData);
  }

  private GooglePhotosInterface getOrCreatePhotosInterface(TokensAndUrlAuthData authData) {
    return photosInterface == null ? makePhotosInterface(authData) : photosInterface;
  }

  private synchronized GooglePhotosInterface makePhotosInterface(TokensAndUrlAuthData authData) {
    GooglePhotosInterface photosInterface = new GooglePhotosInterface(authData);
    return photosInterface;
  }
}
