package org.datatransferproject.types.common.models.playlists;

/**
 * POJO https://schema.org/MusicAlbum
 */
public class MusicAlbum extends CreativeWork {
  // NOTE: only a subset of fields are used so far, feel free to add more fields from the spec as
  // needed.
  private String headline;

  public MusicAlbum(String headline) {
    super(headline);
  }
}
