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
import java.util.Objects;

/** Class representing a playlist item as returned by the Google Music API. */
public class GooglePlaylistItem {
  @JsonProperty("track")
  private GoogleTrack track;

  @JsonProperty("order")
  private int order;

  public GoogleTrack getTrack() {
    return track;
  }

  public int getOrder() {
    return order;
  }

  public void setTrack(GoogleTrack track) {
    this.track = track;
  }

  public void setOrder(int order) {
    this.order = order;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof GooglePlaylistItem)) {
      return false;
    }
    GooglePlaylistItem that = (GooglePlaylistItem) o;
    return Objects.equals(track, that.track) && order == that.order;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getTrack(), getOrder());
  }
}
