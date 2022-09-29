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
import com.google.common.base.Preconditions;
import java.util.Objects;

/**
 * POJO for MusicPlaylistItem
 */
public class MusicPlaylistItem {

  /**
   * The MusicRecording belongs to the playlist item. Required.
   */
  private final MusicRecording track;

  /**
   * The playlist id of playlist item. Required. This ID should be unique within the upstream system
   * from which the data originated. No particular format is guaranteed (eg: it might like a URI
   * "music.acme.co/playlists/23d89fea", a plain ID "28921", or anything else really).
   */
  private final String playlistId;

  /**
   * The index of track inside the playlist. Best efforts to make sure the correct order of playlist
   * items. Optional. Any int is valid - these numbers are meant purely to be relative to other
   * MusicPlaylistItem {@code order} values
   */
  private final int order;

  @JsonCreator
  public MusicPlaylistItem(
      @JsonProperty("track") MusicRecording track,
      @JsonProperty("playlistId") String playlistId,
      @JsonProperty("order") int order) {
    Preconditions.checkArgument(track != null, "track must be set for MusicPlaylistItem");
    this.track = track;
    Preconditions.checkArgument(!isNullOrEmpty(playlistId),
        "non-empty playlistId must be set for MusicPlaylistItem");
    this.playlistId = playlistId;
    this.order = order;
  }

  public MusicRecording getTrack() {
    return track;
  }

  public String getPlaylistId() {
    return playlistId;
  }

  public int getOrder() {
    return order;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("track", getTrack())
        .add("playlistId", getPlaylistId())
        .add("order", getOrder())
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MusicPlaylistItem)) {
      return false;
    }
    MusicPlaylistItem that = (MusicPlaylistItem) o;
    return Objects.equals(track, that.track)
        && Objects.equals(playlistId, that.playlistId)
        && order == that.order;
  }

  @Override
  public int hashCode() {
    return Objects.hash(track, playlistId, order);
  }
}
