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

import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.uploader.UploadMetaData;
import java.io.BufferedInputStream;
import java.io.IOException;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.transfer.provider.ImportResult;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.models.photos.PhotoAlbum;
import org.dataportabilityproject.types.transfer.models.photos.PhotoModel;
import org.dataportabilityproject.types.transfer.models.photos.PhotosContainerResource;

public class FlickrPhotosImporter implements Importer<AuthData, PhotosContainerResource> {
  private final JobStore jobStore;

  FlickrPhotosImporter(JobStore jobStore){
    this.jobStore = jobStore;
  }

  @Override
  public ImportResult importItem(String jobId, AuthData authData, PhotosContainerResource data) {
    for(PhotoAlbum album : data.getAlbums()) {
      String key = album.getId();
      // TODO: store key and album into the JobStore
    }

    for (PhotoModel photo : data.getPhotos()) {
      try{
      String photoId = uploadPhoto(photo);
      } catch(FlickrException | IOException e) {
        throw new IllegalArgumentException(e);
      }

      String oldPhotoId = photo.getAlbumId();

      if(/*!jobStore.hasKey(old album)*/false) {
        // get data for albummetadata+oldalbum id
        // use photosetsinterface to create the prefix of the albmu
        // store the old album id into the jobdata cache
      } else {
        // old alsums created in the cache, add the photo to the album
      }
    }
    return null;
  }

  private String uploadPhoto(PhotoModel photo) throws IOException, FlickrException {
    BufferedInputStream inStream ;//= imageStreamProvider.get(photo.getFetchableUrl());
    UploadMetaData uploadMetaData =
        new UploadMetaData()
            .setAsync(false)
            .setPublicFlag(false)
            .setFriendFlag(false)
            .setFamilyFlag(false)
            .setTitle(/*COPY_PREFIX + */ photo.getTitle())
            .setDescription(photo.getDescription());
    return "blah"; // uploader.upload(inStream, uploadMetaData);
  }

}
