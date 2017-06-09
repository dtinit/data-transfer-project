package org.dataportabilityproject.serviceProviders.smugmug.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class SmugMugAlbumImage {
  @JsonProperty("Title")
  private String title;

  @JsonProperty("Caption")
  private String caption;

  @JsonProperty("Keywords")
  private String keywords;

  @JsonProperty("Format")
  private String format;

  @JsonProperty("Latitude")
  private String latitude;

  @JsonProperty("Longitude")
  private String longitude;

  @JsonProperty("FileName")
  private String fileName;

  @JsonProperty("ArchivedUri")
  private String archivedUri;

  @JsonProperty("Uris")
  private Map<String, SmugMugUrl> uris;

  public String getTitle() {
    return title;
  }

  public String getCaption() {
    return caption;
  }

  public String getFileName() {
    return fileName;
  }

  public String getArchivedUri() {
    return archivedUri;
  }

  public Map<String, SmugMugUrl> getUris() {
    return uris;
  }

  public String getFormat() {
    return format;
  }
}
