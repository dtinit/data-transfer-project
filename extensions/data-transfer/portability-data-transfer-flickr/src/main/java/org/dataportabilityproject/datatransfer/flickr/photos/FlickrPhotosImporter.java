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

package org.dataportabilityproject.datatransfer.flickr.photos;

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
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.transfer.provider.ImportResult;
import org.dataportabilityproject.spi.transfer.provider.ImportResult.ResultType;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.spi.transfer.types.TempPhotosData;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.models.photos.PhotoAlbum;
import org.dataportabilityproject.types.transfer.models.photos.PhotoModel;
import org.dataportabilityproject.types.transfer.models.photos.PhotosContainerResource;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.UUID;

public class FlickrPhotosImporter implements Importer<AuthData, PhotosContainerResource> {
  @VisibleForTesting static final String CACHE_ALBUM_METADATA_PREFIX = "meta-";
  @VisibleForTesting static final String COPY_PREFIX = "Copy of - ";
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
      return new ImportResult(
          ImportResult.ResultType.ERROR, "Error authorizing Flickr Auth: " + e.getErrorMessage());
    }
    RequestContext.getRequestContext().setAuth(auth);

    TempPhotosData tempPhotosData = jobStore.findData(TempPhotosData.class, jobId);
    if (tempPhotosData == null) {
      tempPhotosData = new TempPhotosData(jobId);
      jobStore.create(jobId, tempPhotosData);
    }

    Preconditions.checkArgument(
        data.getAlbums() != null || data.getPhotos() != null,
        "" + "Error: There is no data to import");

    if (data.getAlbums() != null) {
      importAlbums(data.getAlbums(), tempPhotosData);
      jobStore.update(jobId, tempPhotosData);
    }

    if (data.getPhotos() != null) {
      for(PhotoModel photo : data.getPhotos()) {
        try {
          importSinglePhoto(jobId, photo);
        } catch (FlickrException | IOException e) {
          return new ImportResult(ResultType.ERROR, "Error importing photo " + e.getMessage());
        }
      }
    }

    return new ImportResult(ImportResult.ResultType.OK);
  }

  // Store any album data in the cache because Flickr only allows you to create an album with a
  // photo in it, so we have to wait for the first photo to create the album
  private void importAlbums(Collection<PhotoAlbum> albums, TempPhotosData tempPhotosData) {
    for (PhotoAlbum album : albums) {
      tempPhotosData.addTempAlbumMapping(CACHE_ALBUM_METADATA_PREFIX + album.getId(), album);
    }
  }

  private void importSinglePhoto(UUID id, PhotoModel photo)
      throws FlickrException, IOException {
      String photoId = uploadPhoto(photo);

      // TODO: what happens when the photo isn't associated with an album?
      String oldAlbumId = photo.getAlbumId();
      TempPhotosData tempData = jobStore.findData(TempPhotosData.class, id);
      String newAlbumId = tempData.lookupNewAlbumId(oldAlbumId);
      if (Strings.isNullOrEmpty(newAlbumId)) {
        // This means that we havent created the new album yet, create the photoset
        PhotoAlbum album = tempData.lookupTempAlbum(CACHE_ALBUM_METADATA_PREFIX + oldAlbumId);
        Photoset photoset =
            photosetsInterface.create(
                COPY_PREFIX + album.getName(), album.getDescription(), photoId);
        tempData.addAlbumId(oldAlbumId, photoset.getId());
        tempData.removeTempPhotoAlbum(CACHE_ALBUM_METADATA_PREFIX + oldAlbumId);
      } else {
        // We've already created a new album, add the photo to the new album
        photosetsInterface.addPhoto(newAlbumId, photoId);
      }
      jobStore.update(id, tempData);
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
