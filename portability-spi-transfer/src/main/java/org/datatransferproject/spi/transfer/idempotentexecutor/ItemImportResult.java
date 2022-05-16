package org.datatransferproject.spi.transfer.idempotentexecutor;

import com.google.common.base.Preconditions;
import java.io.Serializable;

public class ItemImportResult<T> {
  /**
   * The caching executor will keep it the cache. Nonnull in case of success. Null in case of
   * failure.
   */
  private final T data;

  /** The size of the item we tried to import, successfully or not. Optional (nullable). */
  private final Long bytes;

  private final Status status;

  private final Exception exception;

  private ItemImportResult(T data, Long bytes, Status status, Exception exception) {
    Preconditions.checkArgument(bytes == null || bytes >= 0);
    this.data = data;
    this.bytes = bytes;
    this.status = status;
    this.exception = exception;
  }

  public static <T extends Serializable> ItemImportResult<T> success(T data, Long sizeInBytes) {
    Preconditions.checkNotNull(data);
    return new ItemImportResult<>(data, sizeInBytes, Status.SUCCESS, null);
  }

  public static <T extends Serializable> ItemImportResult<T> error(
      Exception exception, Long sizeInBytes) {
    return new ItemImportResult<>(null, sizeInBytes, Status.ERROR, exception);
  }

  public T getData() {
    Preconditions.checkState(status == Status.SUCCESS, "Failed import can't contain data");
    return data;
  }

  public boolean hasBytes() {
    return bytes != null;
  }

  public long getBytes() {
    Preconditions.checkState(hasBytes(), "Result does not contain size info");
    return bytes;
  }

  public Status getStatus() {
    return status;
  }

  public Exception getException() {
    Preconditions.checkState(status == Status.ERROR, "Successful import can't contain throwable");
    return exception;
  }

  public enum Status {
    SUCCESS,
    ERROR
  }
}
