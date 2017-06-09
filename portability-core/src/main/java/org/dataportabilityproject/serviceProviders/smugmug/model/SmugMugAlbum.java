package org.dataportabilityproject.serviceProviders.smugmug.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class SmugMugAlbum {
  @JsonProperty("NiceName")
  private String niceName;

  @JsonProperty("UrlName")
  private String urlName;

  @JsonProperty("Title")
  private String title;

  @JsonProperty("Name")
  private String name;

  @JsonProperty("Description")
  private String description;

  @JsonProperty("AllowDownloads")
  private String allowDownloads;

  @JsonProperty("AlbumKey")
  private String albumKey;

  @JsonProperty("NodeID")
  private String nodeID;

  @JsonProperty("ImageCount")
  private int imageCount;

  @JsonProperty("UrlPath")
  private String urlPath;

  @JsonProperty("Uri")
  private String uri;

  @JsonProperty("Uris")
  private Map<String, SmugMugUrl> uris;

  public Map<String, SmugMugUrl> getUris() {
    return uris;
  }

  public String getTitle() {
    return title;
  }

  public String getAlbumKey() {
    return albumKey;
  }

  public String getDescription() {
    return description;
  }
}
