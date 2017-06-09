package org.dataportabilityproject.serviceProviders.smugmug.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SmugmugAlbumResponse {
  @JsonProperty("Uri")
  private String uri;

  @JsonProperty("Locator")
  private String locator;

  @JsonProperty("LocatorType")
  private String locatorType;

  @JsonProperty("Album")
  private SmugMugAlbum album;

  public SmugMugAlbum getAlbum() {
    return album;
  }

  public String getUri() {
    return uri;
  }
}
