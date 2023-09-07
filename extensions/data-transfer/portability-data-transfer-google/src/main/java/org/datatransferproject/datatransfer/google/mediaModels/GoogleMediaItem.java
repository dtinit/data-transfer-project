/*
 * Copyright 2019 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.google.mediaModels;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.File;
import java.nio.file.Files;
import java.util.Date;
import java.util.Optional;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoModel;

/** Media item returned by queries to the Google Photos API. Represents what is stored by Google. */
public class GoogleMediaItem {

  private final static String DEFAULT_PHOTO_MIMETYPE = "image/jpg";
  private final static String DEFAULT_VIDEO_MIMETYPE = "video/mp4";

  @JsonProperty("id")
  private String id;

  @JsonProperty("description")
  private String description;

  @JsonProperty("baseUrl")
  private String baseUrl;

  @JsonProperty("mimeType")
  private String mimeType;

  @JsonProperty("mediaMetadata")
  private MediaMetadata mediaMetadata;

  @JsonProperty("filename")
  private String filename;

  @JsonProperty("productUrl")
  private String productUrl;

  @JsonProperty("uploadedTime")
  private Date uploadedTime;

  public boolean isPhoto() {
    return this.getMediaMetadata().getPhoto() != null;
  }
  public boolean isVideo() {
    return this.getMediaMetadata().getVideo() != null;
  }


  public String getFetchableUrl() {
    if (this.isPhoto()) {
      return this.getBaseUrl() + "=d";
    } else if (this.isVideo()) {
      // dv = download video otherwise you only get a thumbnail
      return this.getBaseUrl() + "=dv";
    } else {
      throw new IllegalArgumentException("unimplemented media type");
    }
  }

  public static VideoModel convertToVideoModel(
      Optional<String> albumId, GoogleMediaItem mediaItem) {
    Preconditions.checkArgument(mediaItem.isVideo());

    return new VideoModel(
        mediaItem.getFilename(),
        mediaItem.getFetchableUrl(),
        mediaItem.getDescription(),
        getMimeType(mediaItem),
        mediaItem.getId(),
        albumId.orElse(null),
        false /*inTempStore*/,
        mediaItem.getUploadedTime());
  }

  public static PhotoModel convertToPhotoModel(
      Optional<String> albumId, GoogleMediaItem mediaItem) {
    Preconditions.checkArgument(mediaItem.isPhoto());

    return new PhotoModel(
        mediaItem.getFilename(),
        mediaItem.getFetchableUrl(),
        mediaItem.getDescription(),
        getMimeType(mediaItem),
        mediaItem.getId(),
        albumId.orElse(null),
        false  /*inTempStore*/,
        null  /*sha1*/,
        mediaItem.getUploadedTime());
  }

  private static String getMimeType(GoogleMediaItem mediaItem) {
    String guessedMimetype = guessMimeTypeFromFilename(mediaItem.getFilename());
    if (!Strings.isNullOrEmpty(guessedMimetype)) {
      return guessedMimetype;
    }

    if (!Strings.isNullOrEmpty(mediaItem.getMimeType())) {
      return mediaItem.getMimeType();
    }

    if (mediaItem.isPhoto()) {
      return DEFAULT_PHOTO_MIMETYPE;
    }
    return DEFAULT_VIDEO_MIMETYPE;
  }

  // Guesses the mimetype from the filename, or returns null on failure.
  private static String guessMimeTypeFromFilename(String filename) {
    try {
      return Files.probeContentType(new File(filename).toPath());
    } catch (Exception e) {
      return null;
    }
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  // TODO(zacsh) investigate why/if there's no setter for this; do we need setters or does the java
  // annotation do the work for us somehow?
  public String getProductUrl() {
    return productUrl;
  }

  public MediaMetadata getMediaMetadata() {
    return mediaMetadata;
  }

  public void setMediaMetadata(MediaMetadata mediaMetadata) {
    this.mediaMetadata = mediaMetadata;
  }

  public Date getUploadedTime() {
    return this.uploadedTime;
  }

  public void setUploadedTime(Date date) {
    this.uploadedTime = date;
  }
}
