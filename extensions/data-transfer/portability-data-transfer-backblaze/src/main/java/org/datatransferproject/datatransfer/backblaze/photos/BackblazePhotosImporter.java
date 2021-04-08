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

package org.datatransferproject.datatransfer.backblaze.photos;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.backblaze.common.BackblazeDataTransferClient;
import org.datatransferproject.datatransfer.backblaze.common.BackblazeDataTransferClientFactory;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.ImageStreamProvider;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;

import static java.lang.String.format;

public class BackblazePhotosImporter
    implements Importer<TokenSecretAuthData, PhotosContainerResource> {

  private static final String PHOTO_TRANSFER_MAIN_FOLDER = "Photo Transfer";

  private final TemporaryPerJobDataStore jobStore;
  private final ImageStreamProvider imageStreamProvider = new ImageStreamProvider();
  private final Monitor monitor;
  private final BackblazeDataTransferClientFactory b2ClientFactory;

  public BackblazePhotosImporter(Monitor monitor, TemporaryPerJobDataStore jobStore) {
    this.monitor = monitor;
    this.jobStore = jobStore;
    this.b2ClientFactory = new BackblazeDataTransferClientFactory();
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      TokenSecretAuthData authData,
      PhotosContainerResource data)
      throws Exception {
    if (data == null) {
      // Nothing to do
      return ImportResult.OK;
    }

    BackblazeDataTransferClient b2Client = b2ClientFactory.getOrCreateB2Client(monitor, authData);

    if (data.getAlbums() != null && data.getAlbums().size() > 0) {
      for (PhotoAlbum album : data.getAlbums()) {
        idempotentExecutor.executeAndSwallowIOExceptions(
            album.getId(),
            String.format("Caching album name for album '%s'", album.getId()),
            () -> album.getName());
      }
    }

    if (data.getPhotos() != null && data.getPhotos().size() > 0) {
      for (PhotoModel photo : data.getPhotos()) {
        idempotentExecutor.executeAndSwallowIOExceptions(
            photo.getDataId(),
            photo.getTitle(),
            () -> importSinglePhoto(idempotentExecutor, b2Client, jobId, photo));
      }
    }

    return ImportResult.OK;
  }

  private String importSinglePhoto(
      IdempotentImportExecutor idempotentExecutor,
      BackblazeDataTransferClient b2Client,
      UUID jobId,
      PhotoModel photo)
      throws IOException {
    String albumName = idempotentExecutor.getCachedValue(photo.getAlbumId());

    InputStream inputStream;
    if (photo.isInTempStore()) {
      inputStream = jobStore.getStream(jobId, photo.getFetchableUrl()).getStream();
    } else {
      HttpURLConnection conn = imageStreamProvider.getConnection(photo.getFetchableUrl());
      inputStream = conn.getInputStream();
    }

    String response = b2Client.uploadFile(
        String.format("%s/%s/%s.jpg", PHOTO_TRANSFER_MAIN_FOLDER, albumName, photo.getDataId()),
        jobStore.getTempFileFromInputStream(inputStream, photo.getDataId(), ".jpg"));

    try {
      if (photo.isInTempStore()) {
        jobStore.removeData(jobId, photo.getFetchableUrl());
      }
    } catch (Exception e) {
      // Swallow the exception caused by Remove data so that existing flows continue
      monitor.info(
              () -> format("Exception swallowed while removing data for jobId %s, localPath %s",
                      jobId, photo.getFetchableUrl()), e);
    }

    return response;
  }
}
