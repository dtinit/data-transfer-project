package org.datatransferproject.spi.transfer.types;

import javax.annotation.Nonnull;

public class PermissionDeniedException extends CopyExceptionWithFailureReason {

  public PermissionDeniedException(String message, Throwable cause) {
    super(message, cause);
  }

  @Nonnull
  @Override
  public String getFailureReason() {
    return FailureReasons.PERMISSION_DENIED.toString();
  }
}
