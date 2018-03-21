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
import java.io.IOException;
import org.dataportabilityproject.datatransfer.google.common.GoogleStaticObjects;
import org.dataportabilityproject.spi.transfer.provider.ImportResult;
import org.dataportabilityproject.spi.transfer.provider.ImportResult.ResultType;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.models.photos.PhotoAlbum;
import org.dataportabilityproject.types.transfer.models.photos.PhotoModel;
import org.dataportabilityproject.types.transfer.models.photos.PhotosContainerResource;

public class GooglePhotosImporter implements Importer<AuthData, PhotosContainerResource> {

  private volatile PicasawebService photosService;

  @Override
  public ImportResult importItem(String jobId, AuthData authData, PhotosContainerResource data) {
    try {
      for (PhotoAlbum album : data.getAlbums()) {
        importSingleAlbum(jobId, authData, album);
      }
      for (PhotoModel photo : data.getPhotos()) {
        importSinglePhoto(jobId, authData, photo);
      }
    } catch (IOException e) {
      return new ImportResult(ResultType.ERROR, e.getMessage());
    }
    return ImportResult.OK;
  }

  @VisibleForTesting
  void importSingleAlbum(String jobId, AuthData authData, PhotoAlbum photoAlbum) throws IOException {

  }

  @VisibleForTesting
  void importSinglePhoto(String jobId, AuthData authData, PhotoModel photoModel) throws IOException {

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
