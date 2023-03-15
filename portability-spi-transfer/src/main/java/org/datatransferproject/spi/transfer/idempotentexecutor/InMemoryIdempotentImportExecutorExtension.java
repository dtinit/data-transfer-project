package org.datatransferproject.spi.transfer.idempotentexecutor;

import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;

/**
 * ImMemory Implementation of IdempotentImportExecutor.
 */
public class InMemoryIdempotentImportExecutorExtension
    implements IdempotentImportExecutorExtension {

  @Override
  public IdempotentImportExecutor getIdempotentImportExecutor(ExtensionContext extensionContext) {
    return new InMemoryIdempotentImportExecutor(extensionContext.getMonitor());
  }

  @Override
  public void initialize() {
  }
}