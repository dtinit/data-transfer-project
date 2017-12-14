package org.dataportabilityproject.shared.settings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Settings for {@code PortabilityApiServer}.
 */
public class ApiSettings {
  // TODO(rtannenbaum): Change these to URL types instead of String
  // Base url for all calls within the application
  private final String baseUrl;
  // Base url for direct to api calls within the application
  private final String baseApiUrl;

  @JsonCreator
  public ApiSettings(
      @JsonProperty(value="baseUrl", required=true) String baseUrl,
      @JsonProperty(value="baseApiUrl", required=true) String baseApiUrl) {
    this.baseUrl = baseUrl;
    this.baseApiUrl = baseApiUrl;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public String getBaseApiUrl() {
    return baseApiUrl;
  }
}
