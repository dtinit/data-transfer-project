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
package org.datatransferproject.transfer.microsoft.media;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.json.JsonFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

/**
 * Exports Microsoft OneDrive photos using the Graph API.
 *
 * <p>Converts folders to media albums.
 */
public class MicrosoftMediaExporter
    implements Exporter<TokensAndUrlAuthData, MediaContainerResource> {
  static final String DRIVE_TOKEN_PREFIX = "drive:";

  private final MicrosoftCredentialFactory credentialFactory;
  private final JsonFactory jsonFactory;
  private final Monitor monitor;
  private volatile MicrosoftMediaInterface photosInterface;

  public MicrosoftMediaExporter(
      MicrosoftCredentialFactory credentialFactory, JsonFactory jsonFactory, Monitor monitor) {
    this.credentialFactory = credentialFactory;
    this.jsonFactory = jsonFactory;
    this.monitor = monitor;
  }

  @VisibleForTesting
  public MicrosoftMediaExporter(MicrosoftCredentialFactory credentialFactory,
      JsonFactory jsonFactory, MicrosoftMediaInterface photosInterface, Monitor monitor) {
    this.credentialFactory = credentialFactory;
    this.jsonFactory = jsonFactory;
    this.photosInterface = photosInterface;
    this.monitor = monitor;
  }

  @Override
  public ExportResult<MediaContainerResource> export(UUID jobId, TokensAndUrlAuthData authData,
      Optional<ExportInformation> exportInformation) throws IOException {
    if (!exportInformation.isPresent()) {
      return exportOneDrivePhotos(authData, Optional.empty(), Optional.empty(), jobId);
    }

    IdOnlyContainerResource idOnlyContainerResource =
        (IdOnlyContainerResource) exportInformation.get().getContainerResource();
    StringPaginationToken paginationToken =
        (StringPaginationToken) exportInformation.get().getPaginationData();

    return exportOneDrivePhotos(authData, Optional.ofNullable(idOnlyContainerResource),
        Optional.ofNullable(paginationToken), jobId);
  }

  // TODO make this private and tests the real export().
  @VisibleForTesting
  ExportResult<MediaContainerResource> exportOneDrivePhotos(TokensAndUrlAuthData authData,
      Optional<IdOnlyContainerResource> albumData, Optional<PaginationData> paginationData,
      UUID jobId) throws IOException {
    Optional<String> albumId = Optional.empty();
    if (albumData.isPresent()) {
      albumId = Optional.of(albumData.get().getId());
    }
    Optional<String> paginationUrl = getDrivePaginationToken(paginationData);

    MicrosoftDriveItemsResponse driveItemsResponse;
    if (paginationData.isPresent() || albumData.isPresent()) {
      driveItemsResponse =
          getOrCreateMediaInterface(authData).getDriveItems(albumId, paginationUrl);
    } else {
      driveItemsResponse = getOrCreateMediaInterface(authData).getDriveItemsFromSpecialFolder(
          MicrosoftSpecialFolder.FolderType.photos);
    }

    PaginationData nextPageData = setNextPageToken(driveItemsResponse);
    ContinuationData continuationData = new ContinuationData(nextPageData);
    MediaContainerResource containerResource;
    MicrosoftDriveItem[] driveItems = driveItemsResponse.getDriveItems();
    List<MediaAlbum> albums = new ArrayList<>();
    List<PhotoModel> photos = new ArrayList<>();
    List<VideoModel> videos = new ArrayList<>();

    if (driveItems != null && driveItems.length > 0) {
      for (MicrosoftDriveItem driveItem : driveItems) {
        MediaAlbum album = tryConvertDriveItemToMediaAlbum(driveItem, jobId);
        if (album != null) {
          albums.add(album);
          continuationData.addContainerResource(new IdOnlyContainerResource(driveItem.id));
          continue;
        }

        PhotoModel photo = tryConvertDriveItemToPhotoModel(albumId, driveItem, jobId);
        if (photo != null) {
          photos.add(photo);
          continue;
        }

        VideoModel video = tryConvertDriveItemToVideoModel(albumId, driveItem, jobId);
        if (video != null) {
          videos.add(video);
          continue;
        }
      }
    }

    ExportResult.ResultType result =
        nextPageData == null ? ExportResult.ResultType.END : ExportResult.ResultType.CONTINUE;
    containerResource = new MediaContainerResource(albums, photos, videos);
    return new ExportResult<>(result, containerResource, continuationData);
  }

  private MediaAlbum tryConvertDriveItemToMediaAlbum(MicrosoftDriveItem driveItem, UUID jobId) {
    if (!driveItem.isFolder()) {
      return null;
    }

    MediaAlbum mediaAlbum = new MediaAlbum(driveItem.id, driveItem.name, driveItem.description);
    monitor.debug(
        () -> String.format("%s: Microsoft OneDrive exporting album: %s", jobId, mediaAlbum));
    return mediaAlbum;
  }

  private PhotoModel tryConvertDriveItemToPhotoModel(
      Optional<String> albumId, MicrosoftDriveItem driveItem, UUID jobId) {
    if (!driveItem.isImage()) {
      return null;
    }

    PhotoModel photo =
        new PhotoModel(driveItem.name, driveItem.downloadUrl, driveItem.description,
            driveItem.file.mimeType, driveItem.id, albumId.orElse(null), false /*inTempStore*/);
    monitor.debug(
        () -> String.format("%s: Microsoft OneDrive exporting photo: %s", jobId, photo));
    return photo;
  }

  private VideoModel tryConvertDriveItemToVideoModel(
      Optional<String> albumId, MicrosoftDriveItem driveItem, UUID jobId) {
    if (!driveItem.isVideo()) {
      return null;
    }

    VideoModel video =
        new VideoModel(driveItem.name, driveItem.downloadUrl, driveItem.description,
            driveItem.file.mimeType, driveItem.id, albumId.orElse(null), false /*inTempStore*/, null);
    monitor.debug(
        () -> String.format("%s: Microsoft OneDrive exporting video: %s", jobId, video));
    return video;
  }

  private PaginationData setNextPageToken(MicrosoftDriveItemsResponse driveItemsResponse) {
    String url = driveItemsResponse.getNextPageLink();
    if (Strings.isNullOrEmpty(url)) {
      return null;
    }
    return new StringPaginationToken(DRIVE_TOKEN_PREFIX + url);
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

  private synchronized MicrosoftMediaInterface getOrCreateMediaInterface(
      TokensAndUrlAuthData authData) {
    return photosInterface == null ? makeMediaInterface(authData) : photosInterface;
  }

  private synchronized MicrosoftMediaInterface makeMediaInterface(TokensAndUrlAuthData authData) {
    Credential credential = credentialFactory.createCredential(authData);
    return new MicrosoftMediaInterface(credential, jsonFactory);
  }
}
