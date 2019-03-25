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

package org.datatransferproject.transfer;

import com.google.common.base.Joiner;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.provider.IdempotentImportExecutor;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * A {@link IdempotentImportExecutor} that stores known values in memory.
 */
public class InMemoryIdempotentImportExecutor implements IdempotentImportExecutor {
  final private Map<String, Serializable> knownValues = new HashMap<>();
  private final Monitor monitor;

  public InMemoryIdempotentImportExecutor(Monitor monitor) {
    this.monitor = monitor;
  }

  @Override
  public <T extends Serializable> T execute(
      String idempotentId,
      String itemName,
      Callable<T> callable) throws IOException {
    if (knownValues.containsKey(idempotentId)) {
      monitor.debug(() -> "Using cached key {} from cache for {}", idempotentId, itemName);
      return (T) knownValues.get(idempotentId);
    }
    try {
      T result = callable.call();
      knownValues.put(idempotentId, result);
      monitor.debug(() -> "Storing key {} in cache for {}", idempotentId, itemName);
      return result;
    } catch (Exception e) {
      throw new IOException("Problem executing callable for: " + idempotentId, e);
    }
  }

  @Override
  public <T extends Serializable> T getCachedValue(String idempotentId) {
    if (!knownValues.containsKey(idempotentId)) {
      throw new IllegalArgumentException(
          idempotentId + " is not a known key, known keys: "
              + Joiner.on(", ").join(knownValues.keySet()));
    }
    return (T) knownValues.get(idempotentId);
  }

  @Override
  public boolean isKeyCached(String idempotentId) {
    return knownValues.containsKey(idempotentId);
  }
}
