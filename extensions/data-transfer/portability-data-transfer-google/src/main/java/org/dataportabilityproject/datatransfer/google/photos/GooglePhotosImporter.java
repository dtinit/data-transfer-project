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

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.common.annotations.VisibleForTesting;
import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.media.MediaStreamSource;
import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.util.ServiceException;
import org.dataportabilityproject.datatransfer.google.common.GoogleStaticObjects;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.transfer.provider.ImportResult;
import org.dataportabilityproject.spi.transfer.provider.ImportResult.ResultType;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.spi.transfer.types.TempPhotosData;
import org.dataportabilityproject.types.transfer.auth.TokensAndUrlAuthData;
import org.dataportabilityproject.types.transfer.models.photos.PhotoAlbum;
import org.dataportabilityproject.types.transfer.models.photos.PhotoModel;
import org.dataportabilityproject.types.transfer.models.photos.PhotosContainerResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class GooglePhotosImporter implements Importer<TokensAndUrlAuthData, PhotosContainerResource> {

  static final String ALBUM_POST_URL = "https://picasaweb.google.com/data/feed/api/user/default";
  static final String PHOTO_POST_URL_FORMATTER = "https://picasaweb.google.com/data/feed/api/user/default/albumid/%s";

  private final JobStore jobStore;
  private volatile PicasawebService photosService;

  public GooglePhotosImporter(JobStore jobStore) {
    this.photosService = null;
    this.jobStore = jobStore;
  }

  @VisibleForTesting
  GooglePhotosImporter(PicasawebService photosService, JobStore jobStore) {
    this.photosService = photosService;
    this.jobStore = jobStore;
  }

  // We should pull this out into a common library.
  private static InputStream getImageAsStream(String urlStr) throws IOException {
    URL url = new URL(urlStr);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.connect();
    return conn.getInputStream();
  }

  @Override
  public ImportResult importItem(UUID jobId, TokensAndUrlAuthData authData, PhotosContainerResource data) {
    try {
      for (PhotoAlbum album : data.getAlbums()) {
        importSingleAlbum(jobId, authData, album);
      }
      for (PhotoModel photo : data.getPhotos()) {
        importSinglePhoto(jobId, authData, photo);
      }
    } catch (IOException | ServiceException e) {
      // TODO(olsona): we shouldn't just error out if there's a single problem - should retry
      return new ImportResult(ResultType.ERROR, e.getMessage());
    }
    return ImportResult.OK;
  }

  @VisibleForTesting
  void importSingleAlbum(UUID jobId, TokensAndUrlAuthData authData, PhotoAlbum inputAlbum)
      throws IOException, ServiceException {

    // Set up album
    AlbumEntry outputAlbum = new AlbumEntry();
    outputAlbum.setTitle(new PlainTextConstruct("copy of " + inputAlbum.getName()));
    outputAlbum.setDescription(new PlainTextConstruct(inputAlbum.getDescription()));

    // Upload album
    AlbumEntry insertedEntry = getOrCreatePhotosService(authData)
        .insert(new URL(ALBUM_POST_URL), outputAlbum);

    // Put new album ID in job store so photos can be assigned to the correct album
    TempPhotosData photoMappings = jobStore.findData(TempPhotosData.class, jobId);
    if (photoMappings == null) {
      photoMappings = new TempPhotosData(jobId);
      jobStore.create(jobId, photoMappings);
    }
    photoMappings.addAlbumId(inputAlbum.getId(), insertedEntry.getGphotoId());
    jobStore.update(jobId, photoMappings);
  }

  @VisibleForTesting
  void importSinglePhoto(UUID jobId, TokensAndUrlAuthData authData, PhotoModel inputPhoto)
      throws IOException, ServiceException {

    // Set up photo
    PhotoEntry outputPhoto = new PhotoEntry();
    outputPhoto.setTitle(new PlainTextConstruct("copy of " + inputPhoto.getTitle()));
    outputPhoto.setDescription(new PlainTextConstruct(inputPhoto.getDescription()));
    outputPhoto.setClient(GoogleStaticObjects.APP_NAME);

    String mediaType = inputPhoto.getMediaType();
    if (mediaType == null) {
      mediaType = "image/jpeg";
    }

    MediaStreamSource streamSource = new MediaStreamSource(
        getImageAsStream(inputPhoto.getFetchableUrl()), mediaType);
    outputPhoto.setMediaSource(streamSource);

    // Find album to upload photo to
    String albumId = jobStore.findData(TempPhotosData.class, jobId)
        .lookupNewAlbumId(inputPhoto.getAlbumId());
    URL uploadUrl = new URL(String.format(PHOTO_POST_URL_FORMATTER, albumId));

    // Upload photo
    getOrCreatePhotosService(authData).insert(uploadUrl, outputPhoto);
  }

  private PicasawebService getOrCreatePhotosService(TokensAndUrlAuthData authData) {
    return photosService == null ? makePhotosService(authData) : photosService;
  }

  private synchronized PicasawebService makePhotosService(TokensAndUrlAuthData authData) {
    Credential credential =
        new Credential(BearerToken.authorizationHeaderAccessMethod())
            .setAccessToken(authData.getAccessToken());
    PicasawebService service = new PicasawebService(GoogleStaticObjects.APP_NAME);
    service.setOAuth2Credentials(credential);
    return service;
  }
}
