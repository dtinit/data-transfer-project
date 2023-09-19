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
 * POJO for https://schema.org/MusicRecording
 */
public class MusicRecording {

  private final String isrcCode;
  private final String title;
  private final long durationMillis;
  private final MusicRelease musicRelease;
  private final List<MusicGroup> byArtists;

  // Whether the Music entity contains objectionable language.
  private final boolean isExplicit;

  @JsonCreator
  public MusicRecording(
      @JsonProperty("isrcCode") String isrcCode,
      @JsonProperty("title") String title,
      @JsonProperty("durationMillis") long durationMillis,
      @JsonProperty("musicRelease") MusicRelease musicRelease,
      @JsonProperty("byArtists") List<MusicGroup> byArtists,
      @JsonProperty("isExplicit") boolean isExplicit) {
    this.isrcCode = isrcCode;
    this.title = title;
    this.durationMillis = durationMillis;
    this.musicRelease = musicRelease;
    this.byArtists = byArtists;
    this.isExplicit = isExplicit;
  }

  public String getIsrcCode() {
    return isrcCode;
  }

  public String getTitle() {
    return title;
  }

  public long getDurationMillis() {
    return durationMillis;
  }

  public MusicRelease getMusicRelease() {
    return musicRelease;
  }

  public List<MusicGroup> getByArtists() {
    return byArtists;
  }

  public boolean getIsExplicit() {
    return isExplicit;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("isrcCode", getIsrcCode())
        .add("title", getTitle())
        .add("durationMillis", getDurationMillis())
        .add("musicRelease", getMusicRelease())
        .add("byArtists", getByArtists())
        .add("isExplicit", getIsExplicit())
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MusicRecording)) {
      return false;
    }
    MusicRecording that = (MusicRecording) o;
    return Objects.equals(isrcCode, that.isrcCode)
        && Objects.equals(title, that.title)
        && Objects.equals(durationMillis, that.durationMillis)
        && Objects.equals(musicRelease, that.musicRelease)
        && Objects.equals(byArtists, that.byArtists)
        && Objects.equals(isExplicit, that.isExplicit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(isrcCode, title, durationMillis, musicRelease, byArtists, isExplicit);
  }
}
