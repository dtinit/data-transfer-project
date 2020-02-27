package org.datatransferproject.spi.transfer.types;

public enum FailureReasons {
  DESTINATION_FULL("DESTINATION_FULL");

  private final String string;

  FailureReasons(String string) {
    this.string = string;
  }

  @Override
  public String toString() {
    return string;
  }
}
