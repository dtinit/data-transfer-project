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
import jdk.internal.joptsimple.internal.Strings;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.transfer.provider.ImportResult;
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
import java.util.UUID;

public class FlickrPhotosImporter implements Importer<AuthData, PhotosContainerResource> {
  @VisibleForTesting static final String CACHE_ALBUM_METADATA_PREFIX = "meta-";
  @VisibleForTesting static final String COPY_PREFIX = "Copy of - ";
  private final JobStore jobStore;
  private final Flickr flickr;
  private final Uploader uploader;
  private final ImageStreamProvider imageStreamProvider;
  private final PhotosetsInterface photosetsInterface;

  FlickrPhotosImporter(AppCredentials appCredentials, JobStore jobStore) {
    this.jobStore = jobStore;
    this.flickr = new Flickr(appCredentials.getKey(), appCredentials.getSecret(), new REST());
    this.uploader = flickr.getUploader();
    imageStreamProvider = new ImageStreamProvider();
    photosetsInterface = flickr.getPhotosetsInterface();
  }

  @Override
  public ImportResult importItem(String jobId, AuthData authData, PhotosContainerResource data) {
    Auth auth;
    try {
      auth = FlickrUtils.getAuth(authData, flickr);
    } catch (FlickrException e) {
      return new ImportResult(
          ImportResult.ResultType.ERROR, "Error authorizing Flickr Auth: " + e.getErrorMessage());
    }
    RequestContext.getRequestContext().setAuth(auth);
    UUID id = UUID.fromString(jobId);

    // Store any album data in the cache because Flickr only allows you to create an album with a
    // photo in it, so we have to wait for the first photo to create the album
    TempPhotosData tempPhotosData = jobStore.findData(TempPhotosData.class, id);
    if (tempPhotosData == null) {
      tempPhotosData = new TempPhotosData(jobId);
      jobStore.create(id, tempPhotosData);
    }

    for (PhotoAlbum album : data.getAlbums()) {
      tempPhotosData.addAlbum(CACHE_ALBUM_METADATA_PREFIX + album.getId(), album);
    }
    jobStore.update(id, tempPhotosData);

    for (PhotoModel photo : data.getPhotos()) {
      try {
        String photoId = uploadPhoto(photo);
        String oldAlbumId = photo.getAlbumId();
        TempPhotosData tempData = jobStore.findData(TempPhotosData.class, id);
        String newAlbumId = tempData.lookupNewAlbumId(oldAlbumId);
        if (Strings.isNullOrEmpty(newAlbumId)) {
          // This means that we havent created the new album yet, create the photoset
          PhotoAlbum album = tempData.lookupAlbum(CACHE_ALBUM_METADATA_PREFIX + oldAlbumId);
          Photoset photoset =
              photosetsInterface.create(
                  COPY_PREFIX + album.getName(), album.getDescription(), photoId);
          tempData.addAlbumId(oldAlbumId, photoset.getId());
        } else {
          // We've already created a new album, add the photo to the new album
          photosetsInterface.addPhoto(newAlbumId, photoId);
        }
        jobStore.update(id, tempData);
      } catch (FlickrException | IOException e) {
        // TODO: figure out retries
        return new ImportResult(
            ImportResult.ResultType.ERROR, "Error importing item: " + e.getMessage());
      }
    }
    return new ImportResult(ImportResult.ResultType.OK);
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

  private class ImageStreamProvider {
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
