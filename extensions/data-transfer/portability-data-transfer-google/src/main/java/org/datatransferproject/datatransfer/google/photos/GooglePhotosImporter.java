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
import com.google.common.base.Strings;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.photos.model.GoogleAlbum;
import org.datatransferproject.datatransfer.google.photos.model.NewMediaItem;
import org.datatransferproject.datatransfer.google.photos.model.NewMediaItemUpload;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.spi.transfer.types.TempPhotosData;
import org.datatransferproject.transfer.ImageStreamProvider;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.transfer.models.photos.PhotoAlbum;
import org.datatransferproject.types.transfer.models.photos.PhotoModel;
import org.datatransferproject.types.transfer.models.photos.PhotosContainerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.UUID;

public class GooglePhotosImporter
    implements Importer<TokensAndUrlAuthData, PhotosContainerResource> {

  // TODO: internationalize copy prefix
  private static final String COPY_PREFIX = "Copy of ";
  private static final Logger LOGGER = LoggerFactory.getLogger(GooglePhotosImporter.class);

  private static final String TEMP_PHOTOS_KEY = "tempPhotosData";

  private final GoogleCredentialFactory credentialFactory;
  private final JobStore jobStore;
  private final JsonFactory jsonFactory;
  private final ImageStreamProvider imageStreamProvider;
  private volatile GooglePhotosInterface photosInterface;

  public GooglePhotosImporter(GoogleCredentialFactory credentialFactory, JobStore jobStore,
      JsonFactory jsonFactory) {
    this(credentialFactory, jobStore, jsonFactory, null, new ImageStreamProvider());
  }

  @VisibleForTesting
  GooglePhotosImporter(
      GoogleCredentialFactory credentialFactory,
      JobStore jobStore,
      JsonFactory jsonFactory,
      GooglePhotosInterface photosInterface,
      ImageStreamProvider imageStreamProvider) {
    this.credentialFactory = credentialFactory;
    this.jobStore = jobStore;
    this.jsonFactory = jsonFactory;
    this.photosInterface = photosInterface;
    this.imageStreamProvider = imageStreamProvider;
  }

  @Override
  public ImportResult importItem(UUID jobId, TokensAndUrlAuthData authData,
      PhotosContainerResource data) throws IOException {
    if (data == null) {
      // Nothing to do
      return ImportResult.OK;
    }

    // Uploads album metadata
    if (data.getAlbums() != null && data.getAlbums().size() > 0) {
      for (PhotoAlbum album : data.getAlbums()) {
        importSingleAlbum(jobId, authData, album);
      }
    }

    // Uploads photos
    if (data.getPhotos() != null && data.getPhotos().size() > 0) {
      for (PhotoModel photo : data.getPhotos()) {
        importSinglePhoto(jobId, authData, photo);
      }
    }

    return ImportResult.OK;
  }

  @VisibleForTesting
  void importSingleAlbum(UUID jobId, TokensAndUrlAuthData authData, PhotoAlbum inputAlbum)
      throws IOException {
    // Set up album
    GoogleAlbum googleAlbum = new GoogleAlbum();
    googleAlbum.setTitle(COPY_PREFIX + inputAlbum.getName());

    GoogleAlbum responseAlbum = getOrCreatePhotosInterface(authData).createAlbum(googleAlbum);
    TempPhotosData tempPhotosData = jobStore
        .findData(jobId, TEMP_PHOTOS_KEY, TempPhotosData.class);
    if (tempPhotosData == null) {
      tempPhotosData = new TempPhotosData(jobId);
      jobStore.create(jobId, TEMP_PHOTOS_KEY, tempPhotosData);
    }
    tempPhotosData.addAlbumId(inputAlbum.getId(), responseAlbum.getId());
    jobStore.update(jobId, TEMP_PHOTOS_KEY, tempPhotosData);
  }

  @VisibleForTesting
  void importSinglePhoto(UUID jobId, TokensAndUrlAuthData authData, PhotoModel inputPhoto)
      throws IOException {
    /*
    TODO: resumable uploads https://developers.google.com/photos/library/guides/resumable-uploads
    Resumable uploads would allow the upload of larger media that don't fit in memory.  To do this,
    however, seems to require knowledge of the total file size.
    */
    // Upload photo
    InputStream inputStream;
    if (inputPhoto.isInTempStore()) {
      inputStream = jobStore.getStream(jobId, inputPhoto.getFetchableUrl());
    } else {
      inputStream = imageStreamProvider.get(inputPhoto.getFetchableUrl());
    }

    String uploadToken = getOrCreatePhotosInterface(authData).uploadPhotoContent(inputStream);

    String description;
    if (Strings.isNullOrEmpty(inputPhoto.getDescription())) {
      description = "";
    } else {
      description = COPY_PREFIX + inputPhoto.getDescription();
    }
    NewMediaItem newMediaItem = new NewMediaItem(description, uploadToken);

    TempPhotosData tempPhotosData = jobStore
        .findData(jobId, TEMP_PHOTOS_KEY, TempPhotosData.class);
    String albumId;
    if (Strings.isNullOrEmpty(inputPhoto.getAlbumId())) {
      // This is ok, since NewMediaItemUpload will ignore all null values and it's possible to
      // upload a NewMediaItem without a corresponding album id.
      albumId = null;
    } else {
      albumId = tempPhotosData.lookupNewAlbumId(inputPhoto.getAlbumId());
    }

    NewMediaItemUpload uploadItem = new NewMediaItemUpload(albumId,
        Collections.singletonList(newMediaItem));

    getOrCreatePhotosInterface(authData).createPhoto(uploadItem);
  }

  private synchronized GooglePhotosInterface getOrCreatePhotosInterface(
      TokensAndUrlAuthData authData) {
    return photosInterface == null ? makePhotosInterface(authData) : photosInterface;
  }

  private synchronized GooglePhotosInterface makePhotosInterface(TokensAndUrlAuthData authData) {
    Credential credential = credentialFactory.createCredential(authData);
    GooglePhotosInterface photosInterface = new GooglePhotosInterface(credential, jsonFactory);
    return photosInterface;
  }
}
