package org.datatransferproject.datatransfer.google.musicModels;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class ImportPlaylistRequest {
  @JsonProperty("playlist")
  GooglePlaylist playlist;

  @JsonProperty("originalPlaylistId")
  String originalPlaylistId;

  public ImportPlaylistRequest(GooglePlaylist playlist, String originalPlaylistId) {
    this.playlist = playlist;
    this.originalPlaylistId = originalPlaylistId;
  }

  public GooglePlaylist getPlaylist() {
    return playlist;
  }

  public String getOriginalPlaylistId() {
    return originalPlaylistId;
  }

  public void setPlaylist(GooglePlaylist playlist) {
    this.playlist = playlist;
  }

  public void setOriginalPlaylistId(String originalPlaylistId) {
    this.originalPlaylistId = originalPlaylistId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ImportPlaylistRequest)) {
      return false;
    }
    ImportPlaylistRequest that = (ImportPlaylistRequest) o;
    return Objects.equals(playlist, that.playlist) && Objects.equals(originalPlaylistId, that.originalPlaylistId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getPlaylist(), getOriginalPlaylistId());
  }
}
