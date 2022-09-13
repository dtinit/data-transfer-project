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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import java.util.List;
import java.util.Objects;

/**
 * POJO for https://schema.org/MusicRelease
 */
public class MusicRelease {

  private final String icpnCode;
  private final String title;
  private List<MusicGroup> byArtists;

  @JsonCreator
  public MusicRelease(
      @JsonProperty("icpnCode") String icpnCode,
      @JsonProperty("title") String title,
      @JsonProperty("byArtists") List<MusicGroup> byArtists) {
    this.icpnCode = icpnCode;
    this.title = title;
    this.byArtists = byArtists;
  }

  public String getIcpnCode() {
    return icpnCode;
  }

  public String getTitle() {
    return title;
  }

  public List<MusicGroup> getByArtists() {
    return byArtists;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("icpnCode", getIcpnCode())
        .add("title", getTitle())
        .add("byArtists", getByArtists())
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MusicRelease)) {
      return false;
    }
    MusicRelease that = (MusicRelease) o;
    return Objects.equals(icpnCode, that.icpnCode) && Objects.equals(title, that.title)
        && Objects.equals(byArtists, that.byArtists);
  }

  @Override
  public int hashCode() {
    return Objects.hash(icpnCode, title, byArtists);
  }
}
