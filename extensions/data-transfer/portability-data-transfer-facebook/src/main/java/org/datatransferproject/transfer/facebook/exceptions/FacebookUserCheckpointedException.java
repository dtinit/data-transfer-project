package org.datatransferproject.transfer.facebook.exceptions;

import javax.annotation.Nonnull;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;

public class FacebookUserCheckpointedException extends CopyExceptionWithFailureReason {
  public FacebookUserCheckpointedException(String message, Throwable cause) {
    super(message, cause);
  }

  @Nonnull
  @Override
  public String getFailureReason() {
    return "USER_CHECKPOINTED";
  }
}
