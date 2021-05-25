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

package org.datatransferproject.datatransfer.flickr.photos;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.RequestContext;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.photosets.Photoset;
import com.flickr4java.flickr.photosets.PhotosetsInterface;
import com.flickr4java.flickr.uploader.UploadMetaData;
import com.flickr4java.flickr.uploader.Uploader;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.RateLimiter;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.serviceconfig.TransferServiceConfig;

public class FlickrPhotosImporter implements Importer<AuthData, PhotosContainerResource> {

  @VisibleForTesting static final String COPY_PREFIX = "Copy of - ";
  @VisibleForTesting static final String ORIGINAL_ALBUM_PREFIX = "original-album-";

  private final TemporaryPerJobDataStore jobStore;
  private final Flickr flickr;
  private final Uploader uploader;
  private final ImageStreamProvider imageStreamProvider;
  private final PhotosetsInterface photosetsInterface;
  private final Monitor monitor;
  private final RateLimiter perUserRateLimiter;

  public FlickrPhotosImporter(
      AppCredentials appCredentials,
      TemporaryPerJobDataStore jobStore,
      Monitor monitor,
      TransferServiceConfig serviceConfig) {
    this.jobStore = jobStore;
    this.flickr = new Flickr(appCredentials.getKey(), appCredentials.getSecret(), new REST());
    this.uploader = flickr.getUploader();
    this.imageStreamProvider = new ImageStreamProvider();
    this.photosetsInterface = flickr.getPhotosetsInterface();
    this.monitor = monitor;
    this.perUserRateLimiter = serviceConfig.getPerUserRateLimiter();
  }

  @VisibleForTesting
  FlickrPhotosImporter(
      Flickr flickr,
      TemporaryPerJobDataStore jobstore,
      ImageStreamProvider imageStreamProvider,
      Monitor monitor,
      TransferServiceConfig serviceConfig) {
    this.flickr = flickr;
    this.imageStreamProvider = imageStreamProvider;
    this.jobStore = jobstore;
    this.uploader = flickr.getUploader();
    this.photosetsInterface = flickr.getPhotosetsInterface();
    this.monitor = monitor;
    this.perUserRateLimiter = serviceConfig.getPerUserRateLimiter();
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      AuthData authData,
      PhotosContainerResource data)
      throws Exception, IOException {
    Auth auth;
    try {
      auth = FlickrUtils.getAuth(authData, flickr);
    } catch (FlickrException e) {
      return new ImportResult(e);
    }
    RequestContext.getRequestContext().setAuth(auth);

    Preconditions.checkArgument(
        data.getAlbums() != null || data.getPhotos() != null, "Error: There is no data to import");

    if (data.getAlbums() != null) {
      storeAlbumbs(jobId, data.getAlbums());
    }

    if (data.getPhotos() != null) {
      for (PhotoModel photo : data.getPhotos()) {
        try {
          importSinglePhoto(idempotentExecutor, jobId, photo);
        } catch (FlickrException e) {
          if (e.getMessage().contains("Upload limit reached")) {
            throw new DestinationMemoryFullException("Flickr destination memory reached", e);
          } else if (e.getMessage().contains("Photo already in set")) {
            // This can happen if we got a server error on our end, but the request went through.
            // When our retry strategy kicked in the request was complete and the photo already
            // uploaded
            continue;
          }
          throw new IOException(e);
        }
      }
    }

    return new ImportResult(ImportResult.ResultType.OK);
  }

  // Store any album data in the cache because Flickr only allows you to create an album with a
  // photo in it, so we have to wait for the first photo to create the album
  private void storeAlbumbs(UUID jobId, Collection<PhotoAlbum> albums) throws IOException {
    for (PhotoAlbum album : albums) {
      jobStore.create(
          jobId,
          ORIGINAL_ALBUM_PREFIX + album.getId(),
          new FlickrTempPhotoData(album.getName(), album.getDescription()));
    }
  }

  private void importSinglePhoto(
      IdempotentImportExecutor idempotentExecutor, UUID id, PhotoModel photo) throws Exception {
    String photoId =
        idempotentExecutor.executeAndSwallowIOExceptions(
            photo.getAlbumId() + "-" + photo.getDataId(),
            photo.getTitle(),
            () -> uploadPhoto(photo, id));
    if (photoId == null) {
      return;
    }

    String oldAlbumId = photo.getAlbumId();

    // If the photo wasn't associated with an album, we don't have to do anything else, since we've
    // already uploaded it above. This will mean it lives in the user's cameraroll and not in an
    // album.
    // If the uploadPhoto() call fails above, an exception will be thrown, so we don't have to worry
    // about the photo not being uploaded here.
    if (Strings.isNullOrEmpty(oldAlbumId)) {
      return;
    }
    createOrAddToAlbum(idempotentExecutor, id, photo.getAlbumId(), photoId);
  }

  private void createOrAddToAlbum(
      IdempotentImportExecutor idempotentExecutor, UUID jobId, String oldAlbumId, String photoId)
      throws Exception {
    if (idempotentExecutor.isKeyCached(oldAlbumId)) {
      String newAlbumId = idempotentExecutor.getCachedValue(oldAlbumId);
      // We've already created the album this photo belongs in, simply add it to the new album
      photosetsInterface.addPhoto(newAlbumId, photoId);
    } else {
      createAlbum(idempotentExecutor, jobId, oldAlbumId, photoId);
    }
  }

  private void createAlbum(
      IdempotentImportExecutor idempotentExecutor,
      UUID jobId,
      String oldAlbumId,
      String firstPhotoId)
      throws Exception {
    // This means that we havent created the new album yet, create the photoset
    FlickrTempPhotoData album =
        jobStore.findData(jobId, ORIGINAL_ALBUM_PREFIX + oldAlbumId, FlickrTempPhotoData.class);

    // TODO: handle what happens if the album doesn't exist. One of the things we can do here is
    // throw them into a default album or add a finalize() step in the Importer which can deal
    // with these (in case the album exists later).
    Preconditions.checkNotNull(album, "Album not found: " + oldAlbumId);

    idempotentExecutor.executeAndSwallowIOExceptions(
        oldAlbumId,
        album.getName(),
        () -> {
          // TODO: make COPY_PREFIX configurable.
          String albumName =
              Strings.isNullOrEmpty(album.getName()) ? "untitled" : COPY_PREFIX + album.getName();
          String albumDescription = cleanString(album.getDescription());

          perUserRateLimiter.acquire();
          Photoset photoset = photosetsInterface.create(albumName, albumDescription, firstPhotoId);
          monitor.debug(() -> String.format("Flickr importer created album: %s", album));
          return photoset.getId();
        });
  }

  private String uploadPhoto(PhotoModel photo, UUID jobId) throws IOException, FlickrException {
    BufferedInputStream inStream = imageStreamProvider.get(photo.getFetchableUrl());
    // TODO: do we want to keep COPY_PREFIX?  I think not
    String photoTitle =
        Strings.isNullOrEmpty(photo.getTitle()) ? "" : COPY_PREFIX + photo.getTitle();
    String photoDescription = cleanString(photo.getDescription());

    UploadMetaData uploadMetaData =
        new UploadMetaData()
            .setAsync(false)
            .setPublicFlag(false)
            .setFriendFlag(false)
            .setFamilyFlag(false)
            .setTitle(photoTitle)
            .setDescription(photoDescription);
    perUserRateLimiter.acquire();
    String uploadResult = uploader.upload(inStream, uploadMetaData);
    inStream.close();
    monitor.debug(() -> String.format("%s: Flickr importer uploading photo: %s", jobId, photo));
    return uploadResult;
  }

  private static String cleanString(String string) {
    return Strings.isNullOrEmpty(string) ? "" : string;
  }

  @VisibleForTesting
  class ImageStreamProvider {

    /**
     * Gets an input stream to an image, given its URL. Used by {@link FlickrPhotosImporter} to
     * upload the image.
     */
    public BufferedInputStream get(String urlStr) throws IOException {
      URL url = new URL(urlStr);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.connect();
      return new BufferedInputStream(conn.getInputStream());
    }
  }
}
