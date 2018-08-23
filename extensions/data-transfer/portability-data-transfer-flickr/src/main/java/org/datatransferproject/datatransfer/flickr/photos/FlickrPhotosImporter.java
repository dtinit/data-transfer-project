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
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.UUID;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.ImportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.spi.transfer.types.TempPhotosData;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.models.photos.PhotoAlbum;
import org.datatransferproject.types.transfer.models.photos.PhotoModel;
import org.datatransferproject.types.transfer.models.photos.PhotosContainerResource;

public class FlickrPhotosImporter implements Importer<AuthData, PhotosContainerResource> {
  @VisibleForTesting static final String COPY_PREFIX = "Copy of - ";
  @VisibleForTesting static final String DEFAULT_ALBUM = "Default";

  private final JobStore jobStore;
  private final Flickr flickr;
  private final Uploader uploader;
  private final ImageStreamProvider imageStreamProvider;
  private final PhotosetsInterface photosetsInterface;

  public FlickrPhotosImporter(AppCredentials appCredentials, JobStore jobStore) {
    this.jobStore = jobStore;
    this.flickr = new Flickr(appCredentials.getKey(), appCredentials.getSecret(), new REST());
    this.uploader = flickr.getUploader();
    this.imageStreamProvider = new ImageStreamProvider();
    this.photosetsInterface = flickr.getPhotosetsInterface();
  }

  @VisibleForTesting
  FlickrPhotosImporter(Flickr flickr, JobStore jobstore, ImageStreamProvider imageStreamProvider) {
    this.flickr = flickr;
    this.imageStreamProvider = imageStreamProvider;
    this.jobStore = jobstore;
    this.uploader = flickr.getUploader();
    this.photosetsInterface = flickr.getPhotosetsInterface();
  }

  @Override
  public ImportResult importItem(UUID jobId, AuthData authData, PhotosContainerResource data) {
    Auth auth;
    try {
      auth = FlickrUtils.getAuth(authData, flickr);
    } catch (FlickrException e) {
      return new ImportResult(e);
    }
    RequestContext.getRequestContext().setAuth(auth);

    // TODO: store objects containing individual mappings instead of single object containing all mappings
    TempPhotosData tempPhotosData = jobStore.findData(jobId, createCacheKey(), TempPhotosData.class);
    if (tempPhotosData == null) {
      tempPhotosData = new TempPhotosData(jobId);
      try {
        jobStore.create(jobId, createCacheKey(), tempPhotosData);
      } catch (IOException e) {
        return new ImportResult(e);
      }
    }

    Preconditions.checkArgument(
        data.getAlbums() != null || data.getPhotos() != null,
        "Error: There is no data to import");

    if (data.getAlbums() != null) {
      importAlbums(data.getAlbums(), tempPhotosData);
      jobStore.update(jobId, createCacheKey(), tempPhotosData);
    }

    if (data.getPhotos() != null) {
      for (PhotoModel photo : data.getPhotos()) {
        try {
          importSinglePhoto(jobId, photo);
        } catch (FlickrException | IOException e) {
          return new ImportResult(e);
        }
      }
    }

    return new ImportResult(ImportResult.ResultType.OK);
  }

  // Store any album data in the cache because Flickr only allows you to create an album with a
  // photo in it, so we have to wait for the first photo to create the album
  private void importAlbums(Collection<PhotoAlbum> albums, TempPhotosData tempPhotosData) {
    for (PhotoAlbum album : albums) {
      tempPhotosData.addTempAlbumMapping(album.getId(), album);
    }
  }

  private void importSinglePhoto(UUID id, PhotoModel photo) throws FlickrException, IOException {
    String photoId = uploadPhoto(photo);

    String oldAlbumId = photo.getAlbumId();

    // If the photo wasn't associated with an album, we don't have to do anything else, since we've
    // already uploaded it above. This will mean it lives in the user's cameraroll and not in an album.
    // If the uploadPhoto() call fails above, an exception will be thrown, so we don't have to worry
    // about the photo not being uploaded here.
    if (Strings.isNullOrEmpty(oldAlbumId)) {
      return;
    }

    TempPhotosData tempData = jobStore.findData(id, createCacheKey(), TempPhotosData.class);
    String newAlbumId = tempData.lookupNewAlbumId(oldAlbumId);

    if (Strings.isNullOrEmpty(newAlbumId)) {
      // This means that we havent created the new album yet, create the photoset
      PhotoAlbum album = tempData.lookupTempAlbum(oldAlbumId);

      // TODO: handle what happens if the album doesn't exist. One of the things we can do here is
      // throw them into a default album or add a finalize() step in the Importer which can deal
      // with these (in case the album exists later).
      Preconditions.checkArgument(album != null, "Album not found: {}", oldAlbumId);

      Photoset photoset =
          photosetsInterface.create(COPY_PREFIX + album.getName(), album.getDescription(), photoId);

      // Update the temp mapping to reflect that we've created the album
      tempData.addAlbumId(oldAlbumId, photoset.getId());
      tempData.removeTempPhotoAlbum(oldAlbumId);
    } else {
      // We've already created the album this photo belongs in, simply add it to the new album
      photosetsInterface.addPhoto(newAlbumId, photoId);
    }

    jobStore.update(id, createCacheKey(), tempData);
  }

  private String uploadPhoto(PhotoModel photo) throws IOException, FlickrException {
    BufferedInputStream inStream = imageStreamProvider.get(photo.getFetchableUrl());
    UploadMetaData uploadMetaData =
        new UploadMetaData()
            .setAsync(false)
            .setPublicFlag(false)
            .setFriendFlag(false)
            .setFamilyFlag(false)
            .setTitle(COPY_PREFIX + photo.getTitle())
            .setDescription(photo.getDescription());
    return uploader.upload(inStream, uploadMetaData);
  }

  /** Key for cache of album mappings.
   * TODO: Add a method parameter for a {@code key} for fine grained objects.
   */
  private String createCacheKey() {
    // TODO: store objects containing individual mappings instead of single object containing all mappings
    return "tempPhotosData";
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
