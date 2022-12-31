package org.datatransferproject.spi.transfer.idempotentexecutor;

import org.datatransferproject.api.launcher.BootExtension;
import org.datatransferproject.api.launcher.ExtensionContext;

public interface IdempotentExecutor extends BootExtension {
  IdempotentImportExecutor getIdempotentImportExecutor(ExtensionContext extensionContext);
}
