package org.datatransferproject.spi.transfer.idempotentexecutor;

import com.google.common.collect.ImmutableList;
import java.util.ServiceLoader;
import org.datatransferproject.api.launcher.ExtensionContext;

public class IdempotentExecutorLoader {

  public static IdempotentImportExecutor load(ExtensionContext extensionContext) {
    ImmutableList.Builder<IdempotentExecutor> builder = ImmutableList.builder();
    ServiceLoader.load(IdempotentExecutor.class)
        .iterator()
        .forEachRemaining(builder::add);
    ImmutableList<IdempotentExecutor> executors = builder.build();
    if (executors.isEmpty()) {
      return new InMemoryIdempotentImportExecutor(extensionContext.getMonitor());
    } else if (executors.size() == 1) {
      IdempotentExecutor extension = executors.get(0);
      extension.initialize();
      return extension.getIdempotentImportExecutor(extensionContext);
    } else {
      throw new IllegalStateException("Cannot load multiple IdempotentImportExecutors");
    }
  }

  private IdempotentExecutorLoader() {
  }
}
