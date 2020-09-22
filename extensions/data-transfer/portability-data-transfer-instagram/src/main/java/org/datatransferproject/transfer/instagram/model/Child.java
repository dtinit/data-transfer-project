package org.datatransferproject.transfer.instagram.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** DataModel for a media feed in the Instagram API. Instantiated by JSON mapping. */
public final class Child {

  @JsonProperty("id")
  private String id;

  @JsonProperty("media_type")
  private String media_type;

  @JsonProperty("media_url")
  private String media_url;

  public String getId() {
    return id;
  }

  public String getMediaType() {
    return media_type;
  }

  public String getMediaUrl() {
    return media_url;
  }
}
