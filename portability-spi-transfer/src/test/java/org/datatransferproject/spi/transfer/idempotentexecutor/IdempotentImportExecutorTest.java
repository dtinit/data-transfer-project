/*
 * Copyright 2026 The Data Transfer Project Authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableList;
import java.io.Serializable;
import java.util.Collection;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.datatransferproject.types.transfer.errors.ErrorDetail;
import org.junit.jupiter.api.Test;

/** Tests the defaults that executors written before the count methods existed get. */
public class IdempotentImportExecutorTest {

  @Test
  public void getSuccessCount_defaultsToEmpty() {
    IdempotentImportExecutor legacyExecutor = new LegacyExecutor(ImmutableList.of());

    assertEquals(OptionalLong.empty(), legacyExecutor.getSuccessCount());
  }

  @Test
  public void getErrorCount_defaultsToNumberOfErrors() {
    ErrorDetail error =
        ErrorDetail.builder().setId("id").setTitle("name").setException("exception").build();
    IdempotentImportExecutor legacyExecutor = new LegacyExecutor(ImmutableList.of(error, error));

    assertEquals(2, legacyExecutor.getErrorCount());
  }

  /** Implements only the abstract methods, like executors written before the count methods. */
  private static class LegacyExecutor implements IdempotentImportExecutor {
    private final Collection<ErrorDetail> errors;

    LegacyExecutor(Collection<ErrorDetail> errors) {
      this.errors = errors;
    }

    @Override
    public <T extends Serializable> T executeAndSwallowIOExceptions(
        String idempotentId, String itemName, Callable<T> callable) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Serializable> T executeOrThrowException(
        String idempotentId, String itemName, Callable<T> callable) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Serializable> T getCachedValue(String idempotentId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isKeyCached(String idempotentId) {
      return false;
    }

    @Override
    public Collection<ErrorDetail> getErrors() {
      return errors;
    }

    @Override
    public void setJobId(UUID jobId) {}
  }
}
