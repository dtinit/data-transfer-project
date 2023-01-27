package org.datatransferproject.spi.transfer.idempotentexecutor;

@FunctionalInterface
public interface ImportFunction<T, R> {
  ItemImportResult<R> apply(T t) throws Exception;
}
