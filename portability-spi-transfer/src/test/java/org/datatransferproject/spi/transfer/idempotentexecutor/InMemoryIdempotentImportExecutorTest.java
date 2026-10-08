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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicInteger;
import org.datatransferproject.api.launcher.Monitor;
import org.junit.jupiter.api.Test;

public class InMemoryIdempotentImportExecutorTest {

  private final InMemoryIdempotentImportExecutor executor =
      new InMemoryIdempotentImportExecutor(new Monitor() {});

  @Test
  public void counts_startAtZero() {
    assertEquals(OptionalLong.of(0), executor.getSuccessCount());
    assertEquals(0, executor.getErrorCount());
  }

  @Test
  public void success_isCountedOnce_evenWhenCalledAgain() throws Exception {
    AtomicInteger calls = new AtomicInteger();

    executor.executeAndSwallowIOExceptions("id", "name", calls::incrementAndGet);
    executor.executeAndSwallowIOExceptions("id", "name", calls::incrementAndGet);

    assertEquals(1, calls.get());
    assertEquals(OptionalLong.of(1), executor.getSuccessCount());
    assertEquals(0, executor.getErrorCount());
  }

  @Test
  public void nullResult_countsAsSuccess() throws Exception {
    executor.executeAndSwallowIOExceptions("id", "name", () -> null);

    assertEquals(OptionalLong.of(1), executor.getSuccessCount());
  }

  @Test
  public void swallowedFailure_countsAsError() throws Exception {
    executor.executeAndSwallowIOExceptions(
        "id",
        "name",
        () -> {
          throw new IOException("Test IO exception");
        });

    assertEquals(OptionalLong.of(0), executor.getSuccessCount());
    assertEquals(1, executor.getErrorCount());
  }

  @Test
  public void thrownFailure_countsAsError() {
    assertThrows(
        IllegalStateException.class,
        () ->
            executor.executeOrThrowException(
                "id",
                "name",
                () -> {
                  throw new IllegalStateException("Test exception");
                }));

    assertEquals(1, executor.getErrorCount());
  }

  @Test
  public void failureThenSuccess_countsOnlyTheSuccess() throws Exception {
    executor.executeAndSwallowIOExceptions(
        "id",
        "name",
        () -> {
          throw new IOException("Test IO exception");
        });
    executor.executeAndSwallowIOExceptions("id", "name", () -> "result");

    assertEquals(OptionalLong.of(1), executor.getSuccessCount());
    assertEquals(0, executor.getErrorCount());
  }

  @Test
  public void repeatedFailure_isCountedOnce() throws Exception {
    for (int attempt = 0; attempt < 3; attempt++) {
      executor.executeAndSwallowIOExceptions(
          "id",
          "name",
          () -> {
            throw new IOException("Test IO exception");
          });
    }

    assertEquals(OptionalLong.of(0), executor.getSuccessCount());
    assertEquals(1, executor.getErrorCount());
  }

  @Test
  public void errorCount_matchesErrors_andIgnoresResetOfRecentErrors() throws Exception {
    executor.executeAndSwallowIOExceptions("ok", "name", () -> "result");
    executor.executeAndSwallowIOExceptions(
        "failed-1",
        "name",
        () -> {
          throw new IOException("Test IO exception");
        });
    executor.executeAndSwallowIOExceptions(
        "failed-2",
        "name",
        () -> {
          throw new IOException("Test IO exception");
        });

    executor.resetRecentErrors();

    assertEquals(executor.getErrors().size(), executor.getErrorCount());
    assertEquals(2, executor.getErrorCount());
    assertEquals(OptionalLong.of(1), executor.getSuccessCount());
  }
}
