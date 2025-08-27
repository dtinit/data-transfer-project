package org.datatransferproject.datatransfer.google.musicModels;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExportReleaseResponse {

  @JsonProperty("releases")
  private GoogleRelease[] releases;


  @JsonProperty("nextPageToken")
  private String nextPageToken;

  public GoogleRelease[] getReleases() {
    return releases;
  }

  public String getNextPageToken() {
    return nextPageToken;
  }

}