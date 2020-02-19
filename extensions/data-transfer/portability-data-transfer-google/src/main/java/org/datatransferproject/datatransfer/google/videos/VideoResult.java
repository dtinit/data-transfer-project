package org.datatransferproject.datatransfer.google.videos;

import java.io.Serializable;

class VideoResult implements Serializable {
  private String id;
  private Long bytes;

  public VideoResult(String id, Long bytes) {
    this.id = id;
    this.bytes = bytes;
  }

  public String getId() {
    return id;
  }

  public Long getBytes() {
    return bytes;
  }
}
