/*
 * Copyright 2021 The Data Transfer Project Authors.
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
package org.datatransferproject.datatransfer.google.media;

import static java.lang.String.format;

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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.google.common.FailedToListAlbumsException;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.common.GoogleErrorLogger;
import org.datatransferproject.datatransfer.google.mediaModels.AlbumListResponse;
import org.datatransferproject.datatransfer.google.mediaModels.GoogleAlbum;
import org.datatransferproject.datatransfer.google.mediaModels.GoogleMediaItem;
import org.datatransferproject.datatransfer.google.mediaModels.MediaItemSearchResponse;
import org.datatransferproject.datatransfer.google.photos.GooglePhotosInterface;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.datatransferproject.spi.transfer.types.TempMediaData;
import org.datatransferproject.spi.transfer.types.UploadErrorException;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.PaginationData;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.IdOnlyContainerResource;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.transfer.errors.ErrorDetail;

public class GoogleMediaExporter implements Exporter<TokensAndUrlAuthData, MediaContainerResource> {

  static final String ALBUM_TOKEN_PREFIX = "album:";
  static final String MEDIA_TOKEN_PREFIX = "media:";

  private final GoogleCredentialFactory credentialFactory;
  private final JobStore jobStore;
  private final JsonFactory jsonFactory;
  private final Monitor monitor;
  private volatile GooglePhotosInterface photosInterface;
  private IdempotentImportExecutor retryingExecutor;
  private Boolean enableRetrying;

  public GoogleMediaExporter(
      GoogleCredentialFactory credentialFactory,
      JobStore jobStore,
      JsonFactory jsonFactory,
      Monitor monitor) {
    this(
        credentialFactory,
        jobStore,
        jsonFactory,
        monitor,
        /* photosInterface= */ null
    );
  }

  @VisibleForTesting
  GoogleMediaExporter(
      GoogleCredentialFactory credentialFactory,
      JobStore jobStore,
      JsonFactory jsonFactory,
      Monitor monitor, @Nullable
      GooglePhotosInterface photosInterface
  ) {
    this(
        credentialFactory,
        jobStore,
        jsonFactory,
        monitor,
        photosInterface,
        /* retryingExecutor= */ null,
        false);
  }

  @VisibleForTesting
  public GoogleMediaExporter(
      GoogleCredentialFactory credentialFactory,
      JobStore jobStore,
      JsonFactory jsonFactory,
      Monitor monitor,
      @Nullable GooglePhotosInterface photosInterface,
      @Nullable IdempotentImportExecutor retryingExecutor,
      boolean enableRetrying) {
    this.credentialFactory = credentialFactory;
    this.jobStore = jobStore;
    this.jsonFactory = jsonFactory;
    this.photosInterface = photosInterface;
    this.monitor = monitor;
    this.retryingExecutor = retryingExecutor;
    this.enableRetrying = enableRetrying;
  }

  @VisibleForTesting
  static InputStream convertJsonToInputStream(Object jsonObject) throws JsonProcessingException {
    String tempString = new ObjectMapper().writeValueAsString(jsonObject);
    return new ByteArrayInputStream(tempString.getBytes(StandardCharsets.UTF_8));
  }

  private static String createCacheKey() {
    return "tempMediaData";
  }

  @Override
  public ExportResult<MediaContainerResource> export(
      UUID jobId, TokensAndUrlAuthData authData, Optional<ExportInformation> exportInformation)
          throws UploadErrorException, FailedToListAlbumsException, InvalidTokenException, PermissionDeniedException, IOException {
    if (!exportInformation.isPresent()) {
      // Make list of photos contained in albums so they are not exported twice later on
      populateContainedMediaList(jobId, authData);
      return exportAlbums(authData, Optional.empty(), jobId);
    } else if (exportInformation.get().getContainerResource() instanceof PhotosContainerResource) {
      // if ExportInformation is a photos container, this is a request to only export the contents
      // in that container instead of the whole user library
      return exportPhotosContainer(
          (PhotosContainerResource) exportInformation.get().getContainerResource(), authData, jobId);
    } else if (exportInformation.get().getContainerResource() instanceof MediaContainerResource) {
      // if ExportInformation is a media container, this is a request to only export the contents
      // in that container instead of the whole user library (this is to support backwards
      // compatibility with the GooglePhotosExporter)
      return exportMediaContainer(
          (MediaContainerResource) exportInformation.get().getContainerResource(), authData, jobId);
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
     * containing only MEDIA_TOKEN_PREFIX with no token attached, in order to differentiate this
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
      // were still listing out all of the albums since we have pagination data
      return exportAlbums(authData, Optional.of(paginationToken), jobId);
    } else {
      return exportMedia(
          authData,
          Optional.ofNullable(idOnlyContainerResource),
          Optional.ofNullable(paginationToken),
          jobId);
    }
  }

  /* Maintain this for backwards compatability, so that we can pull out the album information */
  private ExportResult<MediaContainerResource> exportPhotosContainer(
      PhotosContainerResource container, TokensAndUrlAuthData authData, UUID jobId)
      throws IOException, InvalidTokenException, PermissionDeniedException {
    ImmutableList.Builder<MediaAlbum> albumBuilder = ImmutableList.builder();
    ImmutableList.Builder<PhotoModel> photosBuilder = ImmutableList.builder();
    List<IdOnlyContainerResource> subResources = new ArrayList<>();

    for (PhotoAlbum album : container.getAlbums()) {
      GoogleAlbum googleAlbum = getGoogleAlbum(album.getIdempotentId(), album.getId(), album.getName(), authData);
      if (googleAlbum == null) {
        continue;
      }

      albumBuilder.add(new MediaAlbum(googleAlbum.getId(), googleAlbum.getTitle(), null));
      // Adding subresources tells the framework to recall export to get all the photos
      subResources.add(new IdOnlyContainerResource(googleAlbum.getId()));
    }

    ImmutableList.Builder<ErrorDetail> errors = ImmutableList.builder();
    for (PhotoModel photo : container.getPhotos()) {
      GoogleMediaItem googleMediaItem =
          getGoogleMediaItem(photo.getIdempotentId(), photo.getDataId(), photo.getName(), authData);
      if (googleMediaItem == null) {
        continue;
      }

      try {
        photosBuilder.add(GoogleMediaItem.convertToPhotoModel(Optional.empty(), googleMediaItem));
      } catch(ParseException e) {
        monitor.info(() -> "Parse exception occurred while converting photo, skipping this item. "
            + "Failure message : %s ", e.getMessage());

        errors.add(GoogleErrorLogger.createErrorDetail(
            googleMediaItem.getId(), googleMediaItem.getFilename(), e, /* canSkip= */ true));
      }
    }
    // Log all the errors in 1 commit to DataStore
    GoogleErrorLogger.logFailedItemErrors(jobStore, jobId, errors.build());

    MediaContainerResource mediaContainerResource =
        new MediaContainerResource(albumBuilder.build(), photosBuilder.build(), null);
    ContinuationData continuationData = new ContinuationData(null);
    subResources.forEach(resource -> continuationData.addContainerResource(resource));
    return new ExportResult<>(ResultType.CONTINUE, mediaContainerResource, continuationData);
  }

  /* Maintain this for backwards compatability, so that we can pull out the album information */
  private ExportResult<MediaContainerResource> exportMediaContainer(
      MediaContainerResource container, TokensAndUrlAuthData authData, UUID jobId)
      throws IOException, InvalidTokenException, PermissionDeniedException {
    ImmutableList.Builder<MediaAlbum> albumBuilder = ImmutableList.builder();
    ImmutableList.Builder<PhotoModel> photosBuilder = ImmutableList.builder();
    ImmutableList.Builder<VideoModel> videosBuilder = ImmutableList.builder();
    List<IdOnlyContainerResource> subResources = new ArrayList<>();

    for (MediaAlbum album : container.getAlbums()) {
      GoogleAlbum googleAlbum = getGoogleAlbum(album.getIdempotentId(), album.getId(), album.getName(), authData);
      if (googleAlbum == null) {
        continue;
      }

      albumBuilder.add(new MediaAlbum(googleAlbum.getId(), googleAlbum.getTitle(), null));
      // Adding subresources tells the framework to recall export to get all the photos
      subResources.add(new IdOnlyContainerResource(googleAlbum.getId()));
    }

    ImmutableList.Builder<ErrorDetail> errors = ImmutableList.builder();
    for (PhotoModel photo : container.getPhotos()) {
      GoogleMediaItem photoMediaItem =
          getGoogleMediaItem(photo.getIdempotentId(), photo.getDataId(), photo.getName(), authData);
      if (photoMediaItem == null) {
        continue;
      }

      try {
        photosBuilder.add(GoogleMediaItem.convertToPhotoModel(Optional.empty(), photoMediaItem));
      } catch(ParseException e) {
        monitor.info(() -> "Parse exception occurred while converting photo, skipping this item. "
            + "Failure message : %s ", e.getMessage());

        errors.add(GoogleErrorLogger.createErrorDetail(
            photoMediaItem.getId(), photoMediaItem.getFilename(), e, /* canSkip= */ true));
      }
    }

    for (VideoModel video : container.getVideos()) {
      GoogleMediaItem videoMediaItem =
          getGoogleMediaItem(video.getIdempotentId(), video.getDataId(), video.getName(), authData);
      if (videoMediaItem == null) {
        continue;
      }

      try {
        videosBuilder.add(GoogleMediaItem.convertToVideoModel(Optional.empty(), videoMediaItem));
      } catch(ParseException e) {
        monitor.info(() -> "Parse exception occurred while converting video, skipping this item. "
            + "Failure message : %s ", e.getMessage());

        errors.add(GoogleErrorLogger.createErrorDetail(
            videoMediaItem.getId(), videoMediaItem.getFilename(), e, /* canSkip= */ true));
      }
    }

    // Log all the errors in 1 commit to DataStore
    GoogleErrorLogger.logFailedItemErrors(jobStore, jobId, errors.build());

    MediaContainerResource mediaContainerResource =
        new MediaContainerResource(
            albumBuilder.build(), photosBuilder.build(), videosBuilder.build());
    ContinuationData continuationData = new ContinuationData(null);
    subResources.forEach(resource -> continuationData.addContainerResource(resource));
    return new ExportResult<>(ResultType.CONTINUE, mediaContainerResource, continuationData);
  }

  /**
   * Note: not all accounts have albums to return. In that case, we just return an empty list of
   * albums instead of trying to iterate through a null list.
   */
  @VisibleForTesting
  ExportResult<MediaContainerResource> exportAlbums(
      TokensAndUrlAuthData authData, Optional<PaginationData> paginationData, UUID jobId)
          throws FailedToListAlbumsException {
    Optional<String> paginationToken = Optional.empty();
    if (paginationData.isPresent()) {
      String token = ((StringPaginationToken) paginationData.get()).getToken();
      Preconditions.checkArgument(
          token.startsWith(ALBUM_TOKEN_PREFIX), "Invalid pagination token " + token);
      paginationToken = Optional.of(token.substring(ALBUM_TOKEN_PREFIX.length()));
    }

    AlbumListResponse albumListResponse = listAlbums(jobId, authData, paginationToken);

    PaginationData nextPageData;
    String token = albumListResponse.getNextPageToken();
    List<MediaAlbum> albums = new ArrayList<>();
    GoogleAlbum[] googleAlbums = albumListResponse.getAlbums();

    if (Strings.isNullOrEmpty(token)) {
      nextPageData = new StringPaginationToken(MEDIA_TOKEN_PREFIX);
    } else {
      nextPageData = new StringPaginationToken(ALBUM_TOKEN_PREFIX + token);
    }
    ContinuationData continuationData = new ContinuationData(nextPageData);

    if (googleAlbums != null && googleAlbums.length > 0) {
      for (GoogleAlbum googleAlbum : googleAlbums) {
        // Add album info to list so album can be recreated later
        MediaAlbum album = new MediaAlbum(googleAlbum.getId(), googleAlbum.getTitle(), null);
        albums.add(album);

        monitor.debug(
            () -> format("%s: Google Photos exporting album: %s", jobId, album.getId()));

        // Add album id to continuation data
        continuationData.addContainerResource(new IdOnlyContainerResource(googleAlbum.getId()));
      }
    }

    ResultType resultType = ResultType.CONTINUE;

    MediaContainerResource containerResource = new MediaContainerResource(albums, null, null);
    return new ExportResult<>(resultType, containerResource, continuationData);
  }

  @VisibleForTesting
  ExportResult<MediaContainerResource> exportMedia(
      TokensAndUrlAuthData authData,
      Optional<IdOnlyContainerResource> albumData,
      Optional<PaginationData> paginationData,
      UUID jobId)
      throws IOException, InvalidTokenException, PermissionDeniedException, UploadErrorException {
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
              MEDIA_TOKEN_PREFIX + mediaItemSearchResponse.getNextPageToken());
    }
    ContinuationData continuationData = new ContinuationData(nextPageData);

    MediaContainerResource containerResource = null;
    GoogleMediaItem[] mediaItems = mediaItemSearchResponse.getMediaItems();
    if (mediaItems != null && mediaItems.length > 0) {
      containerResource = convertMediaListToResource(albumId, mediaItems, jobId);
    }

    ResultType resultType = ResultType.CONTINUE;
    if (nextPageData == null) {
      resultType = ResultType.END;
    }

    return new ExportResult<>(resultType, containerResource, continuationData);
  }

  /** Method for storing a list of all photos that are already contained in albums */
  void populateContainedMediaList(UUID jobId, TokensAndUrlAuthData authData)
          throws IOException, InvalidTokenException, PermissionDeniedException, UploadErrorException, FailedToListAlbumsException {
    // This method is only called once at the beginning of the transfer, so we can start by
    // initializing a new TempMediaData to be store in the job store.
    TempMediaData tempMediaData = new TempMediaData(jobId);

    String albumToken = null;
    AlbumListResponse albumListResponse;
    MediaItemSearchResponse containedMediaSearchResponse;
    do {
      albumListResponse = listAlbums(jobId, authData, Optional.ofNullable(albumToken));
      albumToken = albumListResponse.getNextPageToken();
      if (albumListResponse.getAlbums() == null) {
        continue;
      }

      for (GoogleAlbum album : albumListResponse.getAlbums()) {
        String albumId = album.getId();
        String photoToken = null;

        do {
          containedMediaSearchResponse =
              getOrCreatePhotosInterface(authData)
                  .listMediaItems(Optional.of(albumId), Optional.ofNullable(photoToken));
          if (containedMediaSearchResponse.getMediaItems() != null) {
            for (GoogleMediaItem mediaItem : containedMediaSearchResponse.getMediaItems()) {
              tempMediaData.addContainedPhotoId(mediaItem.getId());
            }
          }
          photoToken = containedMediaSearchResponse.getNextPageToken();
        } while (photoToken != null);
      }

      albumToken = albumListResponse.getNextPageToken();
    } while (albumToken != null);

    // TODO: if we see complaints about objects being too large for JobStore in other places, we
    // should consider putting logic in JobStore itself to handle it
    InputStream stream = convertJsonToInputStream(tempMediaData);
    jobStore.create(jobId, createCacheKey(), stream);
  }

  private Optional<String> getPhotosPaginationToken(Optional<PaginationData> paginationData) {
    Optional<String> paginationToken = Optional.empty();
    if (paginationData.isPresent()) {
      String token = ((StringPaginationToken) paginationData.get()).getToken();
      Preconditions.checkArgument(
          token.startsWith(MEDIA_TOKEN_PREFIX), "Invalid pagination token " + token);
      if (token.length() > MEDIA_TOKEN_PREFIX.length()) {
        paginationToken = Optional.of(token.substring(MEDIA_TOKEN_PREFIX.length()));
      }
    }
    return paginationToken;
  }

  private MediaContainerResource convertMediaListToResource(
      Optional<String> albumId, GoogleMediaItem[] mediaItems, UUID jobId) throws IOException {
    List<PhotoModel> photos = new ArrayList<>(mediaItems.length);
    List<VideoModel> videos = new ArrayList<>(mediaItems.length);

    TempMediaData tempMediaData = null;
    InputStream stream = jobStore.getStream(jobId, createCacheKey()).getStream();
    if (stream != null) {
      tempMediaData = new ObjectMapper().readValue(stream, TempMediaData.class);
      stream.close();
    }

    ImmutableList.Builder<ErrorDetail> errors = ImmutableList.builder();
    for (GoogleMediaItem mediaItem : mediaItems) {
      boolean shouldUpload = albumId.isPresent();

      if (tempMediaData != null) {
        shouldUpload = shouldUpload || !tempMediaData.isContainedPhotoId(mediaItem.getId());
      }

      if (mediaItem.isPhoto()) {
        if (shouldUpload) {
          try {
            PhotoModel photoModel = GoogleMediaItem.convertToPhotoModel(albumId, mediaItem);
            photos.add(photoModel);

            monitor.debug(
                () -> String.format("%s: Google exporting photo: %s", jobId, photoModel.getDataId()));
          } catch(ParseException e) {
            monitor.info(() -> "Parse exception occurred while converting photo, skipping this item. "
                + "Failure message : %s ", e.getMessage());

            errors.add(GoogleErrorLogger.createErrorDetail(
                mediaItem.getId(), mediaItem.getFilename(), e, /* canSkip= */ true));
          }
        }
      } else if (mediaItem.isVideo()) {
        if (shouldUpload) {
          try {
            VideoModel videoModel = GoogleMediaItem.convertToVideoModel(albumId, mediaItem);
            videos.add(videoModel);

            monitor.debug(
                () -> String.format("%s: Google exporting video: %s", jobId, videoModel.getDataId()));
          } catch(ParseException e) {
            monitor.info(() -> "Parse exception occurred while converting video, skipping this item. "
                + "Failure message : %s ", e.getMessage());

            errors.add(GoogleErrorLogger.createErrorDetail(
                mediaItem.getId(), mediaItem.getFilename(), e, /* canSkip= */ true));
          }
        }
      }
    }

    // Log all the errors in 1 commit to DataStore
    GoogleErrorLogger.logFailedItemErrors(jobStore, jobId, errors.build());
    return new MediaContainerResource(null  /*albums*/, photos, videos);
  }

  @VisibleForTesting
  @Nullable
  GoogleAlbum getGoogleAlbum(String albumIdempotentId, String albumId, String albumName,
      TokensAndUrlAuthData authData) throws IOException, InvalidTokenException,
      PermissionDeniedException {
    if (retryingExecutor == null || !enableRetrying) {
      return getOrCreatePhotosInterface(authData).getAlbum(albumId);
    }

    try {
      GoogleAlbum googleAlbum = retryingExecutor.executeAndSwallowIOExceptions(
          albumIdempotentId, albumName,
          () -> getOrCreatePhotosInterface(authData).getAlbum(albumId)
      );
      return googleAlbum;
    } catch (Exception e) {
      monitor.info(() -> format("Retry exception encountered while fetching an album: %s", e));
    }
    return null;
  }

  //TODO(#1308): Make the retrying methods API & adaptor agnostic
  @VisibleForTesting
  @Nullable
  GoogleMediaItem getGoogleMediaItem(String photoIdempotentId, String photoDataId,
      String photoName, TokensAndUrlAuthData authData) throws IOException, InvalidTokenException, PermissionDeniedException {
    if (retryingExecutor == null || !enableRetrying) {
      return getOrCreatePhotosInterface(authData).getMediaItem(photoDataId);
    }

    try {
      GoogleMediaItem googleMediaItem = retryingExecutor.executeAndSwallowIOExceptions(
          photoIdempotentId, photoName,
          () -> getOrCreatePhotosInterface(authData).getMediaItem(photoDataId)
      );
      return googleMediaItem;
    } catch (Exception e) {
      monitor.info(() -> format("Retry exception encountered while fetching a photo: %s", e));
    }
    return null;
  }

  /**
   * Tries to call PhotosInterface.listAlbums, and retries on failure. If unsuccessful, throws a
   * FailedToListAlbumsException.
   */
  private AlbumListResponse listAlbums(UUID jobId, TokensAndUrlAuthData authData, Optional<String> albumToken)
          throws FailedToListAlbumsException {
    if (retryingExecutor == null || !enableRetrying) {
      try {
        return getOrCreatePhotosInterface(authData).listAlbums(albumToken);
      } catch (IOException | InvalidTokenException | PermissionDeniedException e) {
        throw new FailedToListAlbumsException(e.getMessage(), e);
      }
    }

    try {
        return retryingExecutor.executeOrThrowException(
              format("%s: listAlbums(page=%s)", jobId, albumToken),
              format("listAlbums(page=%s)", albumToken),
              () -> getOrCreatePhotosInterface(authData).listAlbums(albumToken)
      );
    } catch (Exception e) {
      throw new FailedToListAlbumsException(e.getMessage(), e);
    }
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
}
