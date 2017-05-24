package org.dataportabilityproject.serviceProviders.instagram.model;

/**
 * DataModel for a media feed in the Instagram API. Instantiated by JSON mapping.
 */
public final class MediaFeedData {
  private String id;

  private String type;

  private String created_time;

  private ImageObject images;

  private Caption caption;

  public String getId() {
    return id;
  }

  public Caption getCaption() {
    return caption;
  }

  public ImageObject getImages() {
    return images;
  }

  public String getCreatedTime() {
    return created_time;
  }

  public String getType() {
    return type;
  }
}
