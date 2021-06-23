package org.datatransferproject.transfer.photobucket.model.error;

public class WrongStatusCodeRetriableException extends Exception {
  public WrongStatusCodeRetriableException(String message) {
    super(message);
  }
}
