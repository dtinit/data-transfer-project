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

  public MusicRecording(
      String identifier,
      String headline,
      String isrcCode,
      MusicAlbum musicAlbum,
      MusicGroup byArtist) {
    super(identifier);
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

  @Override
  public String toString() {
    return String.format("MusicRecording{id: %s, headline: %s, isrc: %s}",
        getIdentifier(), getHeadline(), getIsrcCode());
  }
}
