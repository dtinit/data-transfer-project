package org.datatransferproject.datatransfer.backblaze.exception;

import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;

public final class BackblazeCredentialsException extends CopyExceptionWithFailureReason {
  public BackblazeCredentialsException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public String getFailureReason() {
    return "INVALID_MANUAL_CREDENTIALS";
  }
}
