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
import com.google.gdata.data.MediaContent;
import com.google.gdata.data.photos.AlbumFeed;
import com.google.gdata.data.photos.GphotoEntry;
import com.google.gdata.data.photos.UserFeed;
import com.google.gdata.util.ServiceException;
import org.dataportabilityproject.datatransfer.google.common.GoogleStaticObjects;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.transfer.provider.ExportResult;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.provider.ExportResult.ResultType;
import org.dataportabilityproject.spi.transfer.types.ContinuationData;
import org.dataportabilityproject.spi.transfer.types.ExportInformation;
import org.dataportabilityproject.spi.transfer.types.IdOnlyContainerResource;
import org.dataportabilityproject.spi.transfer.types.PaginationData;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.models.ContainerResource;
import org.dataportabilityproject.types.transfer.models.photos.PhotoAlbum;
import org.dataportabilityproject.types.transfer.models.photos.PhotoModel;
import org.dataportabilityproject.types.transfer.models.photos.PhotosContainerResource;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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

  private ExportResult<PhotosContainerResource> exportAlbums(AuthData authData) {
    // TODO(olsona): is pagination data relevant for this?

    try {
      URL albumUrl = new URL("https://picasaweb.google.com/data/feed/api/user/default?kind=album");
      UserFeed albumFeed = getOrCreatePhotosService(authData).getFeed(albumUrl, UserFeed.class);

      List<PhotoAlbum> albums = new ArrayList<>(albumFeed.getAlbumEntries().size());
      ContinuationData continuationData = new ContinuationData(null);

      for (GphotoEntry googleAlbum : albumFeed.getAlbumEntries()) {
        // Add album info to list so album can be recreated later
        albums.add(new PhotoAlbum(googleAlbum.getGphotoId(), googleAlbum.getTitle().getPlainText(),
                googleAlbum.getDescription().getPlainText()));

        // Add album id to continuation data
        continuationData.addContainerResource(new IdOnlyContainerResource(googleAlbum.getGphotoId()));
      }

      ResultType resultType = ResultType.CONTINUE;
      PhotosContainerResource containerResource = new PhotosContainerResource(albums, null);
      return new ExportResult<PhotosContainerResource>
              (resultType, containerResource, continuationData);
    } catch (ServiceException | IOException e) {
      return new ExportResult<PhotosContainerResource>(ResultType.ERROR, e.getMessage());
    }
  }

  private ExportResult<PhotosContainerResource> exportPhotos(String albumId, AuthData authData) {
    // TODO(olsona): pagination data?

    try {
      // imgmax=d gets the original image as per:
      // https://developers.google.com/picasa-web/docs/3.0/reference
      URL photosUrl = new URL(
              "https://picasaweb.google.com/data/feed/api/user/default/albumid/"
                      + albumId
                      + "?imgmax=d");
      AlbumFeed photoFeed = getOrCreatePhotosService(authData).getFeed(photosUrl, AlbumFeed.class);

      List<PhotoModel> photos = new ArrayList<>(photoFeed.getEntries().size());
      for (GphotoEntry photo : photoFeed.getEntries()) {
        MediaContent mediaContent = (MediaContent) photo.getContent();
        photos.add(new PhotoModel(photo.getTitle().getPlainText(), mediaContent.getUri(), photo
                .getDescription().getPlainText(), mediaContent.getMimeType().getMediaType(),
                albumId));
      }

      PhotosContainerResource containerResource = new PhotosContainerResource(null, photos);
      return new ExportResult<PhotosContainerResource>(ResultType.END, containerResource, null);
    } catch (ServiceException | IOException e) {
      return new ExportResult<PhotosContainerResource>(ResultType.ERROR, e.getMessage());
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
