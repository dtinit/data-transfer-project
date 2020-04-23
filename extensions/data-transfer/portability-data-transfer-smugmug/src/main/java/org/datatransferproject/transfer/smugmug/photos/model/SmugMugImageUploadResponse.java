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
package org.datatransferproject.transfer.smugmug.photos.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public final class SmugMugImageUploadResponse {

  @JsonProperty("stat")
  private String stat;

  @JsonProperty("method")
  private String method;

  @JsonProperty("Image")
  private ImageInfo image;

  @JsonCreator
  public SmugMugImageUploadResponse(
      @JsonProperty("stat") String stat,
      @JsonProperty("method") String method,
      @JsonProperty("Image") ImageInfo image) {
    this.stat = stat;
    this.method = method;
    this.image = image;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("stat", stat)
        .add("method", method)
        .add("Image", image)
        .toString();
  }

  public ImageInfo getImageInfo() {
    return image;
  }

  public String getStat() {
    return stat;
  }

  public static class ImageInfo {

    @JsonProperty("ImageUri")
    private String imageUri;

    @JsonProperty("AlbumImageUri")
    private String albumImageUri;

    @JsonProperty("StatusImageReplaceUri")
    private String statusImageReplaceUri;

    @JsonProperty("URL")
    private String url;

    @JsonCreator
    public ImageInfo(
        @JsonProperty("ImageUri") String imageUri,
        @JsonProperty("AlbumImageUri") String albumImageUri,
        @JsonProperty("StatusImageReplaceUri") String statusImageReplaceUri,
        @JsonProperty("URL") String url) {
      this.imageUri = imageUri;
      this.albumImageUri = albumImageUri;
      this.statusImageReplaceUri = statusImageReplaceUri;
      this.url = url;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("ImageUri", imageUri)
          .add("AlbumImageUri", albumImageUri)
          .add("StatusImageReplaceUri", statusImageReplaceUri)
          .add("URL", url)
          .toString();
    }
  }
}
