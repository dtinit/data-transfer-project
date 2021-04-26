package org.datatransferproject.spi.transfer.types;

import javax.annotation.Nonnull;

public class UserCheckpointedException extends CopyExceptionWithFailureReason {

  public UserCheckpointedException(String message, Throwable cause) {
    super(message, cause);
  }

  @Nonnull
  @Override
  public String getFailureReason() {
    return FailureReasons.USER_CHECKPOINTED.toString();
  }
}
