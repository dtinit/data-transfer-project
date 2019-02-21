package org.datatransferproject.types.common.models.playlists;

/**
 * POJO of https://schema.org/CreativeWork
 */
public class CreativeWork {
  // NOTE: only a subset of fields are used so far, feel free to add more fields from the spec as
  // needed.
  protected String headline;

  public CreativeWork(String headline) {
    this.headline = headline;
  }

  public String headline() {
    return headline;
  }
}
