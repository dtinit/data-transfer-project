package org.dataportabilityproject.serviceProviders.instagram.model;

import java.util.List;

/**
 * DataModel for the result of a media query in the Instagram API. Instantiated by JSON mapping.
 */
public final class MediaResponse {
  private Meta meta;

  private List<MediaFeedData> data;

  public Meta getMeta() {
    return meta;
  }

  public List<MediaFeedData> getData() {
    return data;
  }
}
