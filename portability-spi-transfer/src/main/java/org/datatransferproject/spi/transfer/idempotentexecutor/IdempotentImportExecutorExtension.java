package org.datatransferproject.spi.transfer.idempotentexecutor;

import org.datatransferproject.api.launcher.BootExtension;
import org.datatransferproject.api.launcher.Monitor;

interface IdempotentImportExecutorExtension extends BootExtension {
  IdempotentImportExecutor getIdempotentImportExecutor(Monitor monitor);
}
