package org.datatransferproject.spi.transfer.idempotentexecutor;

import org.datatransferproject.api.launcher.ExtensionContext;

/**
 * ImMemory Implementation of IdempotentImportExecutor.
 */
public class InMemoryIdempotentImportExecutorExtension
    implements IdempotentImportExecutorExtension {

  IdempotentImportExecutor idempotentImportExecutor;
  IdempotentImportExecutor retryingIdempotentImportExecutor;
  @Override
  public IdempotentImportExecutor getIdempotentImportExecutor(ExtensionContext extensionContext) {
    if (idempotentImportExecutor == null) {
      idempotentImportExecutor = new InMemoryIdempotentImportExecutor(extensionContext.getMonitor());
    }
    return idempotentImportExecutor;
  }

  @Override
  public IdempotentImportExecutor getRetryingIdempotentImportExecutor(ExtensionContext extensionContext){
    if(retryingIdempotentImportExecutor == null) {
      retryingIdempotentImportExecutor = new RetryingInMemoryIdempotentImportExecutor(extensionContext.getMonitor(), extensionContext.getSetting("retryLibrary", null));
    }
    return retryingIdempotentImportExecutor;
  }

  @Override
  public void initialize() {
  }
}