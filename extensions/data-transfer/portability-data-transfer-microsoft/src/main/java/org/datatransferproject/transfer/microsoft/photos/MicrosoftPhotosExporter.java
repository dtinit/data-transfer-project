/*
 * Copyright 2018 The Data-Portability Project Authors.
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
package org.datatransferproject.transfer.microsoft.photos;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.json.JsonFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.transfer.microsoft.common.MicrosoftCredentialFactory;
import org.datatransferproject.transfer.microsoft.driveModels.MicrosoftDriveItem;
import org.datatransferproject.transfer.microsoft.driveModels.MicrosoftDriveItemsResponse;
import org.datatransferproject.transfer.microsoft.driveModels.MicrosoftSpecialFolder;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.PaginationData;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.IdOnlyContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Exports Microsoft OneDrive photos using the Graph API.
 *
 * <p>Converts folders to albums.
 */
public class MicrosoftPhotosExporter
    implements Exporter<TokensAndUrlAuthData, PhotosContainerResource> {

  static final String DRIVE_TOKEN_PREFIX = "drive:";

  private final MicrosoftCredentialFactory credentialFactory;
  private final JsonFactory jsonFactory;
  private final Monitor monitor;
  private volatile MicrosoftPhotosInterface photosInterface;

  public MicrosoftPhotosExporter(
      MicrosoftCredentialFactory credentialFactory, JsonFactory jsonFactory, Monitor monitor) {
    this.credentialFactory = credentialFactory;
    this.jsonFactory = jsonFactory;
    this.monitor = monitor;
  }

  @VisibleForTesting
  public MicrosoftPhotosExporter(
      MicrosoftCredentialFactory credentialFactory,
      JsonFactory jsonFactory,
      MicrosoftPhotosInterface photosInterface,
      Monitor monitor) {
    this.credentialFactory = credentialFactory;
    this.jsonFactory = jsonFactory;
    this.photosInterface = photosInterface;
    this.monitor = monitor;
  }

  @Override
  public ExportResult<PhotosContainerResource> export(
      UUID jobId, TokensAndUrlAuthData authData, Optional<ExportInformation> exportInformation)
      throws IOException {
    if (!exportInformation.isPresent()) {
      return exportOneDrivePhotos(authData, Optional.empty(), Optional.empty(), jobId);
    }

    IdOnlyContainerResource idOnlyContainerResource =
        (IdOnlyContainerResource) exportInformation.get().getContainerResource();
    StringPaginationToken paginationToken =
        (StringPaginationToken) exportInformation.get().getPaginationData();

    return exportOneDrivePhotos(
        authData,
        Optional.ofNullable(idOnlyContainerResource),
        Optional.ofNullable(paginationToken),
        jobId);
  }

  @VisibleForTesting
  ExportResult<PhotosContainerResource> exportOneDrivePhotos(
      TokensAndUrlAuthData authData,
      Optional<IdOnlyContainerResource> albumData,
      Optional<PaginationData> paginationData,
      UUID jobId)
      throws IOException {
    Optional<String> albumId = Optional.empty();
    if (albumData.isPresent()) {
      albumId = Optional.of(albumData.get().getId());
    }
    Optional<String> paginationUrl = getDrivePaginationToken(paginationData);

    MicrosoftDriveItemsResponse driveItemsResponse;
    if (paginationData.isPresent() || albumData.isPresent()) {
      driveItemsResponse =
          getOrCreatePhotosInterface(authData).getDriveItems(albumId, paginationUrl);
    } else {
      driveItemsResponse =
          getOrCreatePhotosInterface(authData)
              .getDriveItemsFromSpecialFolder(MicrosoftSpecialFolder.FolderType.photos);
    }

    PaginationData nextPageData = SetNextPageToken(driveItemsResponse);
    ContinuationData continuationData = new ContinuationData(nextPageData);
    PhotosContainerResource containerResource;
    MicrosoftDriveItem[] driveItems = driveItemsResponse.getDriveItems();
    List<PhotoAlbum> albums = new ArrayList<>();
    List<PhotoModel> photos = new ArrayList<>();

    if (driveItems != null && driveItems.length > 0) {
      for (MicrosoftDriveItem driveItem : driveItems) {
        PhotoAlbum album = tryConvertDriveItemToPhotoAlbum(driveItem, jobId);
        if (album != null) {
          albums.add(album);
          continuationData.addContainerResource(new IdOnlyContainerResource(driveItem.id));
        }

        PhotoModel photo = tryConvertDriveItemToPhotoModel(albumId, driveItem, jobId);
        if (photo != null) {
          photos.add(photo);
        }
      }
    }

    ExportResult.ResultType result =
        nextPageData == null ? ExportResult.ResultType.END : ExportResult.ResultType.CONTINUE;
    containerResource = new PhotosContainerResource(albums, photos);
    return new ExportResult<>(result, containerResource, continuationData);
  }

  private PhotoAlbum tryConvertDriveItemToPhotoAlbum(MicrosoftDriveItem driveItem, UUID jobId) {

    if (driveItem.folder != null) {
      PhotoAlbum photoAlbum = new PhotoAlbum(driveItem.id, driveItem.name, driveItem.description);
      monitor.debug(
          () -> String.format("%s: Microsoft OneDrive exporting album: %s", jobId, photoAlbum));
      return photoAlbum;
    }

    return null;
  }

  private PhotoModel tryConvertDriveItemToPhotoModel(
      Optional<String> albumId, MicrosoftDriveItem driveItem, UUID jobId) {

    if (driveItem.file != null
        && driveItem.file.mimeType != null
        && driveItem.file.mimeType.startsWith("image/")) {
      PhotoModel photo =
          new PhotoModel(
              driveItem.name,
              driveItem.downloadUrl,
              driveItem.description,
              driveItem.file.mimeType,
              driveItem.id,
              albumId.orElse(null),
              false);
      monitor.debug(
          () -> String.format("%s: Microsoft OneDrive exporting photo: %s", jobId, photo));
      return photo;
    }

    return null;
  }

  private PaginationData SetNextPageToken(MicrosoftDriveItemsResponse driveItemsResponse) {
    String url = driveItemsResponse.getNextPageLink();

    if (!Strings.isNullOrEmpty(url)) {
      return new StringPaginationToken(DRIVE_TOKEN_PREFIX + url);
    }

    return null;
  }

  private Optional<String> getDrivePaginationToken(Optional<PaginationData> paginationData) {
    return getPaginationToken(paginationData, DRIVE_TOKEN_PREFIX);
  }

  private Optional<String> getPaginationToken(
      Optional<PaginationData> paginationData, String tokenPrefix) {
    Optional<String> paginationToken = Optional.empty();
    if (paginationData.isPresent()) {
      String token = ((StringPaginationToken) paginationData.get()).getToken();
      Preconditions.checkArgument(
          token.startsWith(tokenPrefix), "Invalid pagination token " + token);
      if (token.length() > tokenPrefix.length()) {
        paginationToken = Optional.of(token.substring(tokenPrefix.length()));
      }
    }
    return paginationToken;
  }

  private synchronized MicrosoftPhotosInterface getOrCreatePhotosInterface(
      TokensAndUrlAuthData authData) {
    return photosInterface == null ? makePhotosInterface(authData) : photosInterface;
  }

  private synchronized MicrosoftPhotosInterface makePhotosInterface(TokensAndUrlAuthData authData) {
    Credential credential = credentialFactory.createCredential(authData);
    return new MicrosoftPhotosInterface(credential, jsonFactory);
  }
}
