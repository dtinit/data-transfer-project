package org.dataportabilityproject.serviceProviders.smugmug.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class SmugMugAlbumInfoResponse {
  @JsonProperty("Uri")
  private String uri;

  @JsonProperty("Locator")
  private String locator;

  @JsonProperty("LocatorType")
  private String locatorType;

  @JsonProperty("AlbumImage")
  private List<SmugMugAlbumImage> images;

  @JsonProperty("Pages")
  private SmugMugPageInfo pages;

  public List<SmugMugAlbumImage> getImages() {
    return images;
  }

  public SmugMugPageInfo getPageInfo() {
    return pages;
  }
}
