package org.datatransferproject.types.common.models.playlists;

public class CreativeWork {
  protected String headline;

  public CreativeWork(String headline) {
    this.headline = headline;
  }

  public String headline() {
    return headline;
  }
}
