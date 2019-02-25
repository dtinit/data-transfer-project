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

package org.datatransferproject.types.common.models.videos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.datatransferproject.types.common.models.MediaObject;

public class VideoObject extends MediaObject {

  private String dataId;
  private String albumId;
  private boolean inTempStore;

  @JsonCreator
  public VideoObject(
          @JsonProperty("name") String name,
          @JsonProperty("contentUrl") String contentUrl,
          @JsonProperty("description") String description,
          @JsonProperty("encodingFormat") String encodingFormat,
          @JsonProperty("dataId") String dataId,
          @JsonProperty("albumId") String albumId,
          @JsonProperty("inTempStore") boolean inTempStore) {
    setName(name);
    setContentUrl(contentUrl);
    setDescription(description);
    setEncodingFormat(encodingFormat);
    this.dataId = dataId;
    this.albumId = albumId;
    this.inTempStore = inTempStore;
  }

  public String getAlbumId() {
    return albumId;
  }

  public String getDataId() {
    return dataId;
  }


  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
            .add("name", getName())
            .add("contentUrl", getContentUrl())
            .add("description", getDescription())
            .add("encodingFormat", getEncodingFormat())
            .add("dataId", dataId)
            .add("albumId", albumId)
            .add("inTempStore", inTempStore)
            .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VideoObject that = (VideoObject) o;
    return Objects.equal(getName(), that.getName()) &&
            Objects.equal(getContentUrl(), that.getContentUrl()) &&
            Objects.equal(getDescription(), that.getDescription()) &&
            Objects.equal(getEncodingFormat(), that.getEncodingFormat()) &&
            Objects.equal(getDataId(), that.getDataId()) &&
            Objects.equal(getAlbumId(), that.getAlbumId());
  }
}
