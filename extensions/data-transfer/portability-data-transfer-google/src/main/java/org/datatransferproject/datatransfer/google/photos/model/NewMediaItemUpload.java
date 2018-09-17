package org.datatransferproject.datatransfer.google.photos.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.Nullable;

@JsonInclude(Include.NON_NULL)
public class NewMediaItemUpload {
  @JsonProperty("albumId")
  private String albumId;

  @JsonProperty("newMediaItems")
  private List<NewMediaItem> newMediaItems;

  @JsonProperty("albumPosition")
  private AlbumPosition albumPosition;

  public NewMediaItemUpload(@Nullable String albumId, List<NewMediaItem> newMediaItems,
      @Nullable AlbumPosition albumPosition) {
    this.albumId = albumId;
    this.newMediaItems = newMediaItems;
    this.albumPosition = albumPosition;
  }

  public AlbumPosition getAlbumPosition() {
    return albumPosition;
  }

  public String getAlbumId() {
    return albumId;
  }

  public List<NewMediaItem> getNewMediaItems() {
    return newMediaItems;
  }
}
