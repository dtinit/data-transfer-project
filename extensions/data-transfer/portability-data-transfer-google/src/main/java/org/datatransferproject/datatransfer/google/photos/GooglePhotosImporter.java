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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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

  static final String ALBUM_POST_URL = "https://picasaweb.google.com/data/feed/api/user/default";
  static final String PHOTO_POST_URL_FORMATTER =
      "https://picasaweb.google.com/data/feed/api/user/default/albumid/%s";
  // The default album to upload to if the photo is not associated with an album
  static final String DEFAULT_ALBUM_ID = "default";
  static final Logger logger = LoggerFactory.getLogger(GooglePhotosImporter.class);

  private final GoogleCredentialFactory credentialFactory;
  private final JobStore jobStore;
  private final ImageStreamProvider imageStreamProvider;
  private volatile GooglePhotosInterface photosInterface;

  public GooglePhotosImporter(GoogleCredentialFactory credentialFactory, JobStore jobStore) {
    this(credentialFactory, jobStore, null, new ImageStreamProvider());
  }

  @VisibleForTesting
  GooglePhotosImporter(
      GoogleCredentialFactory credentialFactory,
      JobStore jobStore,
      GooglePhotosInterface photosInterface,
      ImageStreamProvider imageStreamProvider) {
    this.credentialFactory = credentialFactory;
    this.jobStore = jobStore;
    this.photosInterface = photosInterface;
    this.imageStreamProvider = imageStreamProvider;
  }

  @Override
  public ImportResult importItem(UUID jobId, TokensAndUrlAuthData authData,
      PhotosContainerResource data) throws IOException {
    if (data.getAlbums() != null && data.getAlbums().size() > 0) {
      for (PhotoAlbum album : data.getAlbums()) {
        importSingleAlbum(jobId, album, authData);
      }
    }

    if (data.getPhotos() != null && data.getPhotos().size() > 0) {
      for (PhotoModel photo : data.getPhotos()) {
        importSinglePhoto(authData, photo, jobId);
      }
    }

    return ImportResult.OK;
  }

  @VisibleForTesting
  void importSingleAlbum(UUID jobId, PhotoAlbum inputAlbum, TokensAndUrlAuthData authData)
      throws IOException {
    // Set up album
    Map<String, String> albumInfo = new HashMap<>();
    albumInfo.put("title", "Copy of " + inputAlbum.getName());

    GoogleAlbum responseAlbum = getOrCreatePhotosInterface(authData).createAlbum(albumInfo);
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
  void importSinglePhoto(TokensAndUrlAuthData authData, PhotoModel inputPhoto, UUID jobId)
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

    TempPhotosData tempPhotosData = jobStore
        .findData(jobId, createCacheKey(), TempPhotosData.class);
    String albumId = tempPhotosData.lookupNewAlbumId(inputPhoto.getAlbumId());

    // TODO: what to do about null photo descriptions?
    NewMediaItem newMediaItem = new NewMediaItem("Copy of " + inputPhoto.getDescription(),
        uploadToken);
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
    GooglePhotosInterface photosInterface = new GooglePhotosInterface(credential);
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
