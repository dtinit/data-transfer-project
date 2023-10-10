package org.datatransferproject.datatransfer.google.musicModels;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class ImportPlaylistItemRequest {

  @JsonProperty("playlistId")
  private String playlistId;

  @JsonProperty("playlistItem")
  private GooglePlaylistItem playlistItem;

  public ImportPlaylistItemRequest(String playlistId, GooglePlaylistItem playlistItem) {
    this.playlistId = playlistId;
    this.playlistItem = playlistItem;
  }

  public String getPlaylistId() {
    return playlistId;
  }

  public GooglePlaylistItem getPlaylistItem() {
    return playlistItem;
  }

  public void setPlaylistId(String playlistId) {
    this.playlistId = playlistId;
  }

  public void setPlaylistItem(
      GooglePlaylistItem playlistItem) {
    this.playlistItem = playlistItem;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ImportPlaylistItemRequest)) {
      return false;
    }
    ImportPlaylistItemRequest that = (ImportPlaylistItemRequest) o;
    return Objects.equals(playlistId, that.playlistId) && Objects.equals(playlistItem, that.playlistItem);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getPlaylistId(), getPlaylistItem());
  }
}
