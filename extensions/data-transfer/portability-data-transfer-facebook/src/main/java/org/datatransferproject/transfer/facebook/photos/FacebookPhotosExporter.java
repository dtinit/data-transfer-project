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

package org.datatransferproject.transfer.facebook.photos;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.restfb.types.Album;
import com.restfb.types.Photo;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FacebookPhotosExporter
    implements Exporter<TokensAndUrlAuthData, PhotosContainerResource> {
  private AppCredentials appCredentials;
  private FacebookPhotosInterface photosInterface;

  public FacebookPhotosExporter(AppCredentials appCredentials) {
    this.appCredentials = appCredentials;
  }

  @VisibleForTesting
  FacebookPhotosExporter(AppCredentials appCredentials, FacebookPhotosInterface photosInterface) {
    this.appCredentials = appCredentials;
    this.photosInterface = photosInterface;
  }

  @Override
  public ExportResult<PhotosContainerResource> export(
      UUID jobId, TokensAndUrlAuthData authData, Optional<ExportInformation> exportInformation) {
    Preconditions.checkNotNull(authData);

    ArrayList<PhotoAlbum> exportAlbums = new ArrayList<>();

    // Get albums
    Iterable<List<Album>> connection = getOrCreatePhotosInterface(authData).getAlbums();

    // TODO(wmorland): Paginate through albums
    for (List<Album> albums : connection) {
      for (Album album : albums) {
        exportAlbums.add(new PhotoAlbum(album.getId(), album.getName(), album.getDescription()));
      }
    }

    // Get photos for each album
    ArrayList<PhotoModel> exportPhotos = new ArrayList<>();

    for (PhotoAlbum album : exportAlbums) {
      Iterable<List<Photo>> photoConnection =
          getOrCreatePhotosInterface(authData).getPhotos(album.getId());
      for (List<Photo> photos : photoConnection) {
        for (Photo photo : photos) {
          Preconditions.checkNotNull(photo.getImages().get(0).getSource());
          exportPhotos.add(
              new PhotoModel(
                  String.format("%s.jpg", photo.getId()),
                  photo.getImages().get(0).getSource(),
                  photo.getName(),
                  "image/jpg",
                  photo.getId(),
                  album.getId(),
                  false));
        }
      }
    }

    return new ExportResult<>(
        ExportResult.ResultType.END, new PhotosContainerResource(exportAlbums, exportPhotos));
  }

  private synchronized FacebookPhotosInterface getOrCreatePhotosInterface(
      TokensAndUrlAuthData authData) {
    return photosInterface == null ? makePhotosInterface(authData) : photosInterface;
  }

  private synchronized FacebookPhotosInterface makePhotosInterface(TokensAndUrlAuthData authData) {
    photosInterface = new RestFbFacebookPhotos(authData, appCredentials);
    return photosInterface;
  }
}
