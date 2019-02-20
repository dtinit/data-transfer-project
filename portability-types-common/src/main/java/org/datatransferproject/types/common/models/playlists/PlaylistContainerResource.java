package org.datatransferproject.types.common.models.playlists;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.datatransferproject.types.common.models.ContainerResource;

/** A Wrapper for a group of playlists. */
@JsonTypeName("PlaylistContainerResource")
public class PlaylistContainerResource extends ContainerResource {
  private final Collection<MusicPlaylist> playlists;

  @JsonCreator
  public PlaylistContainerResource(
      @JsonProperty("playlists") Collection<MusicPlaylist> playlists) {
    this.playlists = playlists == null ? ImmutableList.of() : playlists;
  }

  public Collection<MusicPlaylist> getLists() {
    return playlists;
  }

}
