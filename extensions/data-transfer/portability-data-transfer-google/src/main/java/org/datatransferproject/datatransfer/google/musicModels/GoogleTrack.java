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
import java.util.Arrays;
import java.util.Objects;

/** Represents a track (eg: a song) as returned by the Google Music API. */
public class GoogleTrack {

  @JsonProperty("isrc")
  private String isrc;

  @JsonProperty("release")
  private GoogleRelease release;

  @JsonProperty("trackTitle")
  private String trackTitle;

  @JsonProperty("artistTitles")
  private String[] artistTitles;

  @JsonProperty("durationMillis")
  private long durationMillis;

  public String getIsrc() {
    return isrc;
  }

  public GoogleRelease getRelease() {
    return release;
  }

  public String getTrackTitle() {
    return trackTitle;
  }

  public String[] getArtistTitles() {
    return artistTitles;
  }

  public long getDurationMillis() {
    return durationMillis;
  }

  public void setIsrc(String isrc) {
    this.isrc = isrc;
  }

  public void setRelease(GoogleRelease release) {
    this.release = release;
  }

  public void setTrackTitle(String trackTitle) {
    this.trackTitle = trackTitle;
  }

  public void setArtistTitles(String[] artistTitles) {
    this.artistTitles = artistTitles;
  }

  public void setDurationMillis(long durationMillis) {
    this.durationMillis = durationMillis;
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
        && Objects.equals(trackTitle, that.trackTitle)
        && Arrays.equals(artistTitles, that.artistTitles)
        && durationMillis == that.durationMillis;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getIsrc(),
        getRelease(),
        getTrackTitle(),
        Arrays.hashCode(getArtistTitles()),
        getDurationMillis());
  }
}
