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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.json.JsonFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.mediaModels.AlbumListResponse;
import org.datatransferproject.datatransfer.google.mediaModels.GoogleAlbum;
import org.datatransferproject.datatransfer.google.mediaModels.GoogleMediaItem;
import org.datatransferproject.datatransfer.google.mediaModels.MediaItemSearchResponse;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.datatransferproject.spi.transfer.types.TempPhotosData;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.PaginationData;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.IdOnlyContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

// Not ready for prime-time!
// TODO: fix duplication problems introduced by exporting all photos in 'root' directory first

public class GooglePhotosExporter
    implements Exporter<TokensAndUrlAuthData, PhotosContainerResource> {

  static final String ALBUM_TOKEN_PREFIX = "album:";
  static final String PHOTO_TOKEN_PREFIX = "media:";

  private final GoogleCredentialFactory credentialFactory;
  private final TemporaryPerJobDataStore jobStore;
  private final JsonFactory jsonFactory;
  private volatile GooglePhotosInterface photosInterface;

  private final Monitor monitor;

  public GooglePhotosExporter(
      GoogleCredentialFactory credentialFactory,
      TemporaryPerJobDataStore jobStore,
      JsonFactory jsonFactory,
      Monitor monitor) {
    this.credentialFactory = credentialFactory;
    this.jobStore = jobStore;
    this.jsonFactory = jsonFactory;
    this.monitor = monitor;
  }

  @VisibleForTesting
  GooglePhotosExporter(
      GoogleCredentialFactory credentialFactory,
      TemporaryPerJobDataStore jobStore,
      JsonFactory jsonFactory,
      GooglePhotosInterface photosInterface,
      Monitor monitor) {
    this.credentialFactory = credentialFactory;
    this.jobStore = jobStore;
    this.jsonFactory = jsonFactory;
    this.photosInterface = photosInterface;
    this.monitor = monitor;
  }

  @Override
  public ExportResult<PhotosContainerResource> export(
      UUID jobId, TokensAndUrlAuthData authData, Optional<ExportInformation> exportInformation)
      throws IOException, InvalidTokenException, PermissionDeniedException {
    if (!exportInformation.isPresent()) {
      // Make list of photos contained in albums so they are not exported twice later on
      populateContainedPhotosList(jobId, authData);
      return exportAlbums(authData, Optional.empty(), jobId);
    } else if (exportInformation.get().getContainerResource() instanceof PhotosContainerResource) {
      // if ExportInformation is a photos container, this is a request to only export the contents
      // in that container instead of the whole user library
      return exportPhotosContainer(
          (PhotosContainerResource) exportInformation.get().getContainerResource(), authData);
    }

    /*
     * Use the export information to determine whether this export call should export albums or
     * photos.
     *
     * Albums are exported if and only if the export information doesn't hold an album
     * already, and the pagination token begins with the album prefix.  There must be a pagination
     * token for album export since this is isn't the first export operation performed (if it was,
     * there wouldn't be any export information at all).
     *
     * Otherwise, photos are exported. If photos are exported, there may or may not be pagination
     * information, and there may or may not be album information. If there is no container
     * resource, that means that we're exporting albumless photos and a pagination token must be
     * present. The beginning step of exporting albumless photos is indicated by a pagination token
     * containing only PHOTO_TOKEN_PREFIX with no token attached, in order to differentiate this
     * case for the first step of export (no export information at all).
     */
    StringPaginationToken paginationToken =
        (StringPaginationToken) exportInformation.get().getPaginationData();
    IdOnlyContainerResource idOnlyContainerResource =
        (IdOnlyContainerResource) exportInformation.get().getContainerResource();

    boolean containerResourcePresent = idOnlyContainerResource != null;
    boolean paginationDataPresent = paginationToken != null;

    if (!containerResourcePresent
        && paginationDataPresent
        && paginationToken.getToken().startsWith(ALBUM_TOKEN_PREFIX)) {
      return exportAlbums(authData, Optional.of(paginationToken), jobId);
    } else {
      return exportPhotos(
          authData,
          Optional.ofNullable(idOnlyContainerResource),
          Optional.ofNullable(paginationToken),
          jobId);
    }
  }

  private ExportResult<PhotosContainerResource> exportPhotosContainer(
      PhotosContainerResource container, TokensAndUrlAuthData authData)
      throws IOException, InvalidTokenException, PermissionDeniedException {
    ImmutableList.Builder<PhotoAlbum> albumBuilder = ImmutableList.builder();
    ImmutableList.Builder<PhotoModel> photosBuilder = ImmutableList.builder();
    List<IdOnlyContainerResource> subResources = new ArrayList<>();

    for (PhotoAlbum album : container.getAlbums()) {
      GoogleAlbum googleAlbum = getOrCreatePhotosInterface(authData).getAlbum(album.getId());
      albumBuilder.add(new PhotoAlbum(googleAlbum.getId(), googleAlbum.getTitle(), null));
      // Adding subresources tells the framework to recall export to get all the photos
      subResources.add(new IdOnlyContainerResource(googleAlbum.getId()));
    }

    for (PhotoModel photo : container.getPhotos()) {
      GoogleMediaItem googleMediaItem =
          getOrCreatePhotosInterface(authData).getMediaItem(photo.getDataId());
      photosBuilder.add(convertToPhotoModel(Optional.empty(), googleMediaItem));
    }

    PhotosContainerResource photosContainerResource =
        new PhotosContainerResource(albumBuilder.build(), photosBuilder.build());
    ContinuationData continuationData = new ContinuationData(null);
    subResources.forEach(resource -> continuationData.addContainerResource(resource));
    return new ExportResult<>(ResultType.CONTINUE, photosContainerResource, continuationData);
  }

  /**
   * Note: not all accounts have albums to return. In that case, we just return an empty list of
   * albums instead of trying to iterate through a null list.
   */
  @VisibleForTesting
  ExportResult<PhotosContainerResource> exportAlbums(
      TokensAndUrlAuthData authData, Optional<PaginationData> paginationData, UUID jobId)
      throws IOException, InvalidTokenException, PermissionDeniedException {
    Optional<String> paginationToken = Optional.empty();
    if (paginationData.isPresent()) {
      String token = ((StringPaginationToken) paginationData.get()).getToken();
      Preconditions.checkArgument(
          token.startsWith(ALBUM_TOKEN_PREFIX), "Invalid pagination token " + token);
      paginationToken = Optional.of(token.substring(ALBUM_TOKEN_PREFIX.length()));
    }

    AlbumListResponse albumListResponse;

    albumListResponse = getOrCreatePhotosInterface(authData).listAlbums(paginationToken);

    PaginationData nextPageData;
    String token = albumListResponse.getNextPageToken();
    List<PhotoAlbum> albums = new ArrayList<>();
    GoogleAlbum[] googleAlbums = albumListResponse.getAlbums();

    if (Strings.isNullOrEmpty(token)) {
      nextPageData = new StringPaginationToken(PHOTO_TOKEN_PREFIX);
    } else {
      nextPageData = new StringPaginationToken(ALBUM_TOKEN_PREFIX + token);
    }
    ContinuationData continuationData = new ContinuationData(nextPageData);

    if (googleAlbums != null && googleAlbums.length > 0) {
      for (GoogleAlbum googleAlbum : googleAlbums) {
        // Add album info to list so album can be recreated later
        PhotoAlbum photoAlbum = new PhotoAlbum(googleAlbum.getId(), googleAlbum.getTitle(), null);
        albums.add(photoAlbum);

        monitor.debug(
            () ->
                String.format("%s: Google Photos exporting album: %s", jobId, photoAlbum.getId()));

        // Add album id to continuation data
        continuationData.addContainerResource(new IdOnlyContainerResource(googleAlbum.getId()));
      }
    }

    ResultType resultType = ResultType.CONTINUE;

    PhotosContainerResource containerResource = new PhotosContainerResource(albums, null);
    return new ExportResult<>(resultType, containerResource, continuationData);
  }

  @VisibleForTesting
  ExportResult<PhotosContainerResource> exportPhotos(
      TokensAndUrlAuthData authData,
      Optional<IdOnlyContainerResource> albumData,
      Optional<PaginationData> paginationData,
      UUID jobId)
      throws IOException, InvalidTokenException, PermissionDeniedException {
    Optional<String> albumId = Optional.empty();
    if (albumData.isPresent()) {
      albumId = Optional.of(albumData.get().getId());
    }
    Optional<String> paginationToken = getPhotosPaginationToken(paginationData);

    MediaItemSearchResponse mediaItemSearchResponse =
        getOrCreatePhotosInterface(authData).listMediaItems(albumId, paginationToken);

    PaginationData nextPageData = null;
    if (!Strings.isNullOrEmpty(mediaItemSearchResponse.getNextPageToken())) {
      nextPageData =
          new StringPaginationToken(
              PHOTO_TOKEN_PREFIX + mediaItemSearchResponse.getNextPageToken());
    }
    ContinuationData continuationData = new ContinuationData(nextPageData);

    PhotosContainerResource containerResource = null;
    GoogleMediaItem[] mediaItems = mediaItemSearchResponse.getMediaItems();
    if (mediaItems != null && mediaItems.length > 0) {
      List<PhotoModel> photos = convertPhotosList(albumId, mediaItems, jobId);
      containerResource = new PhotosContainerResource(null, photos);
    }

    ResultType resultType = ResultType.CONTINUE;
    if (nextPageData == null) {
      resultType = ResultType.END;
    }

    return new ExportResult<>(resultType, containerResource, continuationData);
  }

  /**
   * Method for storing a list of all photos that are already contained in albums
   */
  @VisibleForTesting
  void populateContainedPhotosList(UUID jobId, TokensAndUrlAuthData authData)
      throws IOException, InvalidTokenException, PermissionDeniedException {
    // This method is only called once at the beginning of the transfer, so we can start by
    // initializing a new TempPhotosData to be store in the job store.
    TempPhotosData tempPhotosData = new TempPhotosData(jobId);

    String albumToken = null;
    AlbumListResponse albumListResponse;
    MediaItemSearchResponse containedMediaSearchResponse;
    do {
      albumListResponse =
          getOrCreatePhotosInterface(authData).listAlbums(Optional.ofNullable(albumToken));
      if (albumListResponse.getAlbums() != null) {
        for (GoogleAlbum album : albumListResponse.getAlbums()) {
          String albumId = album.getId();
          String photoToken = null;
          do {
            containedMediaSearchResponse =
                getOrCreatePhotosInterface(authData)
                    .listMediaItems(Optional.of(albumId), Optional.ofNullable(photoToken));
            if (containedMediaSearchResponse.getMediaItems() != null) {
              for (GoogleMediaItem mediaItem : containedMediaSearchResponse.getMediaItems()) {
                tempPhotosData.addContainedPhotoId(mediaItem.getId());
              }
            }
            photoToken = containedMediaSearchResponse.getNextPageToken();
          } while (photoToken != null);
        }
      }
      albumToken = albumListResponse.getNextPageToken();
    } while (albumToken != null);

    // TODO: if we see complaints about objects being too large for JobStore in other places, we
    // should consider putting logic in JobStore itself to handle it
    InputStream stream = convertJsonToInputStream(tempPhotosData);
    jobStore.create(jobId, createCacheKey(), stream);
  }

  @VisibleForTesting
  static InputStream convertJsonToInputStream(Object jsonObject) throws JsonProcessingException {
    String tempString = new ObjectMapper().writeValueAsString(jsonObject);
    return new ByteArrayInputStream(tempString.getBytes(StandardCharsets.UTF_8));
  }

  private Optional<String> getPhotosPaginationToken(Optional<PaginationData> paginationData) {
    Optional<String> paginationToken = Optional.empty();
    if (paginationData.isPresent()) {
      String token = ((StringPaginationToken) paginationData.get()).getToken();
      Preconditions.checkArgument(
          token.startsWith(PHOTO_TOKEN_PREFIX), "Invalid pagination token " + token);
      if (token.length() > PHOTO_TOKEN_PREFIX.length()) {
        paginationToken = Optional.of(token.substring(PHOTO_TOKEN_PREFIX.length()));
      }
    }
    return paginationToken;
  }

  private List<PhotoModel> convertPhotosList(
      Optional<String> albumId, GoogleMediaItem[] mediaItems, UUID jobId) throws IOException {
    List<PhotoModel> photos = new ArrayList<>(mediaItems.length);

    TempPhotosData tempPhotosData = null;
    InputStream stream = jobStore.getStream(jobId, createCacheKey()).getStream();
    if (stream != null) {
      tempPhotosData = new ObjectMapper().readValue(stream, TempPhotosData.class);
      stream.close();
    }

    for (GoogleMediaItem mediaItem : mediaItems) {
      if (mediaItem.getMediaMetadata().getPhoto() != null) {
        // TODO: address videos
        boolean shouldUpload = albumId.isPresent();

        if (tempPhotosData != null) {
          shouldUpload = shouldUpload || !tempPhotosData.isContainedPhotoId(mediaItem.getId());
        }

        if (shouldUpload) {
          PhotoModel photoModel = convertToPhotoModel(albumId, mediaItem);
          photos.add(photoModel);

          monitor.debug(
              () -> String.format("%s: Google exporting photo: %s", jobId, photoModel.getDataId()));
        }
      }
    }
    return photos;
  }

  private PhotoModel convertToPhotoModel(Optional<String> albumId, GoogleMediaItem mediaItem) {
    Preconditions.checkArgument(mediaItem.getMediaMetadata().getPhoto() != null);

    return new PhotoModel(
        mediaItem.getFilename(),
        mediaItem.getBaseUrl() + "=d",
        mediaItem.getDescription(),
        mediaItem.getMimeType(),
        mediaItem.getId(),
        albumId.orElse(null),
        false);
  }

  private synchronized GooglePhotosInterface getOrCreatePhotosInterface(
      TokensAndUrlAuthData authData) {
    return photosInterface == null ? makePhotosInterface(authData) : photosInterface;
  }

  private synchronized GooglePhotosInterface makePhotosInterface(TokensAndUrlAuthData authData) {
    Credential credential = credentialFactory.createCredential(authData);
    return new GooglePhotosInterface(
        credentialFactory, credential, jsonFactory, monitor, /* arbitrary writesPerSecond */ 1.0);
  }

  private static String createCacheKey() {
    return "tempPhotosData";
  }
}
