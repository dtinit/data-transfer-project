/*
 * Copyright 2026 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.amazon.photos;

import org.datatransferproject.types.common.DownloadableItem;
import org.datatransferproject.types.common.models.FavoriteInfo;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoModel;

import java.util.Date;
import java.util.Optional;

/**
 * Immutable parameter object describing a single item to upload via {@link
 * AmazonImportHelper#uploadItem}: the source item plus its display name, data id, album id,
 * uploaded time and favorite flag.
 *
 * <p>Instances are created only through {@link #forPhoto} and {@link #forVideo}, which keeps the
 * photo/video field mapping in one place and spares call sites from ordering several
 * similarly-typed arguments by hand.
 */
final class UploadItemRequest {

  final DownloadableItem item;
  final String displayName;
  final String dataId;
  final String albumId;
  final Date uploadedTime;
  final boolean isFavorite;

  private UploadItemRequest(DownloadableItem item, String displayName, String dataId,
                            String albumId, Date uploadedTime, boolean isFavorite) {
    this.item = item;
    this.displayName = displayName;
    this.dataId = dataId;
    this.albumId = albumId;
    this.uploadedTime = uploadedTime;
    this.isFavorite = isFavorite;
  }

  static UploadItemRequest forPhoto(PhotoModel photo) {
    return new UploadItemRequest(photo, photo.getTitle(), photo.getDataId(), photo.getAlbumId(),
        photo.getUploadedTime(), favorited(photo.getFavoriteInfo()));
  }

  static UploadItemRequest forVideo(VideoModel video) {
    return new UploadItemRequest(video, video.getName(), video.getDataId(), video.getAlbumId(),
        video.getUploadedTime(), favorited(video.getFavoriteInfo()));
  }

  private static boolean favorited(FavoriteInfo favoriteInfo) {
    return Optional.ofNullable(favoriteInfo).map(FavoriteInfo::getFavorited).orElse(false);
  }
}
