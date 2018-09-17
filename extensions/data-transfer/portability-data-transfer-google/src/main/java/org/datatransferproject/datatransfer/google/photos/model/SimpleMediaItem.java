package org.datatransferproject.datatransfer.google.photos.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SimpleMediaItem {
  @JsonProperty("uploadToken")
  private String uploadToken;

  SimpleMediaItem(String uploadToken) {
    this.uploadToken = uploadToken;
  }

  public String getUploadToken() {
    return uploadToken;
  }
}
