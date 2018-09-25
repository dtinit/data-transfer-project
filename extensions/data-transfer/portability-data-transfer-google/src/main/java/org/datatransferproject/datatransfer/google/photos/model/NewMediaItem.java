package org.datatransferproject.datatransfer.google.photos.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class NewMediaItem {
  @JsonProperty("description")
  private String description;

  @JsonProperty("simpleMediaItem")
  private SimpleMediaItem simpleMediaItem;

  @JsonCreator
  public NewMediaItem(String description, SimpleMediaItem simpleMediaItem) {
    this.description = description;
    this.simpleMediaItem = simpleMediaItem;
  }

  public NewMediaItem(String description, String uploadToken) {
    this.description = description;
    this.simpleMediaItem = new SimpleMediaItem(uploadToken);
  }

  public SimpleMediaItem getSimpleMediaItem() {
    return simpleMediaItem;
  }

  public String getDescription() {
    return description;
  }

}
