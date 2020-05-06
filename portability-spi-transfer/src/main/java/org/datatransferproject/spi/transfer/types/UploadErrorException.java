package org.datatransferproject.spi.transfer.types;

import javax.annotation.Nonnull;

public class UploadErrorException extends CopyExceptionWithFailureReason {

  public UploadErrorException(String message, Throwable cause) {
    super(message, cause);
  }

  @Nonnull
  @Override
  public String getFailureReason() {
    return FailureReasons.UPLOAD_ERROR.toString();
  }
}
