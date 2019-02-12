package org.datatransferproject.transfer;

import java.util.UUID;

public class CredsTimeoutException extends RuntimeException {
  private UUID jobId;

  public CredsTimeoutException(String message, UUID jobId) {
    super(message);
    this.jobId = jobId;
  }

  public UUID getJobId() {
    return jobId;
  }
}
