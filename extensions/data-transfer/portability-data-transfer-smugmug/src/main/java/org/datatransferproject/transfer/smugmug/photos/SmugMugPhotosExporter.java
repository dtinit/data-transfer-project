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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.transfer.smugmug.photos.model.SmugMugAlbum;
import org.datatransferproject.transfer.smugmug.photos.model.SmugMugAlbumImageResponse;
import org.datatransferproject.transfer.smugmug.photos.model.SmugMugAlbumsResponse;
import org.datatransferproject.transfer.smugmug.photos.model.SmugMugImage;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.IdOnlyContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SmugMugPhotosExporter
    implements Exporter<TokenSecretAuthData, PhotosContainerResource> {

  static final String ALBUM_TOKEN_PREFIX = "album:";
  static final String PHOTO_TOKEN_PREFIX = "photo:";
  static final String PRIVATE_ALBUM = "Private";

  private final AppCredentials appCredentials;
  private final ObjectMapper mapper;
  private final TemporaryPerJobDataStore jobStore;
  private final Monitor monitor;

  private SmugMugInterface smugMugInterface;

  public SmugMugPhotosExporter(
      AppCredentials appCredentials,
      ObjectMapper mapper,
      TemporaryPerJobDataStore jobStore,
      Monitor monitor) {
    this(null, appCredentials, mapper, jobStore, monitor);
  }

  @VisibleForTesting
  SmugMugPhotosExporter(
      SmugMugInterface smugMugInterface,
      AppCredentials appCredentials,
      ObjectMapper mapper,
      TemporaryPerJobDataStore jobStore,
      Monitor monitor) {
    this.appCredentials = appCredentials;
    this.smugMugInterface = smugMugInterface;
    this.mapper = mapper;
    this.jobStore = jobStore;
    this.monitor = monitor;
  }

  @Override
  public ExportResult<PhotosContainerResource> export(
      UUID jobId, TokenSecretAuthData authData, Optional<ExportInformation> exportInformation)
      throws IOException {

    StringPaginationToken paginationToken =
        exportInformation.isPresent()
            ? (StringPaginationToken) exportInformation.get().getPaginationData()
            : null;
    IdOnlyContainerResource resource =
        exportInformation.isPresent()
            ? (IdOnlyContainerResource) exportInformation.get().getContainerResource()
            : null;

    SmugMugInterface smugMugInterface;

    try {
      smugMugInterface = getOrCreateSmugMugInterface(authData);
    } catch (IOException e) {
      monitor.severe(() -> "Unable to create Smugmug service for user", e);
      throw e;
    }

    if (resource != null) {
      return exportPhotos(resource, paginationToken, smugMugInterface, jobId);
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
      monitor.severe(() -> "Unable to get AlbumsResponse: ", e);
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
      SmugMugInterface smugMugInterface,
      UUID jobId)
      throws IOException {
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
      monitor.severe(() -> "Unable to get SmugMugAlbumImageResponse");
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
    List<SmugMugImage> images =
        albumImageList.getAlbumImages() == null
            ? ImmutableList.of()
            : albumImageList.getAlbumImages();

    for (SmugMugImage albumImage : images) {
      String title = albumImage.getTitle();
      if (Strings.isNullOrEmpty(title)) {
        title = albumImage.getFileName();
      }

      PhotoModel model =
          new PhotoModel(
              title,
              // TODO: sign the archived uri to get private photos to work.
              albumImage.getArchivedUri(),
              albumImage.getCaption(),
              getMimeType(albumImage.getFormat()),
              albumImage.getArchivedUri(),
              containerResource.getId(),
              true);

      InputStream inputStream = smugMugInterface.getImageAsStream(model.getFetchableUrl());
      jobStore.create(jobId, model.getFetchableUrl(), inputStream);

      photoList.add(model);
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
        ? new SmugMugInterface(appCredentials, authData, mapper)
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
