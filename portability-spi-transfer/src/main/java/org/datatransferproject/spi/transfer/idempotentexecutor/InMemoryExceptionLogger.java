package org.datatransferproject.spi.transfer.idempotentexecutor;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.datatransferproject.types.transfer.errors.ErrorDetail;

/**
 * This class pulls out the error logging logic from the ImportExecutors and allows us to
 * use a public API on the Executors to log errors.
 */
abstract class InMemoryExceptionLogger {
  // Holds the collection of errors that were run into during the entire transfer
  private Map<String, ErrorDetail> errors;
  // Holds only the errors in the current export / import, and is reset every new import cycle.
  // portability-transfer/src/main/java/org/datatransferproject/transfer/CallableImporter.java#L66
  private Map<String, ErrorDetail> recentErrors;

  private void logError(String idempotentId, ErrorDetail errorDetail) {
    this.errors.put(idempotentId, errorDetail);
    this.recentErrors.put(idempotentId, errorDetail);
  }

  public Collection<ErrorDetail> getErrors() {
    return ImmutableList.copyOf(errors.values());
  }

  public Collection<ErrorDetail> getRecentErrors() {
    return ImmutableList.copyOf(recentErrors.values());
  }

  public ErrorDetail addError(
      String idempotentId,
      String itemName,
      Exception encounteredException,
      boolean canSkip) {
    ErrorDetail.Builder errorDetailBuilder = ErrorDetail.builder()
        .setId(idempotentId)
        .setTitle(itemName)
        .setException(Throwables.getStackTraceAsString(encounteredException));
    if (canSkip) {
      errorDetailBuilder.setCanSkip(true);
    }
    ErrorDetail errorDetail = errorDetailBuilder.build();

    logError(
        idempotentId,
        errorDetail
    );

    return errorDetail;
  }
}
