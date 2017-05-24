package org.dataportabilityproject.serviceProviders.instagram.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DataModel for a image in the Instagram API, contains references to multiple resolutions.
 * Instantiated by JSON mapping.
 */
public final class ImageObject {
  private ImageData thumbnail;

  @JsonProperty("standard_resolution")
  private ImageData standard_resolution;

  @JsonProperty("low_resolution")
  private ImageData low_resolution;

  public ImageData getThumbnail() {
    return thumbnail;
  }

  public ImageData getStandardResolution() {
    return standard_resolution;
  }

  public ImageData getLowResolution() {
    return low_resolution;
  }
}
