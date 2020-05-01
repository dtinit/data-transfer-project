package org.datatransferproject.spi.transfer.types;

public enum FailureReasons {
  DESTINATION_FULL("DESTINATION_FULL"),
  INVALID_TOKEN("INVALID_TOKEN"),
  PERMISSION_DENIED("PERMISSION_DENIED"),
  UPLOAD_ERROR("UPLOAD_ERROR");

  private final String string;

  FailureReasons(String string) {
    this.string = string;
  }

  @Override
  public String toString() {
    return string;
  }
}
