package org.datatransferproject.spi.transfer.types;

import javax.annotation.Nonnull;

public class SessionInvalidatedException extends CopyExceptionWithFailureReason {

  public SessionInvalidatedException(String message, Throwable cause) {
    super(message, cause);
  }

  @Nonnull
  @Override
  public String getFailureReason() {
    return FailureReasons.SESSION_INVALIDATED.toString();
  }
}
