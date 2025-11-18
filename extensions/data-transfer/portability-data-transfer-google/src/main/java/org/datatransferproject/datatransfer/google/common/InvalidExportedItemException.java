package org.datatransferproject.datatransfer.google.common;

// An exception to throw when converting a photo/video/mediaitem into a shared data class fails,
// or any of the data on the exported item is invalid.
public class InvalidExportedItemException extends Exception {
  public InvalidExportedItemException(String message) {
    super(message);
  }

  public InvalidExportedItemException(String message, Throwable cause) {
    super(message, cause);
  }
}
