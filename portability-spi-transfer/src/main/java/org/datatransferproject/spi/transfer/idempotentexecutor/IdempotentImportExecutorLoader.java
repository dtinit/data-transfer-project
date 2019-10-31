package org.datatransferproject.spi.transfer.idempotentexecutor;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ServiceLoader;
import org.datatransferproject.api.launcher.Monitor;

public class IdempotentImportExecutorLoader {

  public static IdempotentImportExecutor load(Monitor monitor) {
    ImmutableList.Builder<IdempotentImportExecutorExtension> builder = ImmutableList.builder();
    ServiceLoader.load(IdempotentImportExecutorExtension.class)
        .iterator()
        .forEachRemaining(builder::add);
    ImmutableList<IdempotentImportExecutorExtension> executors = builder.build();
    if (executors.isEmpty()) {
      return new InMemoryIdempotentImportExecutor(monitor);
    } else if (executors.size() == 1) {
      IdempotentImportExecutorExtension extension = executors.get(0);
      extension.initialize();
      try {
        return extension.getIdempotentImportExecutor(monitor);
      } catch (IOException e) {
        throw new IllegalStateException("Cannot get IdempotentImportExecutor");
      }
    } else {
      throw new IllegalStateException("Cannot load multiple IdempotentImportExecutors");
    }
  }

  private IdempotentImportExecutorLoader() {
  }
}
