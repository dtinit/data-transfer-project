package org.datatransferproject.spi.transfer.types;

import javax.annotation.Nonnull;

public class UnconfirmedUserException extends CopyExceptionWithFailureReason {

  public UnconfirmedUserException(String message, Throwable cause) {
    super(message, cause);
  }

  @Nonnull
  @Override
  public String getFailureReason() {
    return FailureReasons.UNCONFIRMED_USER.toString();
  }
}
