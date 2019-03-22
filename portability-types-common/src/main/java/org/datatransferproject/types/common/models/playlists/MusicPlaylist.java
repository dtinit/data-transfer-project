package org.datatransferproject.types.common.models.playlists;

import com.google.common.collect.ImmutableList;
import org.datatransferproject.types.common.models.CreativeWork;

public class MusicPlaylist extends CreativeWork {
  // NOTE: only a subset of fields are used so far, feel free to add more fields from the spec as
  // needed.
  private ImmutableList<MusicRecording> track;

  public MusicPlaylist(
      String identifier,
      String headline,
      Iterable<MusicRecording> tracks) {
    super(identifier);
    setHeadline(headline);
    this.track = ImmutableList.copyOf(tracks);
  }

  public ImmutableList<MusicRecording> getTrack() {
    return track;
  }
}
