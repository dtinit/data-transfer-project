package org.datatransferproject.spi.transfer.types;

import javax.annotation.Nonnull;

/**
 * These exceptions can be thrown during a copy and should be caught by the JobProcessor which will
 * then add the failure reason to the job.
 */
public abstract class CopyExceptionWithFailureReason extends CopyException {

  public CopyExceptionWithFailureReason(String message, Throwable cause) {
    super(message, cause);
  }

  @Nonnull
  public abstract String getFailureReason();
}
