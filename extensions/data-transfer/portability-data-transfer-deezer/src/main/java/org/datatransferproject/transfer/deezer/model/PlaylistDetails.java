package org.datatransferproject.transfer.deezer.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlaylistDetails extends PlaylistSummary {
  @JsonProperty("tracks")
  private TrackCollection tracks;

  public TrackCollection getTrackCollection() {
    return tracks;
  }
}
