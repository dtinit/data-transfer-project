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

package org.datatransferproject.spi.transfer.idempotentexecutor;

import java.io.Serializable;
import java.util.List;
import javax.annotation.Nullable;
import org.datatransferproject.types.common.ImportableItem;

/** This class provides a more specialized API for imports executions. */
public interface IdempotentImportExecutor extends CachingExecutor {

  /**
   * Execute an import function for a given item. The function throws whatever exception was wrapped
   * in the returning Rusult. If you can, please wrap an exception rather than throw it because an
   * import executor implementation might have some use of the result meta-information.
   */
  @Nullable
  default <T extends ImportableItem, R extends Serializable> R importAndSwallowIOExceptions(
      T item, ImportFunction<T, R> function) throws Exception {
    return executeAndSwallowIOExceptions(
        item.getIdempotentId(),
        item.getName(),
        () -> {
          // apply can throw, it's ok
          ItemImportResult<R> r = function.apply(item);
          if (r.getStatus() == ItemImportResult.Status.SUCCESS) {
            return r.getData();
          } else {
            throw r.getException();
          }
        });
  }

  default <T extends ImportableItem, R extends Serializable>
      List<R> importBatchAndSwallowIOExceptions(
          List<T> items, ImportFunction<List<T>, List<R>> function) {

    // The general idea is:
    //   1. go through the items, filter out isKeyCached(item)
    //   2. pass the remaining ones to function
    //   3. save the resulting objects to cache, one by one
    //   4. return the result that function returned

    throw new UnsupportedOperationException();
  }
}
