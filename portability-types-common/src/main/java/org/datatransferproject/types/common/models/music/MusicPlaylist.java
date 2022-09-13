/*
 * Copyright 2022 The Data Transfer Project Authors.
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

package org.datatransferproject.types.common.models.music;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import java.time.Instant;
import java.util.Objects;

/**
 * POJO for https://schema.org/MusicPlaylist
 */
public class MusicPlaylist {

  private final String id;
  private final String title;
  private final String description;
  private final Instant createTime;
  private final Instant updateTime;

  @JsonCreator
  public MusicPlaylist(
      @JsonProperty("id") String id,
      @JsonProperty("title") String title,
      @JsonProperty("description") String description,
      @JsonProperty("createTime") Instant createTime,
      @JsonProperty("updateTime") Instant updateTime) {
    if (isNullOrEmpty(id)) {
      throw new IllegalArgumentException("id must be for MusicPlaylist");
    }
    this.id = id;
    this.title = title;
    this.description = description;
    this.createTime = createTime;
    this.updateTime = updateTime;
  }

  public String getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public Instant getCreateTime() {
    return createTime;
  }

  public Instant getUpdateTime() {
    return updateTime;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", getId())
        .add("title", getTitle())
        .add("description", getDescription())
        .add("createTime", getCreateTime())
        .add("updateTime", getUpdateTime())
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MusicPlaylist)) {
      return false;
    }
    MusicPlaylist that = (MusicPlaylist) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
