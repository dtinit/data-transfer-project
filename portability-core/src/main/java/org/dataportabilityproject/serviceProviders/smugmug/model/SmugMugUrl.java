package org.dataportabilityproject.serviceProviders.smugmug.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A JSON representation of a authUrl link in SmugMug
 */
public final class SmugMugUrl {
  @JsonProperty("Uri")
  private String uri;

  @JsonProperty("Locator")
  private String locator;

  @JsonProperty("LocatorType")
  private String locatorType;

  @JsonProperty("UriDescription")
  private String uriDescription;

  @JsonProperty("EndpointType")
  private String endpointType;

  public String getUri() {
    return uri;
  }

  public String getLocator() {
    return locator;
  }

  public String getLocatorType() {
    return locatorType;
  }

  public String getUriDescription() {
    return uriDescription;
  }

  public String getEndpointType() {
    return endpointType;
  }
}
