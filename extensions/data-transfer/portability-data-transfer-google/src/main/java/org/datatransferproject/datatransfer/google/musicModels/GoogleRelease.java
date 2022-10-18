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

/**
 * Class representing a release as returned by the Google Music API. A release is a variation of an
 * Album, e.g. Clean, Explicit, Deluxe, Remastered, etc. that contains a list of Tracks.
 */
public class GoogleRelease {

  @JsonProperty("icpn")
  private String icpn;

  @JsonProperty("releaseTitle")
  private String releaseTitle;

  @JsonProperty("artistTitles")
  private String[] artistTitles;

  public String getIcpn() {
    return icpn;
  }

  public String getReleaseTitle() {
    return releaseTitle;
  }

  public String[] getArtistTitles() {
    return artistTitles;
  }

  public void setIcpn(String icpn) {
    this.icpn = icpn;
  }

  public void setReleaseTitle(String releaseTitle) {
    this.releaseTitle = releaseTitle;
  }

  public void setArtistTitles(String[] artistTitles) {
    this.artistTitles = artistTitles;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof GoogleRelease)) {
      return false;
    }
    GoogleRelease that = (GoogleRelease) o;
    return Objects.equals(icpn, that.icpn)
        && Objects.equals(releaseTitle, that.releaseTitle)
        && Arrays.equals(artistTitles, that.artistTitles);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getIcpn(), getReleaseTitle(), Arrays.hashCode(getArtistTitles()));
  }
}
