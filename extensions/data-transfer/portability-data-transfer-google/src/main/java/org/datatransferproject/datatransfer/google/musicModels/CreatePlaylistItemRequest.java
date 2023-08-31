package org.datatransferproject.datatransfer.google.musicModels;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class CreatePlaylistItemRequest {

  @JsonProperty("parent")
  private String parent;

  @JsonProperty("playlistItem")
  private GooglePlaylistItem playlistItem;

  public CreatePlaylistItemRequest(String parent, GooglePlaylistItem playlistItem) {
    this.parent = parent;
    this.playlistItem = playlistItem;
  }

  public String getParent() {
    return parent;
  }

  public GooglePlaylistItem getPlaylistItem() {
    return playlistItem;
  }

  public void setParent(String parent) {
    this.parent = parent;
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
    if (!(o instanceof CreatePlaylistItemRequest)) {
      return false;
    }
    CreatePlaylistItemRequest that = (CreatePlaylistItemRequest) o;
    return Objects.equals(parent, that.parent) && Objects.equals(playlistItem, that.playlistItem);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getParent(), getPlaylistItem());
  }
}
