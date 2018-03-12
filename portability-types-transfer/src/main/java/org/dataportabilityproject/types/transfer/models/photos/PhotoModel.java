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
package org.dataportabilityproject.types.transfer.models.photos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@AutoValue
@JsonDeserialize(builder = PhotoModel.Builder.class)
public abstract class PhotoModel {
  @JsonProperty("title")
  public abstract String getTitle();

  @JsonProperty("mediaType")
  public abstract String getMediaType();

  @Nullable
  @JsonProperty("fetchableUrl")
  public abstract String getFetchableUrl();

  @Nullable
  @JsonProperty("dataId")
  public abstract String getDataId();

  @Nullable
  @JsonProperty("albumId")
  public abstract String getAlbumId();

  @Nullable
  @JsonProperty("description")
  public abstract String getDescription();

  public static Builder builder() {
    // TODO: Fix so we don't need fully qualified name here. This is to get IntelliJ to recognize
    // the class name due to a conflict in package names for our generated code, but the conflict
    // doesn't cause any actual problems with building.
    return new org.dataportabilityproject.types.transfer.models.photos.AutoValue_PhotoModel
        .Builder();
  }

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {
    @JsonCreator
    private static Builder create() {
      return PhotoModel.builder();
    }

    public abstract PhotoModel build();

    @JsonProperty("title")
    public abstract Builder setTitle(String title);

    @JsonProperty("mediaType")
    public abstract Builder setMediaType(String mediaType);

    @JsonProperty("fetchableUrl")
    public abstract Builder setFetchableUrl(String fetchableUrl);

    @JsonProperty("dataId")
    public abstract Builder setDataId(String dataId);

    @JsonProperty("description")
    public abstract Builder setDescription(String description);

    @JsonProperty("albumId")
    public abstract Builder setAlbumId(String albumId);
  }
}
