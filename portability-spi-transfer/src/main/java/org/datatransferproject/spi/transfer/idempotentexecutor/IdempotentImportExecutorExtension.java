package org.datatransferproject.spi.transfer.idempotentexecutor;

import java.io.IOException;
import org.datatransferproject.api.launcher.BootExtension;
import org.datatransferproject.api.launcher.Monitor;

public interface IdempotentImportExecutorExtension extends BootExtension {
  IdempotentImportExecutor getIdempotentImportExecutor(Monitor monitor) throws IOException;
}
