package org.datatransferproject.spi.transfer.types;

import javax.annotation.Nonnull;

public class UpstreamApiUnexpectedResponseException extends CopyExceptionWithFailureReason {

  public UpstreamApiUnexpectedResponseException(String message, Throwable cause) {
    super(message, cause);
  }

  @Nonnull
  @Override
  public String getFailureReason() {
    return FailureReasons.UPSTREAM_API_UNEXPECTED_RESPONSE_EXCEPTION.toString();
  }
}
