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
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.datatransferproject.types.common.models.ContainerResource;

/**
 * A Wrapper for a group of music entities.
 */
@JsonTypeName("MusicContainerResource")
public class MusicContainerResource extends ContainerResource {

  private final Collection<MusicPlaylist> playlists;
  private final List<MusicPlaylistItem> playlistItems;
  private final Collection<MusicRecording> tracks;
  private final Collection<MusicRelease> releases;

  @JsonCreator
  public MusicContainerResource(
      @JsonProperty("playlists") Collection<MusicPlaylist> playlists,
      @JsonProperty("playlistItems") List<MusicPlaylistItem> playlistItems,
      @JsonProperty("tracks") Collection<MusicRecording> tracks,
      @JsonProperty("releases") Collection<MusicRelease> releases) {
    this.playlists = playlists == null ? ImmutableList.of() : playlists;
    this.playlistItems = playlistItems == null ? ImmutableList.of() : playlistItems;
    this.tracks = tracks == null ? ImmutableList.of() : tracks;
    this.releases = releases == null ? ImmutableList.of() : releases;
  }

  public Collection<MusicPlaylist> getPlaylists() {
    return playlists;
  }

  public List<MusicPlaylistItem> getPlaylistItems() {
    return playlistItems;
  }

  public Collection<MusicRecording> getTracks() {
    return tracks;
  }

  public Collection<MusicRelease> getReleases() {
    return releases;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MusicContainerResource)) {
      return false;
    }
    MusicContainerResource that = (MusicContainerResource) o;
    return Iterables.elementsEqual(getPlaylists(), that.getPlaylists())
        && Objects.equals(getPlaylistItems(), that.getPlaylistItems())
        && Iterables.elementsEqual(getTracks(), that.getTracks())
        && Iterables.elementsEqual(getReleases(), that.getReleases());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getPlaylists(), getPlaylistItems(), getTracks(), getReleases());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("playlists", getPlaylists())
        .add("playlistItems", getPlaylistItems())
        .add("tracks", getTracks())
        .add("releases", getReleases())
        .toString();
  }
}
