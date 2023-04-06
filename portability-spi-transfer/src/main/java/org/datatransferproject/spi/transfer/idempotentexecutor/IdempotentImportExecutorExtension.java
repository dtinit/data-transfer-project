package org.datatransferproject.spi.transfer.idempotentexecutor;

import org.datatransferproject.api.launcher.BootExtension;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;

public interface IdempotentImportExecutorExtension extends BootExtension {
  IdempotentImportExecutor getIdempotentImportExecutor(ExtensionContext extensionContext);
  IdempotentImportExecutor getRetryingIdempotentImportExecutor(ExtensionContext extensionContext);
}
