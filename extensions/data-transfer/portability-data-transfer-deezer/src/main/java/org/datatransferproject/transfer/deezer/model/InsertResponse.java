package org.datatransferproject.transfer.deezer.model;

public class InsertResponse {
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
