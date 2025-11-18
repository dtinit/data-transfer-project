package org.datatransferproject.spi.transfer.types;

import javax.annotation.Nonnull;

/**
 * Exception for a single known category of failure-reason, thrown during a copy that should be
 * caught by the JobProcessor (to then add the failure reason to the job).
 *
 * <p>{@link FailureReasons} is intended to assist in keeping implementations of this class
 * accessible.
 *
 * <p>Note if an exception case is needed to express multiple classes of distinct failuiure cases,
 * then deriving from {@link CopyException} instead is probably better.
 */
public abstract class CopyExceptionWithFailureReason extends CopyException {

  public CopyExceptionWithFailureReason(String message, Throwable cause) {
    super(message, cause);
  }

  @Nonnull
  public abstract String getFailureReason();
}
