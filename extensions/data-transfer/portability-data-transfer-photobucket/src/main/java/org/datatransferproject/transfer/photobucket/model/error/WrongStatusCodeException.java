package org.datatransferproject.transfer.photobucket.model.error;

public class WrongStatusCodeException extends Exception {
  public WrongStatusCodeException(String message) {
    super(message);
  }
}
