package org.datatransferproject.spi.transfer.types;

import javax.annotation.Nonnull;

public abstract class CopyExceptionWithFailureReason extends CopyException {

  public CopyExceptionWithFailureReason(String message, Throwable cause) {
    super(message, cause);
  }

  @Nonnull
  public abstract String getFailureReason();
}
