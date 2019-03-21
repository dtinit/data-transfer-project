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
import org.datatransferproject.datatransfer.google.mediaModels.GoogleAlbum;
import org.datatransferproject.datatransfer.google.mediaModels.NewMediaItem;
import org.datatransferproject.datatransfer.google.mediaModels.NewMediaItemUpload;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.provider.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.ImageStreamProvider;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.UUID;

public class GooglePhotosImporter
    implements Importer<TokensAndUrlAuthData, PhotosContainerResource> {

  // TODO: internationalize copy prefix
  private static final String COPY_PREFIX = "Copy of ";

  private final GoogleCredentialFactory credentialFactory;
  private final JobStore jobStore;
  private final JsonFactory jsonFactory;
  private final ImageStreamProvider imageStreamProvider;
  private volatile GooglePhotosInterface photosInterface;

  public GooglePhotosImporter(
      GoogleCredentialFactory credentialFactory, JobStore jobStore, JsonFactory jsonFactory) {
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
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentImportExecutor,
      TokensAndUrlAuthData authData,
      PhotosContainerResource data) throws IOException {
    if (data == null) {
      // Nothing to do
      return ImportResult.OK;
    }

    // Uploads album metadata
    if (data.getAlbums() != null && data.getAlbums().size() > 0) {
      for (PhotoAlbum album : data.getAlbums()) {
        idempotentImportExecutor.execute(
            album.getId(),
            album.getName(),
            () -> importSingleAlbum(authData, album)
        );
      }
    }

    // Uploads photos
    if (data.getPhotos() != null && data.getPhotos().size() > 0) {
      for (PhotoModel photo : data.getPhotos()) {
        idempotentImportExecutor.execute(
            photo.getDataId(),
            photo.getTitle(),
            () -> importSinglePhoto(jobId, authData, photo, idempotentImportExecutor));
      }
    }

    return ImportResult.OK;
  }

  @VisibleForTesting
  String importSingleAlbum(TokensAndUrlAuthData authData, PhotoAlbum inputAlbum)
      throws IOException {
    // Set up album
    GoogleAlbum googleAlbum = new GoogleAlbum();
    googleAlbum.setTitle(COPY_PREFIX + inputAlbum.getName());

    GoogleAlbum responseAlbum = getOrCreatePhotosInterface(authData).createAlbum(googleAlbum);
    return responseAlbum.getId();
  }

  @VisibleForTesting
  String importSinglePhoto(
      UUID jobId,
      TokensAndUrlAuthData authData,
      PhotoModel inputPhoto,
      IdempotentImportExecutor idempotentImportExecutor)
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

    String albumId;
    if (Strings.isNullOrEmpty(inputPhoto.getAlbumId())) {
      // This is ok, since NewMediaItemUpload will ignore all null values and it's possible to
      // upload a NewMediaItem without a corresponding album id.
      albumId = null;
    } else {
      albumId = idempotentImportExecutor.getCachedValue(inputPhoto.getAlbumId());
    }

    NewMediaItemUpload uploadItem =
        new NewMediaItemUpload(albumId, Collections.singletonList(newMediaItem));

    return getOrCreatePhotosInterface(authData).createPhoto(uploadItem)
        .getResults()[0].getMediaItem().getId();
  }

  private synchronized GooglePhotosInterface getOrCreatePhotosInterface(
      TokensAndUrlAuthData authData) {
    return photosInterface == null ? makePhotosInterface(authData) : photosInterface;
  }

  private synchronized GooglePhotosInterface makePhotosInterface(TokensAndUrlAuthData authData) {
    Credential credential = credentialFactory.createCredential(authData);
    return new GooglePhotosInterface(credential, jsonFactory);
  }
}
