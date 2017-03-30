package org.dataportabilityproject.serviceProviders.instagram.model;

/**
 * DataModel for a image in the Instagram API, contains references to multiple resolutions.
 * Instantiated by JSON mapping.
 */
public final class ImageObject {
  private ImageData thumbnail;

  private  ImageData standard_resolution;

  private  ImageData low_resolution;

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
