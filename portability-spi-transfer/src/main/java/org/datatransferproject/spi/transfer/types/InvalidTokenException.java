package org.datatransferproject.spi.transfer.types;

import javax.annotation.Nonnull;

public class InvalidTokenException extends CopyExceptionWithFailureReason {

  public InvalidTokenException(String message, Throwable cause) {
    super(message, cause);
  }

  @Nonnull
  @Override
  public String getFailureReason() {
    return FailureReasons.INVALID_TOKEN.toString();
  }
}
