package org.datatransferproject.spi.transfer.idempotentexecutor;

import java.util.UUID;

public interface InMemoryExceptionHandler {
  void addError(UUID jobId, String idempotentId, String itemName,
      Exception exceptionEncountered, boolean canSkip);
}
