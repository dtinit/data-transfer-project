package org.dataportabilityproject.serviceProviders.smugmug.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * A SmugMug user object.
 */
public class SmugMugUser {
  @JsonProperty("Name")
  private String name;

  @JsonProperty("Uris")
  private Map<String, SmugMugUrl> uris;

  public String getName() {
    return name;
  }

  public Map<String, SmugMugUrl> getUris() {
    return uris;
  }
}
