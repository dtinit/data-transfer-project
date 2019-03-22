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
package org.datatransferproject.types.common.models.photos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class PhotoModel {

  private final String title;
  private final String fetchableUrl;
  private final String description;
  private final String mediaType;
  private final String albumId;
  private final boolean inTempStore;
  private String dataId;

  @JsonCreator
  public PhotoModel(
      @JsonProperty("title") String title,
      @JsonProperty("fetchableUrl") String fetchableUrl,
      @JsonProperty("description") String description,
      @JsonProperty("mediaType") String mediaType,
      @JsonProperty("dataId") String dataId,
      @JsonProperty("albumId") String albumId,
      @JsonProperty("inTempStore") boolean inTempStore) {
    this.title = title;
    this.fetchableUrl = fetchableUrl;
    this.description = description;
    this.mediaType = mediaType;
    if (dataId == null || dataId.isEmpty()) {
      throw new IllegalArgumentException("dataID must be set");
    }
    this.dataId = dataId;
    this.albumId = albumId;
    this.inTempStore = inTempStore;
  }

  public String getTitle() {
    return title;
  }

  public String getFetchableUrl() {
    return fetchableUrl;
  }

  public String getDescription() {
    return description;
  }

  public String getMediaType() {
    return mediaType;
  }

  public String getAlbumId() {
    return albumId;
  }

  public String getDataId() {
    return dataId;
  }

  public boolean isInTempStore() { return inTempStore; }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("title", title)
        .add("fetchableUrl", fetchableUrl)
        .add("description", description)
        .add("mediaType", mediaType)
        .add("dataId", dataId)
        .add("albumId", albumId)
        .add("inTempStore", inTempStore)
        .toString();
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PhotoModel that = (PhotoModel) o;
    return Objects.equal(getTitle(), that.getTitle()) &&
            Objects.equal(getFetchableUrl(), that.getFetchableUrl()) &&
            Objects.equal(getDescription(), that.getDescription()) &&
            Objects.equal(getMediaType(), that.getMediaType()) &&
            Objects.equal(getDataId(), that.getDataId()) &&
            Objects.equal(getAlbumId(), that.getAlbumId());
  }
}
