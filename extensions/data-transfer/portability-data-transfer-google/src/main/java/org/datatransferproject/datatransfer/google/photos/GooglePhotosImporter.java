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
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.google.rpc.Code;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.spi.transfer.i18n.BaseMultilingualDictionary;
import org.datatransferproject.spi.transfer.i18n.BaseMultilingualString;
import org.datatransferproject.datatransfer.google.mediaModels.BatchMediaItemResponse;
import org.datatransferproject.datatransfer.google.mediaModels.GoogleAlbum;
import org.datatransferproject.datatransfer.google.mediaModels.NewMediaItem;
import org.datatransferproject.datatransfer.google.mediaModels.NewMediaItemResult;
import org.datatransferproject.datatransfer.google.mediaModels.NewMediaItemUpload;
import org.datatransferproject.datatransfer.google.mediaModels.Status;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore.InputStreamWrapper;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.datatransferproject.transfer.ImageStreamProvider;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import static java.lang.String.format;

public class GooglePhotosImporter
    implements Importer<TokensAndUrlAuthData, PhotosContainerResource> {

  private final GoogleCredentialFactory credentialFactory;
  private final JobStore jobStore;
  private final JsonFactory jsonFactory;
  private final ImageStreamProvider imageStreamProvider;
  private final Monitor monitor;
  private final double writesPerSecond;
  private volatile Map<UUID, GooglePhotosInterface> photosInterfacesMap;
  private volatile GooglePhotosInterface photosInterface;
  private volatile HashMap<UUID, BaseMultilingualDictionary> multilingualStrings =
      new HashMap<>();

  public GooglePhotosImporter(
      GoogleCredentialFactory credentialFactory,
      JobStore jobStore,
      JsonFactory jsonFactory,
      Monitor monitor,
      double writesPerSecond) {
    this(
        credentialFactory,
        jobStore,
        jsonFactory,
        new HashMap<>(),
        null,
        new ImageStreamProvider(),
        monitor,
        writesPerSecond);
  }

  @VisibleForTesting
  GooglePhotosImporter(
      GoogleCredentialFactory credentialFactory,
      JobStore jobStore,
      JsonFactory jsonFactory,
      Map<UUID, GooglePhotosInterface> photosInterfacesMap,
      GooglePhotosInterface photosInterface,
      ImageStreamProvider imageStreamProvider,
      Monitor monitor,
      double writesPerSecond) {
    this.credentialFactory = credentialFactory;
    this.jobStore = jobStore;
    this.jsonFactory = jsonFactory;
    this.photosInterfacesMap = photosInterfacesMap;
    this.photosInterface = photosInterface;
    this.imageStreamProvider = imageStreamProvider;
    this.monitor = monitor;
    this.writesPerSecond = writesPerSecond;
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentImportExecutor,
      TokensAndUrlAuthData authData,
      PhotosContainerResource data)
      throws Exception {
    if (data == null) {
      // Nothing to do
      return ImportResult.OK;
    }

    // Uploads album metadata
    if (data.getAlbums() != null && data.getAlbums().size() > 0) {
      for (PhotoAlbum album : data.getAlbums()) {
        idempotentImportExecutor.executeAndSwallowIOExceptions(
            album.getId(), album.getName(), () -> importSingleAlbum(jobId, authData, album));
      }
    }

    long bytes = 0L;
    // Uploads photos
    Collection<PhotoModel> photos = data.getPhotos();
    if (photos != null && photos.size() > 0) {
      Map<String, List<PhotoModel>> photosByAlbum =
          photos.stream()
              .filter(photo -> !idempotentImportExecutor.isKeyCached(getIdempotentId(photo)))
              .collect(Collectors.groupingBy(PhotoModel::getAlbumId));
      for (Entry<String, List<PhotoModel>> albumEntry : photosByAlbum.entrySet()) {
        String originalAlbumId = albumEntry.getKey();
        String googleAlbumId;
        if (Strings.isNullOrEmpty(originalAlbumId)) {
          // This is ok, since NewMediaItemUpload will ignore all null values and it's possible to
          // upload a NewMediaItem without a corresponding album id.
          googleAlbumId = null;
        } else {
          // Note this will throw if creating the album failed, which is what we want
          // because that will also mark this photo as being failed.
          googleAlbumId = idempotentImportExecutor.getCachedValue(originalAlbumId);
        }

        // We partition into groups of 49 as 50 is the maximum number of items that can be created
        // in one call. (We use 49 to avoid potential off by one errors)
        // https://developers.google.com/photos/library/guides/upload-media#creating-media-item
        UnmodifiableIterator<List<PhotoModel>> batches =
            Iterators.partition(albumEntry.getValue().iterator(), 49);
        while (batches.hasNext()) {
          long batchBytes =
              importPhotoBatch(
                  jobId, authData, batches.next(), idempotentImportExecutor, googleAlbumId);
          bytes += batchBytes;
        }
      }
    }

    final ImportResult result = ImportResult.OK;
    return result.copyWithBytes(bytes);
  }

  @VisibleForTesting
  String importSingleAlbum(UUID jobId, TokensAndUrlAuthData authData, PhotoAlbum inputAlbum)
      throws IOException, InvalidTokenException, PermissionDeniedException {
    // Set up album
    GoogleAlbum googleAlbum = new GoogleAlbum();
    String copyOf = getOrCreateStringDictionary(jobId).get(BaseMultilingualString.CopyOf);
    String title = MessageFormat.format(copyOf, Strings.nullToEmpty(inputAlbum.getName()));

    // Album titles are restricted to 500 characters
    // https://developers.google.com/photos/library/guides/manage-albums#creating-new-album
    if (title.length() > 500) {
      title = title.substring(0, 497) + "...";
    }
    googleAlbum.setTitle(title);

    GoogleAlbum responseAlbum =
        getOrCreatePhotosInterface(jobId, authData).createAlbum(googleAlbum);
    return responseAlbum.getId();
  }

  long importPhotoBatch(
      UUID jobId,
      TokensAndUrlAuthData authData,
      List<PhotoModel> photos,
      IdempotentImportExecutor executor,
      String albumId)
      throws Exception {
    final ArrayList<NewMediaItem> mediaItems = new ArrayList<>();
    final HashMap<String, PhotoModel> uploadTokenToDataId = new HashMap<>();
    final HashMap<String, Long> uploadTokenToLength = new HashMap<>();

    /*
    TODO: resumable uploads https://developers.google.com/photos/library/guides/resumable-uploads
    Resumable uploads would allow the upload of larger media that don't fit in memory.  To do this,
    however, seems to require knowledge of the total file size.
    */
    // Upload photos
    for (PhotoModel photo : photos) {
      try {
        InputStream inputStream;
        long bytes;
        if (photo.isInTempStore()) {
          final InputStreamWrapper streamWrapper =
              jobStore.getStream(jobId, photo.getFetchableUrl());
          bytes = streamWrapper.getBytes();
          inputStream = streamWrapper.getStream();
        } else {
          HttpURLConnection conn = imageStreamProvider.getConnection(photo.getFetchableUrl());
          final long contentLengthLong = conn.getContentLengthLong();
          bytes = contentLengthLong != -1 ? contentLengthLong : 0;
          inputStream = conn.getInputStream();
        }

        String uploadToken =
            getOrCreatePhotosInterface(jobId, authData).uploadPhotoContent(inputStream);
        mediaItems.add(new NewMediaItem(getPhotoDescription(jobId, photo), uploadToken));
        uploadTokenToDataId.put(uploadToken, photo);
        uploadTokenToLength.put(uploadToken, bytes);
        try {
          if (photo.isInTempStore()) {
            jobStore.removeData(jobId, photo.getFetchableUrl());
          }
        } catch (Exception e) {
          // Swallow the exception caused by Remove data so that existing flows continue
          monitor.info(
                  () -> format("%s: Exception swallowed in removeData call for localPath %s",
                          jobId, photo.getFetchableUrl()), e);
        }
      } catch (IOException e) {
        executor.executeAndSwallowIOExceptions(
            getIdempotentId(photo),
            photo.getTitle(),
            () -> {
              throw e;
            });
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
        String uploadToken = mediaItem.getUploadToken();
        Status status = mediaItem.getStatus();

        PhotoModel photo = uploadTokenToDataId.get(uploadToken);
        Preconditions.checkNotNull(photo);
        if (status.getCode() == Code.OK_VALUE) {
          Long bytes = uploadTokenToLength.get(uploadToken);
          Preconditions.checkNotNull(bytes);
          executor.executeAndSwallowIOExceptions(
              getIdempotentId(photo),
              photo.getTitle(),
              () -> new PhotoResult(mediaItem.getMediaItem().getId(), bytes));
          totalBytes += bytes;
        } else {
          executor.executeAndSwallowIOExceptions(
              getIdempotentId(photo),
              photo.getTitle(),
              () -> {
                throw new IOException(
                    String.format(
                        "Photo could not be created. Code: %d Message: %s",
                        status.getCode(), status.getMessage()));
              });
        }
        uploadTokenToDataId.remove(uploadToken);
      }
      if (!uploadTokenToDataId.isEmpty()) {
        for (PhotoModel photo : uploadTokenToDataId.values()) {
          executor.executeAndSwallowIOExceptions(
              getIdempotentId(photo),
              photo.getTitle(),
              () -> {
                throw new IOException("Photo was missing from results list.");
              });
        }
      }

    } catch (IOException e) {
      if (e.getMessage() != null
          && e.getMessage().contains("The remaining storage in the user's account is not enough")) {
        throw new DestinationMemoryFullException("Google destination storage full", e);
      } else {
        throw e;
      }
    }

    return totalBytes;
  }

  String getIdempotentId(PhotoModel photo) {
    return photo.getAlbumId() + "-" + photo.getDataId();
  }

  private String getPhotoDescription(UUID jobId, PhotoModel inputPhoto) {
    String description;
    if (Strings.isNullOrEmpty(inputPhoto.getDescription())) {
      description = "";
    } else {
      String copyOf = getOrCreateStringDictionary(jobId).get(BaseMultilingualString.CopyOf);
      description = MessageFormat.format(copyOf, inputPhoto.getDescription());
      // Descriptions are restricted to 1000 characters
      // https://developers.google.com/photos/library/guides/upload-media#creating-media-item
      if (description.length() > 1000) {
        description = description.substring(0, 997) + "...";
      }
    }
    return description;
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
