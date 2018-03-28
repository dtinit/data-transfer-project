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

package org.dataportabilityproject.transfer.smugmug.photos;

import static org.dataportabilityproject.transfer.smugmug.photos.SmugMugInterface.ALBUMS_KEY;
import static org.dataportabilityproject.transfer.smugmug.photos.SmugMugInterface.USER_URL;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.dataportabilityproject.spi.transfer.provider.ExportResult;
import org.dataportabilityproject.spi.transfer.provider.ExportResult.ResultType;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.types.ContinuationData;
import org.dataportabilityproject.spi.transfer.types.ExportInformation;
import org.dataportabilityproject.spi.transfer.types.IdOnlyContainerResource;
import org.dataportabilityproject.spi.transfer.types.PaginationData;
import org.dataportabilityproject.spi.transfer.types.StringPaginationToken;
import org.dataportabilityproject.transfer.smugmug.photos.model.SmugMugAlbum;
import org.dataportabilityproject.transfer.smugmug.photos.model.SmugMugAlbumImage;
import org.dataportabilityproject.transfer.smugmug.photos.model.SmugMugAlbumInfoResponse;
import org.dataportabilityproject.transfer.smugmug.photos.model.SmugMugAlbumsResponse;
import org.dataportabilityproject.transfer.smugmug.photos.model.SmugMugResponse;
import org.dataportabilityproject.transfer.smugmug.photos.model.SmugMugUserResponse;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.models.photos.PhotoAlbum;
import org.dataportabilityproject.types.transfer.models.photos.PhotoModel;
import org.dataportabilityproject.types.transfer.models.photos.PhotosContainerResource;

public class SmugMugPhotosExporter implements Exporter<AuthData, PhotosContainerResource> {

  static final String ALBUM_TOKEN_PREFIX = "album:";
  static final String PHOTO_TOKEN_PREFIX = "photo:";

  static final String ALBUM_URL_FORMATTER = "/api/v2/album/%s!images";
  private SmugMugInterface smugMugInterface;

  public SmugMugPhotosExporter() {
    // TODO(olsona)
    this.smugMugInterface = null;
  }

  @VisibleForTesting
  SmugMugPhotosExporter(SmugMugInterface smugMugInterface) {
    this.smugMugInterface = smugMugInterface;
  }

  @Override
  public ExportResult<PhotosContainerResource> export(UUID jobId, AuthData authData) {
    return exportAlbums(Optional.empty());
  }

  @Override
  public ExportResult<PhotosContainerResource> export(UUID jobId, AuthData authData,
      ExportInformation exportInformation) {
    StringPaginationToken paginationToken =
        (StringPaginationToken) exportInformation.getPaginationData();
    if (paginationToken != null && paginationToken.getToken().startsWith(ALBUM_TOKEN_PREFIX)) {
      // Next thing to export is more albums
      return exportAlbums(Optional.of(paginationToken));
    } else {
      // Next thing to export is photos
      IdOnlyContainerResource idOnlyContainerResource =
          (IdOnlyContainerResource) exportInformation.getContainerResource();
      Optional<PaginationData> pageData =
          paginationToken != null ? Optional.of(paginationToken) : Optional.empty();
      return exportPhotos(idOnlyContainerResource, pageData);
    }
  }

  private ExportResult<PhotosContainerResource> exportAlbums(
      Optional<PaginationData> paginationData) {
    try {
      // Make request to SmugMug
      String albumInfoUri;
      if (paginationData.isPresent()) {
        String token = ((StringPaginationToken) paginationData.get()).getToken();
        Preconditions
            .checkState(token.startsWith(ALBUM_TOKEN_PREFIX), "Invalid pagination token " + token);
        albumInfoUri = token.substring(ALBUM_TOKEN_PREFIX.length());
      } else {
        SmugMugResponse<SmugMugUserResponse> userResponse = smugMugInterface
            .makeUserRequest(USER_URL);
        albumInfoUri = userResponse.getResponse().getUser().getUris().get(ALBUMS_KEY).getUri();
      }
      SmugMugResponse<SmugMugAlbumsResponse> albumsResponse = smugMugInterface
          .makeAlbumRequest(albumInfoUri);

      // Set up continuation data
      StringPaginationToken paginationToken = null;
      if (albumsResponse.getResponse().getPageInfo() != null
          && albumsResponse.getResponse().getPageInfo().getNextPage() != null) {
        paginationToken = new StringPaginationToken(ALBUM_TOKEN_PREFIX +
            albumsResponse.getResponse().getPageInfo().getNextPage());
      }
      ContinuationData continuationData = new ContinuationData(paginationToken);

      // Build album list
      List<PhotoAlbum> albumsList = new ArrayList<>();
      for (SmugMugAlbum album : albumsResponse.getResponse().getAlbums()) {
        albumsList
            .add(new PhotoAlbum(album.getAlbumKey(), album.getTitle(), album.getDescription()));
        continuationData.addContainerResource(new IdOnlyContainerResource(album.getAlbumKey()));
      }
      PhotosContainerResource resource = new PhotosContainerResource(albumsList, null);

      // Get result type
      ResultType resultType = ResultType.CONTINUE;
      if (paginationToken == null) {
        resultType = ResultType.END;
      }

      return new ExportResult<>(resultType, resource, continuationData);

    } catch (IOException e) {
      return new ExportResult<>(ResultType.ERROR, e.getMessage());
    }
  }

  private ExportResult<PhotosContainerResource> exportPhotos(
      IdOnlyContainerResource containerResource, Optional<PaginationData> paginationData) {
    try {
      List<PhotoModel> photoList = new ArrayList<>();

      // Make request to SmugMug
      String photoInfoUri;
      if (paginationData.isPresent()) {
        String token = ((StringPaginationToken) paginationData.get()).getToken();
        Preconditions
            .checkState(token.startsWith(PHOTO_TOKEN_PREFIX), "Invalid pagination token " + token);
        photoInfoUri = token.substring(PHOTO_TOKEN_PREFIX.length());
      } else {
        String id = containerResource.getId();
        photoInfoUri = String.format(ALBUM_URL_FORMATTER, id);
      }
      SmugMugResponse<SmugMugAlbumInfoResponse> albumInfoResponse = smugMugInterface
          .makeAlbumInfoRequest(photoInfoUri);

      // Set up continuation data
      StringPaginationToken pageToken = null;
      if (albumInfoResponse.getResponse().getPageInfo().getNextPage() != null) {
        pageToken = new StringPaginationToken(PHOTO_TOKEN_PREFIX +
            albumInfoResponse.getResponse().getPageInfo().getNextPage());
      }
      ContinuationData continuationData = new ContinuationData(pageToken);

      // Make list of photos
      for (SmugMugAlbumImage image : albumInfoResponse.getResponse().getImages()) {
        String title = image.getTitle();
        if (Strings.isNullOrEmpty(title)) {
          title = image.getFileName();
        }

        // TODO(olsona): this.authConsumer.sign(image.getArchivedUri()) ?
        photoList.add(
            new PhotoModel(title, image.getArchivedUri(), image.getCaption(), image.getFormat(),
                containerResource.getId()));
      }
      PhotosContainerResource resource = new PhotosContainerResource(null, photoList);

      // Get result type
      ResultType resultType = ResultType.CONTINUE;
      if (pageToken == null) {
        resultType = ResultType.END;
      }

      return new ExportResult<>(resultType, resource, continuationData);
    } catch (IOException e) {
      return new ExportResult<>(ResultType.ERROR, e.getMessage());
    }
  }
}
