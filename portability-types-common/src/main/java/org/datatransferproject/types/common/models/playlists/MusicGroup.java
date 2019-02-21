package org.datatransferproject.types.common.models.playlists;

/**
 * POJO for https://schema.org/MusicGroup
 */
public class MusicGroup {
  // NOTE: only a subset of fields are used so far, feel free to add more fields from the spec as
  // needed.
  private String headline;

  public MusicGroup(String headline) {
    this.headline = headline;
  }

  public String getHeadline() {
    return headline;
  }
}
