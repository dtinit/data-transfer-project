package org.datatransferproject.spi.transfer.idempotentexecutor;

import com.google.common.collect.ImmutableList;
import java.util.ServiceLoader;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;

public class IdempotentImportExecutorLoader {

  public static IdempotentImportExecutorExtension load(ExtensionContext extensionContext) {
    ImmutableList.Builder<IdempotentImportExecutorExtension> builder = ImmutableList.builder();
    ServiceLoader.load(IdempotentImportExecutorExtension.class)
        .iterator()
        .forEachRemaining(builder::add);
    ImmutableList<IdempotentImportExecutorExtension> executors = builder.build();
    if (executors.isEmpty()) {
      return new InMemoryIdempotentImportExecutorExtension();
    } else if (executors.size() == 1) {
      IdempotentImportExecutorExtension extension = executors.get(0);
      return extension;
    } else {
      throw new IllegalStateException("Cannot load multiple IdempotentImportExecutors");
    }
  }

  private IdempotentImportExecutorLoader() {
  }
}
