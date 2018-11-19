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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.restfb.Connection;
import com.restfb.types.Album;
import com.restfb.types.Photo;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.PaginationData;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.ContainerResource;
import org.datatransferproject.types.common.models.IdOnlyContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FacebookPhotosExporter
    implements Exporter<TokensAndUrlAuthData, PhotosContainerResource> {
  private static final String ALBUM_TOKEN_PREFIX = "album:";
  static final String PHOTO_TOKEN_PREFIX = "media:";

  private AppCredentials appCredentials;
  private FacebookPhotosInterface photosInterface;

  public FacebookPhotosExporter(AppCredentials appCredentials) {
    this.appCredentials = appCredentials;
  }

  @VisibleForTesting
  FacebookPhotosExporter(AppCredentials appCredentials, FacebookPhotosInterface photosInterface) {
    this.appCredentials = appCredentials;
    this.photosInterface = photosInterface;
  }

  @Override
  public ExportResult<PhotosContainerResource> export(
      UUID jobId, TokensAndUrlAuthData authData, Optional<ExportInformation> exportInformation) {
    Preconditions.checkNotNull(authData);

    if (!exportInformation.isPresent()) {
      // No export information if at the start of a bulk export
      // Start by getting the list of albums to export
      return exportAlbums(authData, Optional.empty());
    }

    StringPaginationToken paginationToken =
        (StringPaginationToken) exportInformation.get().getPaginationData();
    ContainerResource containerResource = exportInformation.get().getContainerResource();

    boolean containerResourcePresent = containerResource != null;
    boolean paginationDataPresent = paginationToken != null;

    if (!containerResourcePresent
        && paginationDataPresent
        && paginationToken.getToken().startsWith(ALBUM_TOKEN_PREFIX)) {
      // Continue exporting albums
      return exportAlbums(authData, Optional.of(paginationToken));
    } else if (containerResourcePresent && containerResource instanceof PhotosContainerResource) {
      // We have had albums specified from the front end so process them for import
      PhotosContainerResource photosContainerResource = (PhotosContainerResource) containerResource;
      Preconditions.checkNotNull(photosContainerResource.getAlbums());
      ContinuationData continuationData = new ContinuationData(null);
      for (PhotoAlbum album : photosContainerResource.getAlbums()) {
        continuationData.addContainerResource(new IdOnlyContainerResource(album.getId()));
      }
      return new ExportResult<>(
          ExportResult.ResultType.CONTINUE, photosContainerResource, continuationData);
    } else if (containerResourcePresent && containerResource instanceof IdOnlyContainerResource) {
      // Export photos
      return exportPhotos(authData, (IdOnlyContainerResource) containerResource);
    } else {
      throw new IllegalStateException(String.format("Invalid state passed into FacebookPhotosExporter. ExportInformation: %s", exportInformation));
    }
  }

  private ExportResult<PhotosContainerResource> exportAlbums(
          TokensAndUrlAuthData authData, Optional<StringPaginationToken> paginationData) {
    Optional<String> paginationToken = Optional.empty();
    if (paginationData.isPresent()) {
      String token = paginationData.get().getToken();
      Preconditions.checkArgument(
          token.startsWith(ALBUM_TOKEN_PREFIX), "Invalid pagination token " + token);
      paginationToken = Optional.of(token.substring(ALBUM_TOKEN_PREFIX.length()));
    }

    ArrayList<PhotoAlbum> exportAlbums = new ArrayList<>();

    // Get albums
    Connection<Album> connection = getOrCreatePhotosInterface(authData).getAlbums(paginationToken);

    PaginationData nextPageData = null;
    String token = connection.getAfterCursor();
    if (!Strings.isNullOrEmpty(token)) {
      nextPageData = new StringPaginationToken(ALBUM_TOKEN_PREFIX + token);
    }
    ContinuationData continuationData = new ContinuationData(nextPageData);

    List<Album> albums = connection.getData();

    for (Album album : albums) {
      exportAlbums.add(new PhotoAlbum(album.getId(), album.getName(), album.getDescription()));
      continuationData.addContainerResource(new IdOnlyContainerResource(album.getId()));
    }

    return new ExportResult<>(
        ExportResult.ResultType.CONTINUE,
        new PhotosContainerResource(exportAlbums, null),
        continuationData);
  }

  private ExportResult<PhotosContainerResource> exportPhotos(
      TokensAndUrlAuthData authData, IdOnlyContainerResource containerResource) {
    ArrayList<PhotoModel> exportPhotos = new ArrayList<>();

    String albumId = containerResource.getId();
    Iterable<List<Photo>> photoConnection = getOrCreatePhotosInterface(authData).getPhotos(albumId);
    for (List<Photo> photos : photoConnection) {
      for (Photo photo : photos) {
        Preconditions.checkNotNull(photo.getImages().get(0).getSource());
        exportPhotos.add(
            new PhotoModel(
                String.format("%s.jpg", photo.getId()),
                photo.getImages().get(0).getSource(),
                photo.getName(),
                "image/jpg",
                photo.getId(),
                albumId,
                false));
      }
    }

    return new ExportResult<>(
        ExportResult.ResultType.END, new PhotosContainerResource(null, exportPhotos));
  }

  private synchronized FacebookPhotosInterface getOrCreatePhotosInterface(
      TokensAndUrlAuthData authData) {
    return photosInterface == null ? makePhotosInterface(authData) : photosInterface;
  }

  private synchronized FacebookPhotosInterface makePhotosInterface(TokensAndUrlAuthData authData) {
    photosInterface = new RestFbFacebookPhotos(authData, appCredentials);
    return photosInterface;
  }
}
