package org.datatransferproject.types.common.models.playlists;

/**
 * POJO https://schema.org/MusicAlbum
 */
// Note
public class MusicAlbum extends CreativeWork {
  private String headline;

  public MusicAlbum(String headline) {
    super(headline);
  }
}
