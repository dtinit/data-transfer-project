package org.dataportabilityproject.serviceProviders.smugmug.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class SmugmugAlbumsResponse {
  @JsonProperty("Uri")
  private String uri;

  @JsonProperty("Locator")
  private String locator;

  @JsonProperty("LocatorType")
  private String locatorType;

  @JsonProperty("Album")
  private List<SmugMugAlbum> albums;

  @JsonProperty("Pages")
  private SmugMugPageInfo pageInfo;

  public List<SmugMugAlbum> getAlbums() {
    return albums;
  }

  public SmugMugPageInfo getPageInfo() {
    return pageInfo;
  }

  public String getUri() {
    return uri;
  }
}
