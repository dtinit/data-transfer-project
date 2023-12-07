package org.datatransferproject.datatransfer.google.common;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.UUID;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.types.transfer.errors.ErrorDetail;

/**
 * Helper class that provides functionality for creating errors & logging them to data store.
 * This class logs errors that should appear as failures in the completion email.
 */
public class GoogleErrorLogger {
  public static void logFailedItemErrors(JobStore jobStore, UUID jobId, ImmutableList<ErrorDetail> errorDetails) throws IOException {
    jobStore.addErrorsToJob(jobId, errorDetails);
  }

  /**
   * @param idempotentId Must be an idempotent identifier for the failed item.
   * @param title Title of the failed item
   * @param e Exception thrown that caused the failure.
   * @param canSkip Based on if we're failing the transfer based on this exception or not.
   * @return an ErrorDetail object with the passed in values.
   */
  public static ErrorDetail createErrorDetail(String idempotentId, String title, Exception e, boolean canSkip) {
    return ErrorDetail.builder()
        .setId(idempotentId)
        .setTitle(title)
        .setException(Throwables.getStackTraceAsString(e))
        .setCanSkip(canSkip).build();
  }
}
