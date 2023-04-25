package org.datatransferproject.spi.transfer.idempotentexecutor;

import com.google.inject.Provider;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.types.transfer.retry.RetryStrategyLibrary;

/**
 * ImMemory Implementation of IdempotentImportExecutor.
 */
public class InMemoryIdempotentImportExecutorExtension
    implements IdempotentImportExecutorExtension {

  private Provider<RetryStrategyLibrary> retryStrategyLibraryProvider;

  @Override
  public IdempotentImportExecutor getIdempotentImportExecutor(ExtensionContext extensionContext) {
    return new InMemoryIdempotentImportExecutor(extensionContext.getMonitor());
  }

  @Override
  public IdempotentImportExecutor getRetryingIdempotentImportExecutor(ExtensionContext extensionContext){
    return new RetryingInMemoryIdempotentImportExecutor(extensionContext.getMonitor(), retryStrategyLibraryProvider);
  }

  @Override
  public void initialize() {
  }
}