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

import static java.lang.String.format;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.types.transfer.errors.ErrorDetail;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/** A {@link IdempotentImportExecutor} that stores known values in memory. */
public class InMemoryIdempotentImportExecutor implements IdempotentImportExecutor {
  private final Map<String, Serializable> knownValues = new HashMap<>();
  private final Map<String, ErrorDetail> errors = new HashMap<>();
  private final Monitor monitor;

  public InMemoryIdempotentImportExecutor(Monitor monitor) {
    this.monitor = monitor;
  }

  @Override
  public <T extends Serializable> T executeAndSwallowExceptions(
      String idempotentId, String itemName, Callable<T> callable) {
    try {
      return executeOrThrowException(idempotentId, itemName, callable);
    } catch (IOException e) {
      // Only catching IOException to allow any RuntimeExceptions in the the catch block of
      // executeOrThrowException bubble up and get noticed.

      // Note all errors are logged in executeOrThrowException so no need to re-log them here.
      return null;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Serializable> T executeOrThrowException(
      String idempotentId, String itemName, Callable<T> callable) throws IOException {
    if (knownValues.containsKey(idempotentId)) {
      monitor.debug(() -> format("Using cached key %s from cache for %s", idempotentId, itemName));
      return (T) knownValues.get(idempotentId);
    }
    try {
      T result = callable.call();
      knownValues.put(idempotentId, result);
      monitor.debug(() -> format("Storing key %s in cache for %s", idempotentId, itemName));
      errors.remove(idempotentId);
      return result;
    } catch (Exception e) {
      ErrorDetail errorDetail =
          ErrorDetail.builder()
              .setId(idempotentId)
              .setTitle(itemName)
              .setException(Throwables.getStackTraceAsString(e))
              .build();
      errors.put(idempotentId, errorDetail);
      monitor.severe(() -> "Problem with importing item: " + errorDetail);
      throw new IOException("Problem executing callable for: " + idempotentId, e);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Serializable> T getCachedValue(String idempotentId) {
    if (!knownValues.containsKey(idempotentId)) {
      throw new IllegalArgumentException(
          idempotentId
              + " is not a known key, known keys: "
              + Joiner.on(", ").join(knownValues.keySet()));
    }
    return (T) knownValues.get(idempotentId);
  }

  @Override
  public boolean isKeyCached(String idempotentId) {
    return knownValues.containsKey(idempotentId);
  }

  @Override
  public Collection<ErrorDetail> getErrors() {
    return ImmutableList.copyOf(errors.values());
  }

  @Override
  public void setJobId(UUID jobId) {
    // This runs in memory so the job will always be the same. This means we do not use the jobId.
  }
}
