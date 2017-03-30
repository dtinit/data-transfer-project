package org.dataportabilityproject.serviceProviders.instagram.model;

/**
 * DataModel for a Caption object in the Instagram API. Instantiated
 * by JSON mapping.
 */
public final class Caption {
  private String created_time;

  private String id;

  private String text;

  public String getCreated_time() {
    return created_time;
  }

  public String getId() {
    return id;
  }

  public String getText() {
    return text;
  }
}
