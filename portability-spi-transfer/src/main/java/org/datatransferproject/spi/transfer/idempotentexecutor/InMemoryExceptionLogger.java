package org.datatransferproject.spi.transfer.idempotentexecutor;

import com.google.common.base.Throwables;
import java.util.Map;
import java.util.UUID;
import org.datatransferproject.types.transfer.errors.ErrorDetail;

abstract class InMemoryExceptionLogger {
  private Map<String, ErrorDetail> errors;
  private Map<String, ErrorDetail> recentErrors;

  private void logError(String idempotentId, ErrorDetail errorDetail) {
    this.errors.put(idempotentId, errorDetail);
    this.recentErrors.put(idempotentId, errorDetail);
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
