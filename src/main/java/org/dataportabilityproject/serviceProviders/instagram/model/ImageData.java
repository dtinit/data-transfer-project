package org.dataportabilityproject.serviceProviders.instagram.model;

/**
 * DataModel for a single image object in the Instagram API. Instantiated
 * by JSON mapping.
 */
public final class ImageData {
  private String url;

  private int height;

  private int width;

  public String getUrl() {
    return url;
  }

  public int getHeight() {
    return height;
  }

  public int getWidth() {
    return width;
  }
}
