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

  @JsonProperty("release")
  private GoogleRelease release;

  @JsonProperty("title")
  private String title;

  @JsonProperty("artists")
  private GoogleArtist[] artists;

  // Json format of google.protobuf.Duration is encoded as a string ends in the suffix "s".
  // 3 seconds and 1 microsecond should be expressed in JSON format as "3.000001s".
  @JsonProperty("duration")
  private String duration;

  public String getIsrc() {
    return isrc;
  }

  public GoogleRelease getRelease() {
    return release;
  }

  public String getTitle() {
    return title;
  }

  public GoogleArtist[] getArtists() {
    return artists;
  }

  public String getDuration() {
    return duration;
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

  public void setRelease(GoogleRelease release) {
    this.release = release;
  }

  public void setTitle(String trackTitle) {
    this.title = title;
  }

  public void setArtists(GoogleArtist[] artists) {
    this.artists = artists;
  }

  public void setDuration(String duration) {
    this.duration = duration;
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
        && Objects.equals(release, that.release)
        && Objects.equals(title, that.title)
        && Arrays.equals(artists, that.artists)
        && Objects.equals(duration, that.duration);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getIsrc(),
        getRelease(),
        getTitle(),
        Arrays.hashCode(getArtists()),
        getDuration());
  }
}
