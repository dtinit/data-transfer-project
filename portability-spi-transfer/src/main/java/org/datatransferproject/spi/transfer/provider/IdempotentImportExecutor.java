/*
 * Copyright 2019 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.datatransferproject.spi.transfer.provider;

import org.datatransferproject.types.transfer.errors.ErrorDetail;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * A utility that will execute a {@link Callable} only once for a given {@code idempotentId}. This
 * allows client code to be called multiple times in the case of retries without worrying about
 * duplicating imported data.
 */
public interface IdempotentImportExecutor {
  /**
   * Executes a callable, a callable will only be executed once for a given idempotentId, subsequent
   * calls will return the same result as the first invocation if it was successful.
   *
   * <p>If the provided callable throws an exception if is logged and ignored and null is returned.
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
  <T extends Serializable> T executeAndSwallowExceptions(
      String idempotentId, String itemName, Callable<T> callable);

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
      String idempotentId, String itemName, Callable<T> callable) throws IOException;

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
}
