package org.datatransferproject.types.common.models.playlists;

import org.datatransferproject.types.common.models.CreativeWork;

/**
 * POJO for https://schema.org/MusicRecording
 */
public class MusicRecording extends CreativeWork {
  // Note this is only a partial implementation for fields needed so far, feel free to add more
  // from the spec as needed.
  private String isrcCode;
  private MusicAlbum musicAlbum;
  private MusicGroup byArtist;

  public MusicRecording(String headline,
                        String isrcCode,
                        MusicAlbum musicAlbum,
                        MusicGroup byArtist) {
    setHeadline(headline);
    this.isrcCode = isrcCode;
    this.musicAlbum = musicAlbum;
    this.byArtist = byArtist;
  }


  public String getIsrcCode() {
    return isrcCode;
  }

  public MusicAlbum getMusicAlbum() {
    return musicAlbum;
  }

  public MusicGroup getByArtist() {
    return byArtist;
  }
}
