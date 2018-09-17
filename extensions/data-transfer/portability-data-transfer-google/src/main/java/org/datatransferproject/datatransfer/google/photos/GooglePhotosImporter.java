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
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.UUID;
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

public class GooglePhotosImporter
    implements Importer<TokensAndUrlAuthData, PhotosContainerResource> {

  static final Logger logger = LoggerFactory.getLogger(GooglePhotosImporter.class);

  private final GoogleCredentialFactory credentialFactory;
  private final JobStore jobStore;
  private final JsonFactory jsonFactory;
  private final ImageStreamProvider imageStreamProvider;
  private volatile GooglePhotosInterface photosInterface;

  public GooglePhotosImporter(GoogleCredentialFactory credentialFactory, JobStore jobStore, JsonFactory jsonFactory) {
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
    if (data.getAlbums() != null && data.getAlbums().size() > 0) {
      for (PhotoAlbum album : data.getAlbums()) {
        importSingleAlbum(jobId, authData, album);
      }
    }

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
    googleAlbum.setTitle("Copy of " + inputAlbum.getName());

    GoogleAlbum responseAlbum = getOrCreatePhotosInterface(authData).createAlbum(googleAlbum);
    TempPhotosData tempPhotosData = jobStore
        .findData(jobId, createCacheKey(), TempPhotosData.class);
    if (tempPhotosData == null) {
      tempPhotosData = new TempPhotosData(jobId);
      jobStore.create(jobId, createCacheKey(), tempPhotosData);
    }
    tempPhotosData.addAlbumId(inputAlbum.getId(), responseAlbum.getId());
    jobStore.update(jobId, createCacheKey(), tempPhotosData);
  }

  @VisibleForTesting
  void importSinglePhoto(UUID jobId, TokensAndUrlAuthData authData, PhotoModel inputPhoto)
      throws IOException {
    // Upload photo
    // TODO: resumable uploads https://developers.google.com/photos/library/guides/resumable-uploads
    InputStream inputStream;
    if (inputPhoto.isInTempStore()) {
      inputStream = jobStore.getStream(jobId, inputPhoto.getFetchableUrl());
    } else {
      inputStream = imageStreamProvider.get(inputPhoto.getFetchableUrl());
    }

    String uploadToken = getOrCreatePhotosInterface(authData).uploadPhotoContent(inputStream);

    // TODO: what to do about null photo descriptions?
    NewMediaItem newMediaItem = new NewMediaItem("Copy of " + inputPhoto.getDescription(),
        uploadToken);

    TempPhotosData tempPhotosData = jobStore
        .findData(jobId, createCacheKey(), TempPhotosData.class);
    String albumId = tempPhotosData.lookupNewAlbumId(inputPhoto.getAlbumId());

    NewMediaItemUpload uploadItem = new NewMediaItemUpload(albumId,
        Collections.singletonList(newMediaItem), null);

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

  /**
   * Key for cache of album mappings. TODO: Add a method parameter for a {@code key} for fine
   * grained objects.
   */
  private String createCacheKey() {
    // TODO: store objects containing individual mappings instead of single object containing all mappings
    return "tempPhotosData";
  }
}
