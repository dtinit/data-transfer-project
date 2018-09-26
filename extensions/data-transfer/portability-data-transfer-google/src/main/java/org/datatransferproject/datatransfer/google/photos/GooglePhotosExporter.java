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
package org.datatransferproject.datatransfer.google.photos;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.json.JsonFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.photos.model.GoogleAlbumListResponse;
import org.datatransferproject.datatransfer.google.photos.model.GoogleAlbum;
import org.datatransferproject.datatransfer.google.photos.model.GoogleMediaItem;
import org.datatransferproject.datatransfer.google.photos.model.MediaItemListResponse;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.spi.transfer.types.ExportInformation;
import org.datatransferproject.spi.transfer.types.IdOnlyContainerResource;
import org.datatransferproject.spi.transfer.types.PaginationData;
import org.datatransferproject.spi.transfer.types.StringPaginationToken;
import org.datatransferproject.spi.transfer.types.TempPhotosData;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.transfer.models.photos.PhotoAlbum;
import org.datatransferproject.types.transfer.models.photos.PhotoModel;
import org.datatransferproject.types.transfer.models.photos.PhotosContainerResource;

// Not ready for prime-time!
// TODO: fix duplication problems introduced by exporting all photos in 'root' directory first

public class GooglePhotosExporter
    implements Exporter<TokensAndUrlAuthData, PhotosContainerResource> {

  static final String ALBUM_TOKEN_PREFIX = "album:";
  static final String PHOTO_TOKEN_PREFIX = "media:";

  private final GoogleCredentialFactory credentialFactory;
  private final JobStore jobStore;
  private final JsonFactory jsonFactory;
  private volatile GooglePhotosInterface photosInterface;

  public GooglePhotosExporter(GoogleCredentialFactory credentialFactory, JobStore jobStore, JsonFactory jsonFactory) {
    this.credentialFactory = credentialFactory;
    this.jobStore = jobStore;
    this.jsonFactory = jsonFactory;
  }

  @VisibleForTesting
  GooglePhotosExporter(GoogleCredentialFactory credentialFactory, JobStore jobStore,
      JsonFactory jsonFactory, GooglePhotosInterface photosInterface) {
    this.credentialFactory = credentialFactory;
    this.jobStore = jobStore;
    this.jsonFactory = jsonFactory;
    this.photosInterface = photosInterface;
  }

  @Override
  public ExportResult<PhotosContainerResource> export(UUID jobId, TokensAndUrlAuthData authData,
      Optional<ExportInformation> exportInformation) throws IOException {
    if (!exportInformation.isPresent()) {
      ExportResult<PhotosContainerResource> albumResult = exportAlbums(authData, Optional.empty());
      List<PhotoModel> albumlessPhotos = getAlbumlessPhotos(authData);
      if (albumlessPhotos != null && albumlessPhotos.size() > 0) {
        return addMorePhotos(albumResult, albumlessPhotos);
      } else {
        return albumResult;
      }
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

  /**
   * Note: not all accounts have albums to return.  In that case, we just return an empty list of
   * albums instead of trying to iterate through a null list.
   */
  @VisibleForTesting
  ExportResult<PhotosContainerResource> exportAlbums(TokensAndUrlAuthData authData,
      Optional<PaginationData> paginationData) throws IOException {
    Optional<String> paginationToken = Optional.empty();
    if (paginationData.isPresent()) {
      String token = ((StringPaginationToken) paginationData.get()).getToken();
      Preconditions.checkArgument(
          token.startsWith(ALBUM_TOKEN_PREFIX), "Invalid pagination token " + token);
      paginationToken = Optional.of(token.substring(ALBUM_TOKEN_PREFIX.length()));
    }

    GoogleAlbumListResponse googleAlbumListResponse;

    googleAlbumListResponse = getOrCreatePhotosInterface(authData).listAlbums(paginationToken);

    PaginationData nextPageData = null;
    String token = googleAlbumListResponse.getNextPageToken();
    if (!Strings.isNullOrEmpty(token)) {
      nextPageData = new StringPaginationToken(ALBUM_TOKEN_PREFIX + token);
    }

    ContinuationData continuationData = new ContinuationData(nextPageData);
    List<PhotoAlbum> albums = new ArrayList<>();
    GoogleAlbum[] googleAlbums = googleAlbumListResponse.getAlbums();

    for (GoogleAlbum googleAlbum : googleAlbums) {
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

  @VisibleForTesting
  ExportResult<PhotosContainerResource> exportPhotos(TokensAndUrlAuthData authData, String albumId,
      Optional<PaginationData> paginationData) {
    Optional<String> paginationToken = Optional.empty();
    if (paginationData.isPresent()) {
      String token = ((StringPaginationToken) paginationData.get()).getToken();
      Preconditions.checkArgument(
          token.startsWith(PHOTO_TOKEN_PREFIX), "Invalid pagination token " + token);
      paginationToken = Optional.of(token.substring(PHOTO_TOKEN_PREFIX.length()));
    }

    MediaItemListResponse mediaItemListResponse;

    try {
      mediaItemListResponse = getOrCreatePhotosInterface(authData)
          .listAlbumContents(Optional.of(albumId), paginationToken);
    } catch (IOException e) {
      return new ExportResult<>(e);
    }

    PaginationData nextPageData = null;
    if (!Strings.isNullOrEmpty(mediaItemListResponse.getNextPageToken())) {
      nextPageData = new StringPaginationToken(
          PHOTO_TOKEN_PREFIX + mediaItemListResponse.getNextPageToken());
    }
    ContinuationData continuationData = new ContinuationData(nextPageData);

    List<PhotoModel> photos = new ArrayList<>();
    for (GoogleMediaItem mediaItem : mediaItemListResponse.getMediaItems()) {
      if (mediaItem.getMediaMetadata().getPhoto() != null) {
        // TODO: address videos later on
        photos.add(convertToPhotoModel(albumId, mediaItem));
      }
    }

    PhotosContainerResource containerResource = new PhotosContainerResource(null, photos);

    ResultType resultType = ResultType.CONTINUE;
    if (nextPageData == null) {
      resultType = ResultType.END;
    }
    return new ExportResult<>(resultType, containerResource, continuationData);
  }

  private void populateContainedPhotosList(UUID jobId, TokensAndUrlAuthData authData)
      throws IOException {
    // Get list of all album ids
    List<String> albumIds = new LinkedList<>();
    GoogleAlbumListResponse albumListResponse = getOrCreatePhotosInterface(authData)
        .listAlbums(Optional.empty());
    albumIds.addAll(Arrays.stream(albumListResponse.getAlbums()).map(GoogleAlbum::getId)
        .collect(Collectors.toList()));
    String token;
    while ((token = albumListResponse.getNextPageToken()) != null) {
      albumListResponse = getOrCreatePhotosInterface(authData)
          .listAlbums(Optional.of(token));
      albumIds.addAll(Arrays.stream(albumListResponse.getAlbums()).map(GoogleAlbum::getId)
          .collect(Collectors.toList()));
    }

    // Get list of all ids belonging to photos belonging to albums
    TempPhotosData tempPhotosData = jobStore
        .findData(jobId, createCacheKey(), TempPhotosData.class);
    if (tempPhotosData == null) {
      tempPhotosData = new TempPhotosData(jobId);
      jobStore.create(jobId, createCacheKey(), tempPhotosData);
    }
    for (String albumId : albumIds) {
      MediaItemListResponse albumMediaItemResponse = getOrCreatePhotosInterface(authData)
          .listAlbumContents(Optional.of(albumId), Optional.empty());
      tempPhotosData.addAllContainedPhotoIds(
          Arrays.stream(albumMediaItemResponse.getMediaItems()).map(GoogleMediaItem::getId)
              .collect(Collectors.toList()));
      while ((token = albumMediaItemResponse.getNextPageToken()) != null) {
        albumMediaItemResponse = getOrCreatePhotosInterface(authData)
            .listAlbumContents(Optional.of(albumId), Optional.of(token));
        tempPhotosData.addAllContainedPhotoIds(
            Arrays.stream(albumMediaItemResponse.getMediaItems()).map(GoogleMediaItem::getId)
                .collect(Collectors.toList()));
      }
    }
    jobStore.update(jobId, createCacheKey(), tempPhotosData);
  }

  private List<PhotoModel> getAlbumlessPhotos(TokensAndUrlAuthData authData) throws IOException {
    // TODO: what if this doesn't fit in memory?
    // Get list of all media items
    List<GoogleMediaItem> mediaItems = new LinkedList<>();
    MediaItemListResponse mediaItemResponse = getOrCreatePhotosInterface(authData)
        .listAllMediaItems(Optional.empty());
    mediaItems.addAll(Arrays.asList(mediaItemResponse.getMediaItems()));
    String token;
    while ((token = mediaItemResponse.getNextPageToken()) != null) {
      mediaItemResponse = getOrCreatePhotosInterface(authData)
          .listAllMediaItems(Optional.of(token));
      mediaItems.addAll(Arrays.asList(mediaItemResponse.getMediaItems()));
    }

    // Get list of all albums, and then list of photos in albums
    List<GoogleAlbum> allAlbums = new LinkedList<>();
    GoogleAlbumListResponse albumListResponse = getOrCreatePhotosInterface(authData)
        .listAlbums(Optional.empty());
    allAlbums.addAll(Arrays.asList(albumListResponse.getAlbums()));
    while (albumListResponse.getNextPageToken() != null) {
      albumListResponse = getOrCreatePhotosInterface(authData)
          .listAlbums(Optional.of(albumListResponse.getNextPageToken()));
      allAlbums.addAll(Arrays.asList(albumListResponse.getAlbums()));
    }
    List<GoogleMediaItem> albumMediaItems = new LinkedList<>();
    for (GoogleAlbum album : allAlbums) {
      MediaItemListResponse albumMediaItemResponse = getOrCreatePhotosInterface(authData)
          .listAlbumContents(Optional.of(album.getId()), Optional.empty());
      albumMediaItems.addAll(Arrays.asList(albumMediaItemResponse.getMediaItems()));
      while (albumMediaItemResponse.getNextPageToken() != null) {
        albumMediaItemResponse = getOrCreatePhotosInterface(authData)
            .listAlbumContents(Optional.of(album.getId()),
                Optional.of(albumMediaItemResponse.getNextPageToken()));
        albumMediaItems.addAll(Arrays.asList(albumMediaItemResponse.getMediaItems()));
      }
    }

    // Subtract album photos list from list of all media items
    mediaItems.removeAll(albumMediaItems);

    // Convert to PhotoModels
    if (mediaItems.size() > 0) {
      return mediaItems.stream()
          .map(a -> convertToPhotoModel(null, a))
          .collect(Collectors.toList());
    } else {
      return null;
    }
  }

  @VisibleForTesting
  ExportResult<PhotosContainerResource> addMorePhotos(
      ExportResult<PhotosContainerResource> origExportResult, List<PhotoModel> addlPhotos) {
    ArrayList<PhotoModel> finalPhotoList = new ArrayList<>(
        addlPhotos);
    PhotosContainerResource origPhotosContainerResource = origExportResult.getExportedData();
    finalPhotoList.addAll(origExportResult.getExportedData().getPhotos());

    return new ExportResult<>(
        origExportResult.getType(),
        new PhotosContainerResource(
            origPhotosContainerResource.getAlbums(),
            finalPhotoList),
        origExportResult.getContinuationData());
  }

  private PhotoModel convertToPhotoModel(@Nullable String albumId, GoogleMediaItem mediaItem) {
    Preconditions.checkArgument(mediaItem.getMediaMetadata().getPhoto() != null);

    return new PhotoModel(
        "", // TODO: no title?
        mediaItem.getBaseUrl() + "=d",
        mediaItem.getDescription(),
        mediaItem.getMimeType(),
        mediaItem.getId(),
        albumId,
        false);
  }

  private synchronized GooglePhotosInterface getOrCreatePhotosInterface(
      TokensAndUrlAuthData authData) {
    return photosInterface == null ? makePhotosInterface(authData) : photosInterface;
  }

  private synchronized GooglePhotosInterface makePhotosInterface(TokensAndUrlAuthData authData) {
    Credential credential = credentialFactory.createCredential(authData);
    return new GooglePhotosInterface(credential, jsonFactory);
  }

  private static String createCacheKey() {
    return "tempPhotosData";
  }
}
