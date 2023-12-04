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

package org.datatransferproject.datatransfer.google.musicModels;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.protobuf.util.Durations;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a track (eg: a song) as returned by the Google Music API.
 */
public class GoogleTrack {

  @JsonProperty("isrc")
  private String isrc;

  @JsonProperty("releaseReference")
  private GoogleRelease releaseReference;

  @JsonProperty("title")
  private String title;

  @JsonProperty("artistReferences")
  private GoogleArtist[] artistReferences;

  // Json format of google.protobuf.Duration is encoded as a string ends in the suffix "s".
  // 3 seconds and 1 microsecond should be expressed in JSON format as "3.000001s".
  @JsonProperty("duration")
  private String duration;

  @JsonProperty("explicitType")
  private String explicitType;

  public String getIsrc() {
    return isrc;
  }

  public GoogleRelease getReleaseReference() {
    return releaseReference;
  }

  public String getTitle() {
    return title;
  }

  public GoogleArtist[] getArtistReferences() {
    return artistReferences;
  }

  public String getDuration() {
    return duration;
  }

  public String getExplicitType() {
    return explicitType;
  }

  public long convertDurationToMillions() throws ParseException {
    if (this.duration == null || this.duration.isEmpty()) {
      return 0L;
    }
    return Durations.toMillis(Durations.parse(duration));
  }

  public void setIsrc(String isrc) {
    this.isrc = isrc;
  }

  public void setReleaseReference(GoogleRelease releaseReference) {
    this.releaseReference = releaseReference;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setArtistReferences(GoogleArtist[] artistReferences) {
    this.artistReferences = artistReferences;
  }

  public void setDuration(String duration) {
    this.duration = duration;
  }

  public void setExplicitType(String explicitType) {
    this.explicitType = explicitType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof GoogleTrack)) {
      return false;
    }
    GoogleTrack that = (GoogleTrack) o;
    return Objects.equals(isrc, that.isrc)
        && Objects.equals(releaseReference, that.releaseReference)
        && Objects.equals(title, that.title)
        && Arrays.equals(artistReferences, that.artistReferences)
        && Objects.equals(duration, that.duration)
        && Objects.equals(explicitType, that.explicitType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getIsrc(),
        getReleaseReference(),
        getTitle(),
        Arrays.hashCode(getArtistReferences()),
        getDuration(),
        getExplicitType());
  }
}
