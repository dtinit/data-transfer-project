package org.datatransferproject.transfer.deezer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TrackCollection {
  private Track[] data;

  @JsonCreator
  TrackCollection(@JsonProperty("data") Track[] data) {
    this.data = data;
  }

  public Track[] getTracks() {
    return data;
  }
}
