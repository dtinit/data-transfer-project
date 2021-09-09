package org.datatransferproject.datatransfer.google.photos;

import java.io.Serializable;

public class MediaResult implements Serializable {
  private String id;
  private Long bytes;

  public MediaResult(String id, Long bytes) {
    this.id = id;
    this.bytes = bytes == null ? 0 : bytes;
  }

  public String getId() {
    return id;
  }

  public Long getBytes() {
    return bytes;
  }
}
