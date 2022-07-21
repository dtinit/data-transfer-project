package org.datatransferproject.spi.transfer.idempotentexecutor;

import java.io.Serializable;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Callable;
import javax.annotation.Nullable;
import org.datatransferproject.types.transfer.errors.ErrorDetail;

/**
 * A utility that will execute a {@link Callable} only once for a given {@code idempotentId}. This
 * allows client code to be called multiple times in the case of retries without worrying about
 * duplicating imported data.
 *
 * For importing data, please use IdempotentImportExecutor.
 */
public interface CachingExecutor {
  /**
   * Executes a callable, a callable will only be executed once for a given idempotentId, subsequent
   * calls will return the same result as the first invocation if it was successful.
   *
   * <p>If the provided callable throws an IO exception if is logged and ignored and null is
   * returned. All other exceptions are passed through
   *
   * <p>This is useful for leaf level imports where the importer should continue if a single item
   * can't be imported.
   *
   * <p>Any errors (that aren't latter successful) will be reported as failed items.
   *
   * @param idempotentId a unique ID to prevent data from being duplicated
   * @param itemName a user visible/understandable string to be displayed to the user if the item
   *     can't be imported
   * @param callable the callable to execute
   * @return the result of executing the callable.
   */
  @Nullable
  <T extends Serializable> T executeAndSwallowIOExceptions(
      String idempotentId, String itemName, Callable<T> callable) throws Exception;

  /**
   * Executes a callable, a callable will only be executed once for a given idempotentId, subsequent
   * calls will return the same result as the first invocation if it was successful.
   *
   * <p>If the provided callable throws an exception then that is exception is rethrown.
   *
   * <p>This is useful for container level items where the rest of the import can't continue if
   * there is an error.
   *
   * <p>Any errors (that aren't latter successful) will be reported as failed items.
   *
   * @param idempotentId a unique ID to prevent data from being duplicated
   * @param itemName a user visible/understandable string to be displayed to the user if the item
   *     can't be imported
   * @param callable the callable to execute
   * @return the result of executing the callable.
   */
  <T extends Serializable> T executeOrThrowException(
      String idempotentId, String itemName, Callable<T> callable) throws Exception;

  /**
   * Returns a cached result from a previous call to {@code execute}.
   *
   * @param idempotentId a unique ID previously passed into {@code execute}
   * @return the result of a previously evaluated {@code execute} call
   * @throws IllegalArgumentException if the key is not found
   */
  <T extends Serializable> T getCachedValue(String idempotentId) throws IllegalArgumentException;

  /** Checks if a given key has been cached already. */
  boolean isKeyCached(String idempotentId);

  /** Get the set of all errors that occurred, and weren't subsequently successful. */
  Collection<ErrorDetail> getErrors();

  /**
   * Sets the jobId for the executor so that any values can be linked to the job. This can enable
   * resuming the job without creating duplicate values even if a worker has crashed. Some executors
   * may require this to be called before execution.
   *
   * @param jobId The id of the job this executor is being used for.
   */
  void setJobId(UUID jobId);

  /** Get the set of recent errors that occurred, and weren't subsequently successful. */
  default Collection<ErrorDetail> getRecentErrors() {
    return getErrors();
  }

  /** Reset recent errors to empty set */
  default void resetRecentErrors() {}
}
