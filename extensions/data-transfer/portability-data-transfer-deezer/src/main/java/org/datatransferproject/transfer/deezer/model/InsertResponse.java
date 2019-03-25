package org.datatransferproject.transfer.deezer.model;

import java.io.Serializable;

public class InsertResponse implements Serializable {
  private long id;
  private Error error;

  public long getId() {
    return id;
  }

  public String toString() {
    if (error != null) {
      return error.toString();
    }
    return String.format("InsertResponse{id=%s}", id);
  }

  public Error getError() {
    return error;
  }
}
