package org.datatransferproject.spi.transfer.idempotentexecutor;

import org.datatransferproject.api.launcher.ExtensionContext;

/**
 * ImMemory Implementation of IdempotentImportExecutor.
 */
public class InMemoryIdempotentImportExecutorExtension implements IdempotentImportExecutorExtension {

    @Override
    public IdempotentImportExecutor getIdempotentImportExecutor(ExtensionContext extensionContext) {
        return new InMemoryIdempotentImportExecutor(extensionContext.getMonitor());
    }

    @Override
    public IdempotentImportExecutor getRetryingIdempotentImportExecutor(ExtensionContext extensionContext) {
        return new RetryingInMemoryIdempotentImportExecutor(extensionContext.getMonitor(), extensionContext.getSetting("retryLibrary", null));
    }

    @Override
    public void initialize() {
    }
}
