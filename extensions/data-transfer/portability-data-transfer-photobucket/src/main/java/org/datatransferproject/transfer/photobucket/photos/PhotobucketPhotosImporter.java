/*
 * Copyright 2021 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.photobucket.photos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import okhttp3.OkHttpClient;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.photobucket.client.PhotobucketClient;
import org.datatransferproject.transfer.photobucket.client.PhotobucketCredentialsFactory;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.*;

import java.util.UUID;

import static org.datatransferproject.transfer.photobucket.data.PhotobucketConstants.*;

public class PhotobucketPhotosImporter implements Importer<AuthData, PhotosContainerResource> {

  private final Monitor monitor;
  private final OkHttpClient httpClient;
  private final TemporaryPerJobDataStore jobStore;
  private final PhotobucketCredentialsFactory credentialsFactory;
  private final ObjectMapper objectMapper;

  @VisibleForTesting
  public PhotobucketPhotosImporter(
      PhotobucketCredentialsFactory credentialsFactory,
      Monitor monitor,
      OkHttpClient httpClient,
      TemporaryPerJobDataStore jobStore,
      ObjectMapper objectMapper) {
    monitor.debug(() -> "Starting PhotobucketPhotosImporter initialization");
    this.monitor = monitor;
    this.httpClient = httpClient;
    this.jobStore = jobStore;
    this.credentialsFactory = credentialsFactory;
    this.objectMapper = objectMapper;
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      AuthData authData,
      PhotosContainerResource data)
      throws Exception {

    Preconditions.checkArgument(
        data.getAlbums() != null || data.getPhotos() != null,
        String.format("Error: There is no data to import for jobId=[%s]", jobId));
    if (!(authData instanceof TokensAndUrlAuthData)) {
      throw new IllegalArgumentException("Wrong token instance");
    }

    Credential credential = credentialsFactory.createCredential((TokensAndUrlAuthData) authData);
    PhotobucketClient photobucketClient =
        new PhotobucketClient(jobId, credential, httpClient, jobStore, objectMapper);

    // create empty album in root where all data structure is going to be saved
    monitor.debug(() -> String.format("Creating top level album for jobId=[%s]", jobId));

    photobucketClient.createTopLevelAlbum(MAIN_ALBUM_TITLE);

    // import albums
    monitor.debug(() -> String.format("Starting albums import for jobId=[%s]", jobId));
    for (PhotoAlbum album : data.getAlbums()) {
      photobucketClient.createAlbum(album, ALBUM_TITLE_PREFIX);
    }

    // import photos
    monitor.debug(() -> String.format("Starting images import  for jobId=[%s]", jobId));
    for (PhotoModel photo : data.getPhotos()) {
      photobucketClient.uploadPhoto(photo);
    }
    monitor.debug(() -> String.format("Import complete,  for jobId=[%s]", jobId));

    return new ImportResult(ImportResult.ResultType.OK);
  }
}
