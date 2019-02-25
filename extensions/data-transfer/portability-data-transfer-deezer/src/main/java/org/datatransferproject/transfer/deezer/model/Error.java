package org.datatransferproject.transfer.deezer.model;

public class Error {
  private String type;
  private String message;
  private int code;

  public String getType() {
    return type;
  }

  public String getMessage() {
    return message;
  }

  public int getCode() {
    return code;
  }

  public String toString() {
    return String.format("Error{type=%s message=%s code=%s}",
        type, message, code);
  }
}
