/*
 * Copyright 2023 The Data Transfer Project Authors.
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
import static org.datatransferproject.datatransfer.google.photos.GooglePhotosInterface.ERROR_HASH_MISMATCH;
import static org.datatransferproject.datatransfer.google.videos.GoogleVideosInterface.uploadBatchOfVideos;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.json.JsonFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.rpc.Code;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.common.GooglePhotosImportUtils;
import org.datatransferproject.datatransfer.google.common.gphotos.GPhotosUpload;
import org.datatransferproject.datatransfer.google.mediaModels.BatchMediaItemResponse;
import org.datatransferproject.datatransfer.google.mediaModels.GoogleAlbum;
import org.datatransferproject.datatransfer.google.mediaModels.NewMediaItem;
import org.datatransferproject.datatransfer.google.mediaModels.NewMediaItemResult;
import org.datatransferproject.datatransfer.google.mediaModels.NewMediaItemUpload;
import org.datatransferproject.datatransfer.google.mediaModels.Status;
import org.datatransferproject.datatransfer.google.photos.GooglePhotosInterface;
import org.datatransferproject.datatransfer.google.photos.PhotoResult;
import org.datatransferproject.spi.cloud.connection.ConnectionProvider;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore.InputStreamWrapper;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.spi.transfer.i18n.BaseMultilingualDictionary;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.idempotentexecutor.ItemImportResult;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.datatransferproject.spi.transfer.types.UploadErrorException;
import org.datatransferproject.types.common.DownloadableItem;
import org.datatransferproject.types.common.ImportableItem;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class GoogleMediaImporter
    implements Importer<TokensAndUrlAuthData, MediaContainerResource> {

  private final GoogleCredentialFactory credentialFactory;
  private final JobStore jobStore;
  // TODO(aksingh737) why does one half of the Google photos interactions rely on DTP's
  // TemporaryPerJobDataStore and the other half on. JobStore - how do these relate? can they be
  // consilidated everywhere? at least in this class?
  private final TemporaryPerJobDataStore dataStore;
  private final JsonFactory jsonFactory;
  private final ConnectionProvider connectionProvider;
  private final Monitor monitor;
  private final double writesPerSecond;
  private final Map<UUID, GooglePhotosInterface> photosInterfacesMap;
  // TODO(aksingh737) delete the two interface-management approaches (map vs. singleton); the
  // singleton appears to have been left behind during PR #882
  private final GooglePhotosInterface photosInterface;
  private final HashMap<UUID, BaseMultilingualDictionary> multilingualStrings = new HashMap<>();
  private final PhotosLibraryClient photosLibraryClient;

  // We partition into groups of 49 as 50 is the maximum number of items that can be created
  // in one call. (We use 49 to avoid potential off by one errors)
  // https://developers.google.com/photos/library/guides/upload-media#creating-media-item
  private static final int BATCH_UPLOAD_SIZE = 49;

  public GoogleMediaImporter(
      GoogleCredentialFactory credentialFactory,
      JobStore jobStore,
      TemporaryPerJobDataStore dataStore,
      JsonFactory jsonFactory,
      Monitor monitor,
      double writesPerSecond) {
    this(
        credentialFactory,
        jobStore,
        dataStore,
        jsonFactory,
        new HashMap<>(),  /*photosInterfacesMap*/
        null,  /*photosInterface*/
        null,  /*photosLibraryClient*/
        new ConnectionProvider(jobStore),
        monitor,
        writesPerSecond);
  }

  @VisibleForTesting
  GoogleMediaImporter(
      GoogleCredentialFactory credentialFactory,
      JobStore jobStore,
      TemporaryPerJobDataStore dataStore,
      JsonFactory jsonFactory,
      Map<UUID, GooglePhotosInterface> photosInterfacesMap,
      GooglePhotosInterface photosInterface,
      PhotosLibraryClient photosLibraryClient,
      ConnectionProvider connectionProvider,
      Monitor monitor,
      double writesPerSecond) {
    this.credentialFactory = credentialFactory;
    this.jobStore = jobStore;
    this.dataStore = dataStore;
    this.jsonFactory = jsonFactory;
    this.photosInterfacesMap = photosInterfacesMap;
    this.photosInterface = photosInterface;
    this.photosLibraryClient = photosLibraryClient;
    this.connectionProvider = connectionProvider;
    this.monitor = monitor;
    this.writesPerSecond = writesPerSecond;
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentImportExecutor,
      TokensAndUrlAuthData authData,
      MediaContainerResource data)
      throws Exception {
    if (data == null) {
      // Nothing to do
      return ImportResult.OK;
    }
    final GPhotosUpload gPhotosUpload = new GPhotosUpload(jobId, idempotentImportExecutor, authData);

    // Uploads album metadata
    for (MediaAlbum album : data.getAlbums()) {
      idempotentImportExecutor.executeAndSwallowIOExceptions(
          album.getId(), album.getName(), () -> importSingleAlbum(jobId, authData, album));
    }

    long bytes =
        importPhotos(data.getPhotos(), gPhotosUpload)
        + importVideos(data.getVideos(), gPhotosUpload);

    final ImportResult result = ImportResult.OK;
    return result.copyWithBytes(bytes);
  }

  // TODO(aksingh737,jzacsh) fix unit tests across Google adapters to stop testing internal methods
  // like these, and just test importItem() (of
  // org.datatransferproject.spi.transfer.provider.Importer interface).
  @VisibleForTesting
  String importSingleAlbum(UUID jobId, TokensAndUrlAuthData authData, MediaAlbum inputAlbum)
      throws IOException, InvalidTokenException, PermissionDeniedException, UploadErrorException {
    // Set up album
    GoogleAlbum googleAlbum = new GoogleAlbum();
    googleAlbum.setTitle(GooglePhotosImportUtils.cleanAlbumTitle(inputAlbum.getName()));

    GoogleAlbum responseAlbum =
        getOrCreatePhotosInterface(jobId, authData).createAlbum(googleAlbum);
    return responseAlbum.getId();
  }

  long importPhotos(
      Collection<PhotoModel> photos,
      GPhotosUpload gPhotosUpload)
      throws Exception {
    return gPhotosUpload.uploadItemsViaBatching(
        photos,
        this::importPhotoBatch);
  }

  // TODO(aksingh737,jzacsh) if we consolidate underlying gphtoos SDK approaches, then this and the
  // GoogleVideosInterface.uploadBatchOfVideos function can be de-duped. Right now they interact with
  // differnet APIs and while maybe possible, probably better to spend time on de-duping the
  // underlying SDK wrappers first.
  private long importPhotoBatch(
      UUID jobId,
      TokensAndUrlAuthData authData,
      List<PhotoModel> photos,
      IdempotentImportExecutor executor,
      String albumId)
      throws Exception {
    final ArrayList<NewMediaItem> mediaItems = new ArrayList<>();
    final HashMap<String, PhotoModel> uploadTokenToDataId = new HashMap<>();
    final HashMap<String, Long> uploadTokenToLength = new HashMap<>();

    // TODO: resumable uploads https://developers.google.com/photos/library/guides/resumable-uploads
    //  Resumable uploads would allow the upload of larger media that don't fit in memory.  To do
    //  this however, seems to require knowledge of the total file size.
    for (PhotoModel photo : photos) {
      Long size = null;
      try {
        InputStreamWrapper streamWrapper = connectionProvider
            .getInputStreamForItem(jobId, photo);

        try (InputStream s = streamWrapper.getStream()) {
          String uploadToken = getOrCreatePhotosInterface(jobId, authData).uploadMediaContent(s,
              photo.getSha1());
          String description = GooglePhotosImportUtils.cleanDescription(photo.getDescription());
          mediaItems.add(new NewMediaItem(description, uploadToken));
          uploadTokenToDataId.put(uploadToken, photo);
          size = streamWrapper.getBytes();
          uploadTokenToLength.put(uploadToken, size);
        } catch (UploadErrorException e) {
          if (e.getMessage().contains(ERROR_HASH_MISMATCH)) {
            monitor.severe(
                () -> format("%s: SHA-1 (%s) mismatch during upload", jobId, photo.getSha1()));
          }

          Long finalSize = size;
          executor.importAndSwallowIOExceptions(photo, p -> ItemImportResult.error(e, finalSize));
        }

        try {
          if (photo.isInTempStore()) {
            jobStore.removeData(jobId, photo.getFetchableUrl());
          }
        } catch (Exception e) {
          // Swallow the exception caused by Remove data so that existing flows continue
          monitor.info(
              () ->
                  format(
                      "%s: Exception swallowed in removeData call for localPath %s",
                      jobId, photo.getFetchableUrl()),
              e);
        }
      } catch (IOException exception) {
        Long finalSize = size;
        executor.importAndSwallowIOExceptions(
            photo, p -> ItemImportResult.error(exception, finalSize));
      }
    }

    if (mediaItems.isEmpty()) {
      // Either we were not passed in any videos or we failed upload on all of them.
      return 0L;
    }

    long totalBytes = 0L;
    NewMediaItemUpload uploadItem = new NewMediaItemUpload(albumId, mediaItems);
    try {
      BatchMediaItemResponse photoCreationResponse =
          getOrCreatePhotosInterface(jobId, authData).createPhotos(uploadItem);
      Preconditions.checkNotNull(photoCreationResponse);
      NewMediaItemResult[] mediaItemResults = photoCreationResponse.getResults();
      Preconditions.checkNotNull(mediaItemResults);
      for (NewMediaItemResult mediaItem : mediaItemResults) {
        PhotoModel photo = uploadTokenToDataId.get(mediaItem.getUploadToken());
        totalBytes +=
            processMediaResult(
                mediaItem, photo, executor, uploadTokenToLength.get(mediaItem.getUploadToken()));
        uploadTokenToDataId.remove(mediaItem.getUploadToken());
      }

      if (!uploadTokenToDataId.isEmpty()) {
        for (Entry<String, PhotoModel> entry : uploadTokenToDataId.entrySet()) {
          PhotoModel photo = entry.getValue();
          executor.importAndSwallowIOExceptions(
              photo,
              p ->
                  ItemImportResult.error(
                      new IOException("Photo was missing from results list."),
                      uploadTokenToLength.get(entry.getKey())));
        }
      }
    } catch (IOException e) {
      if (StringUtils.contains(
          e.getMessage(), "The remaining storage in the user's account is not enough")) {
        throw new DestinationMemoryFullException("Google destination storage full", e);
      } else if (StringUtils.contains(
          e.getMessage(), "The provided ID does not match any albums")) {
        // which means the album was likely deleted by the user
        // we skip this batch and log some data to understand it better
        logMissingAlbumDetails(jobId, authData, albumId, e);
      } else {
        throw e;
      }
    }

    return totalBytes;
  }

  long importVideos(
      Collection<VideoModel> videos,
      GPhotosUpload gPhotosUpload)
      throws Exception {
    return gPhotosUpload.uploadItemsViaBatching(
        videos,
        this::importVideosBatch);
  }


  private long importVideosBatch(
      UUID jobId,
      TokensAndUrlAuthData authData,
      List<VideoModel> batch,
      IdempotentImportExecutor executor,
      String albumId)
      throws Exception {
    return uploadBatchOfVideos(
          jobId,
          batch,
          dataStore,
          photosLibraryClient,
          executor,
          connectionProvider,
          monitor);
  }

  private void logMissingAlbumDetails(
      UUID jobId, TokensAndUrlAuthData authData, String albumId, IOException e) {
    monitor.info(
        () -> format("Can't find album during createPhotos call, album is likely deleted"), e);
    try {
      GoogleAlbum album = getOrCreatePhotosInterface(jobId, authData).getAlbum(albumId);
      monitor.debug(
          () ->
              format(
                  "Can't find album during createPhotos call, album info: isWriteable %b, mediaItemsCount %d",
                  album.getIsWriteable(), album.getMediaItemsCount()),
          e);
    } catch (Exception ex) {
      monitor.info(() -> format("Can't find album during getAlbum call"), ex);
    }
  }

  private long processMediaResult(
      NewMediaItemResult mediaItem,
      ImportableItem item,
      IdempotentImportExecutor executor,
      long bytes)
      throws Exception {
    Status status = mediaItem.getStatus();
    if (status.getCode() == Code.OK_VALUE) {
      PhotoResult photoResult = new PhotoResult(mediaItem.getMediaItem().getId(), bytes);
      executor.importAndSwallowIOExceptions(
          item, itemToImport -> ItemImportResult.success(photoResult, bytes));
      return bytes;
    } else {
      executor.importAndSwallowIOExceptions(
          item,
          itemToImport ->
              ItemImportResult.error(
                  new IOException(
                      String.format(
                          "Media item could not be created. Code: %d Message: %s",
                          status.getCode(), status.getMessage())),
                  bytes));
      return 0;
    }
  }

  private synchronized GooglePhotosInterface getOrCreatePhotosInterface(
      UUID jobId, TokensAndUrlAuthData authData) {

    if (photosInterface != null) {
      return photosInterface;
    }

    if (photosInterfacesMap.containsKey(jobId)) {
      return photosInterfacesMap.get(jobId);
    }

    GooglePhotosInterface newInterface = makePhotosInterface(authData);
    photosInterfacesMap.put(jobId, newInterface);

    return newInterface;
  }

  private synchronized GooglePhotosInterface makePhotosInterface(TokensAndUrlAuthData authData) {
    Credential credential = credentialFactory.createCredential(authData);
    return new GooglePhotosInterface(
        credentialFactory, credential, jsonFactory, monitor, writesPerSecond);
  }

  private synchronized BaseMultilingualDictionary getOrCreateStringDictionary(UUID jobId) {
    if (!multilingualStrings.containsKey(jobId)) {
      PortabilityJob job = jobStore.findJob(jobId);
      String locale = job != null ? job.userLocale() : null;
      multilingualStrings.put(jobId, new BaseMultilingualDictionary(locale));
    }

    return multilingualStrings.get(jobId);
  }
}
