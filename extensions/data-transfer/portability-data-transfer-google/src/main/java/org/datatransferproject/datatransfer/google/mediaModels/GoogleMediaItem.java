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
import java.io.Serializable;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.apache.tika.Tika;
import com.google.common.base.Strings;


/** Media item returned by queries to the Google Photos API. Represents what is stored by Google. */
public class GoogleMediaItem implements Serializable {
  public final static Tika TIKA = new Tika();
  private final static String DEFAULT_PHOTO_MIMETYPE = "image/jpg";
  private final static String DEFAULT_VIDEO_MIMETYPE = "video/mp4";
  // If Tika cannot detect the mimetype, it returns the binary mimetype. This can be considered null
  private final static String DEFAULT_BINARY_MIMETYPE = "application/octet-stream";
  private final static SimpleDateFormat CREATION_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

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
  // TODO akshaysinghh - rename the field to creationTime since creation time is what all the
  //  services use to display the photos timeline, instead of uploadTime.
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
      Optional<String> albumId, GoogleMediaItem mediaItem) throws ParseException{
    Preconditions.checkArgument(mediaItem.isVideo());

    return new VideoModel(
        mediaItem.getFilename(),
        mediaItem.getFetchableUrl(),
        mediaItem.getDescription(),
        getMimeType(mediaItem),
        mediaItem.getId(),
        albumId.orElse(null),
        false /*inTempStore*/,
        getCreationTime(mediaItem));
  }

  public static PhotoModel convertToPhotoModel (
      Optional<String> albumId, GoogleMediaItem mediaItem) throws ParseException{
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
        getCreationTime(mediaItem));
  }

  private static Date getCreationTime(GoogleMediaItem mediaItem) throws ParseException  {
    // cannot be empty or null based. Verified the backend code.
    try {
      return CREATION_TIME_FORMAT.parse(mediaItem.getMediaMetadata().getCreationTime());
    } catch (ParseException parseException) {
      throw new ParseException(String.format("Failed to parse the string %s to get creationTime. "
              + "Let's look into the dataFormatter",
          mediaItem.getMediaMetadata().getCreationTime()), parseException.getErrorOffset());
    }
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
      String mimeType = TIKA.detect(filename);
      if (mimeType.equals(DEFAULT_BINARY_MIMETYPE)) {
        return null;
      }
      return mimeType;
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
