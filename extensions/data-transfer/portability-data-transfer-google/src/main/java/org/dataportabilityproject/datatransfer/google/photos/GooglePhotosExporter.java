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
package org.dataportabilityproject.datatransfer.google.photos;

import com.google.api.client.auth.oauth2.Credential;
import com.google.common.annotations.VisibleForTesting;
import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.photos.UserFeed;
import com.google.gdata.util.ServiceException;
import org.dataportabilityproject.datatransfer.google.common.GoogleStaticObjects;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.transfer.provider.ExportResult;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.types.ExportInformation;
import org.dataportabilityproject.spi.transfer.types.PaginationData;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.models.photos.PhotosContainerResource;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

public class GooglePhotosExporter implements Exporter<AuthData, PhotosContainerResource> {

  private volatile PicasawebService photosService;
  private final JobStore jobStore;

  public GooglePhotosExporter(JobStore jobStore) {
    this.photosService = null;
    this.jobStore = jobStore;
  }

  @VisibleForTesting
  GooglePhotosExporter(PicasawebService photosService, JobStore jobStore) {
    this.photosService = photosService;
    this.jobStore = jobStore;
  }

  @Override
  public ExportResult<PhotosContainerResource> export(AuthData authData) {
    return null;
  }

  @Override
  public ExportResult<PhotosContainerResource> export(AuthData authData, ExportInformation exportInformation) {
    return null;
  }

  private ExportResult<PhotosContainerResource> exportAlbums(
          AuthData authData, Optional<PaginationData> paginationData) {
    try {
      URL albumUrl = new URL("https://picasaweb.google.com/data/feed/api/user/default?kind=album");

      UserFeed albumFeed = service.getFeed(albumUrl, UserFeed.class);
    } catch (ServiceException e) {
      throw new IOException("Problem making request to: " + albumUrl, e);
    }
  }

  private PicasawebService getOrCreatePhotosService(AuthData authData) {
    return photosService == null ? makePhotosService(authData) : photosService;
  }

  private synchronized PicasawebService makePhotosService(AuthData authData) {
    // TODO(olsona): create credentials from authdata
    Credential credential = null;
    PicasawebService service = new PicasawebService(GoogleStaticObjects.APP_NAME);
    service.setOAuth2Credentials(credential);
    return service;
  }
}
