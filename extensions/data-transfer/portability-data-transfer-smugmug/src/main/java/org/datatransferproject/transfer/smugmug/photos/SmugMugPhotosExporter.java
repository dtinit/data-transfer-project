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

package org.datatransferproject.transfer.smugmug.photos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.HttpTransport;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.spi.transfer.types.ExportInformation;
import org.datatransferproject.spi.transfer.types.IdOnlyContainerResource;
import org.datatransferproject.spi.transfer.types.StringPaginationToken;
import org.datatransferproject.transfer.smugmug.photos.model.SmugMugAlbum;
import org.datatransferproject.transfer.smugmug.photos.model.SmugMugAlbumImage;
import org.datatransferproject.transfer.smugmug.photos.model.SmugMugAlbumImageResponse;
import org.datatransferproject.transfer.smugmug.photos.model.SmugMugAlbumsResponse;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;
import org.datatransferproject.types.transfer.models.photos.PhotoAlbum;
import org.datatransferproject.types.transfer.models.photos.PhotoModel;
import org.datatransferproject.types.transfer.models.photos.PhotosContainerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmugMugPhotosExporter
    implements Exporter<TokenSecretAuthData, PhotosContainerResource> {

  static final String ALBUM_TOKEN_PREFIX = "album:";
  static final String PHOTO_TOKEN_PREFIX = "photo:";
  static final String PRIVATE_ALBUM = "Private";

  private final AppCredentials appCredentials;
  private final HttpTransport transport;
  private final Logger logger = LoggerFactory.getLogger(SmugMugPhotosExporter.class);
  private final ObjectMapper mapper;

  private SmugMugInterface smugMugInterface;

  public SmugMugPhotosExporter(
      HttpTransport transport, AppCredentials appCredentials, ObjectMapper mapper) {
    this(null, transport, appCredentials, mapper);
  }

  @VisibleForTesting
  SmugMugPhotosExporter(
      SmugMugInterface smugMugInterface,
      HttpTransport transport,
      AppCredentials appCredentials,
      ObjectMapper mapper) {
    this.transport = transport;
    this.appCredentials = appCredentials;
    this.smugMugInterface = smugMugInterface;
    this.mapper = mapper;
  }

  @Override
  public ExportResult<PhotosContainerResource> export(
      UUID jobId, TokenSecretAuthData authData, Optional<ExportInformation> exportInformation)
      throws IOException {

    StringPaginationToken paginationToken = exportInformation.isPresent()
        ? (StringPaginationToken) exportInformation.get().getPaginationData()
        : null;
    IdOnlyContainerResource resource = exportInformation.isPresent()
        ? (IdOnlyContainerResource) exportInformation.get().getContainerResource()
        : null;

    SmugMugInterface smugMugInterface;

    try {
      smugMugInterface = getOrCreateSmugMugInterface(authData);
    } catch (IOException e) {
      logger.warn("Unable to create smugmug service for user: {}", e.getMessage());
      throw e;
    }

    if (resource != null) {
      return exportPhotos(resource, paginationToken, smugMugInterface);
    } else {
      return exportAlbums(paginationToken, smugMugInterface);
    }
  }

  private ExportResult<PhotosContainerResource> exportAlbums(
      StringPaginationToken paginationData, SmugMugInterface smugMugInterface) throws IOException {

    SmugMugAlbumsResponse albumsResponse;
    try {
      // Make request to SmugMug
      String albumInfoUri = "";
      if (paginationData != null) {
        String pageToken = paginationData.getToken();
        Preconditions.checkState(
            pageToken.startsWith(ALBUM_TOKEN_PREFIX), "Invalid pagination token " + pageToken);
        albumInfoUri = pageToken.substring(ALBUM_TOKEN_PREFIX.length());
      }
      albumsResponse = smugMugInterface.getAlbums(albumInfoUri);
    } catch (IOException e) {
      logger.warn("Unable to get AlbumsResponse: " + e.getMessage());
      throw e;
    }

    // Set up continuation data
    StringPaginationToken paginationToken = null;
    if (albumsResponse.getPageInfo() != null
        && albumsResponse.getPageInfo().getNextPage() != null) {
      paginationToken =
          new StringPaginationToken(
              ALBUM_TOKEN_PREFIX + albumsResponse.getPageInfo().getNextPage());
    }
    ContinuationData continuationData = new ContinuationData(paginationToken);

    // Build album list
    List<PhotoAlbum> albumsList = new ArrayList<>();
    for (SmugMugAlbum album : albumsResponse.getAlbums()) {
      if (!album.getPrivacy().equals(PRIVATE_ALBUM)) {
        albumsList.add(new PhotoAlbum(album.getWebUri(), album.getName(), album.getDescription()));
        continuationData.addContainerResource(new IdOnlyContainerResource(album.getUri()));
      }
    }
    PhotosContainerResource resource = new PhotosContainerResource(albumsList, null);

    // Get result type
    ResultType resultType = ResultType.CONTINUE;
    if (paginationToken == null) {
      resultType = ResultType.END;
    }

    return new ExportResult<>(resultType, resource, continuationData);
  }

  private ExportResult<PhotosContainerResource> exportPhotos(
      IdOnlyContainerResource containerResource,
      StringPaginationToken paginationData,
      SmugMugInterface smugMugInterface) throws IOException {
    List<PhotoModel> photoList = new ArrayList<>();

    // Make request to SmugMug
    String photoInfoUri;
    if (paginationData != null) {
      String token = paginationData.getToken();
      Preconditions.checkState(
          token.startsWith(PHOTO_TOKEN_PREFIX), "Invalid pagination token " + token);
      photoInfoUri = token.substring(PHOTO_TOKEN_PREFIX.length());
    } else {
      photoInfoUri = containerResource.getId();
    }

    SmugMugAlbumImageResponse albumImageList;
    try {
      albumImageList = smugMugInterface.getListOfAlbumImages(photoInfoUri + "!images");
    } catch (IOException e) {
      logger.warn("Unable to get SmugMugAlbumImageResponse");
      throw e;
    }

    // Set up continuation data
    StringPaginationToken pageToken = null;
    if (albumImageList.getPageInfo().getNextPage() != null) {
      pageToken =
          new StringPaginationToken(
              PHOTO_TOKEN_PREFIX + albumImageList.getPageInfo().getNextPage());
    }
    ContinuationData continuationData = new ContinuationData(pageToken);

    // Make list of photos - images may be empty if the album provided is empty
    List<SmugMugAlbumImage> images =
        albumImageList.getAlbumImages() == null
            ? ImmutableList.of()
            : albumImageList.getAlbumImages();

    for (SmugMugAlbumImage albumImage : images) {
      String title = albumImage.getImage().getTitle();
      if (Strings.isNullOrEmpty(title)) {
        title = albumImage.getImage().getFileName();
      }

      photoList.add(
          new PhotoModel(
              title,
              // TODO: sign the archived uri to get private photos to work.
              albumImage.getImage().getWebUri(),
              albumImage.getImage().getCaption(),
              getMimeType(albumImage.getImage().getFormat()),
              null,
              containerResource.getId()));
    }

    PhotosContainerResource resource = new PhotosContainerResource(null, photoList);

    // Get result type
    ResultType resultType = ResultType.CONTINUE;
    if (pageToken == null) {
      resultType = ResultType.END;
    }

    return new ExportResult<>(resultType, resource, continuationData);
  }

  // Returns the provided interface, or a new one specific to the authData provided.
  private SmugMugInterface getOrCreateSmugMugInterface(TokenSecretAuthData authData)
      throws IOException {
    return smugMugInterface == null
        ? new SmugMugInterface(transport, appCredentials, authData, mapper)
        : smugMugInterface;
  }

  private String getMimeType(String smugMugformat) {
    switch (smugMugformat) {
      case "JPG":
      case "JPEG":
        return "image/jpeg";
      case "PNG":
        return "image/png";
      default:
        throw new IllegalArgumentException("Don't know how to map: " + smugMugformat);
    }
  }
}
