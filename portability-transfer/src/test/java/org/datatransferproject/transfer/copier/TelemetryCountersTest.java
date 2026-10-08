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

package org.datatransferproject.transfer.copier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.idempotentexecutor.InMemoryIdempotentImportExecutor;
import org.datatransferproject.spi.transfer.idempotentexecutor.RetryingInMemoryIdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.ImportResult.ResultType;
import org.datatransferproject.types.common.models.DataModel;
import org.datatransferproject.types.transfer.retry.RetryStrategyLibrary;
import org.datatransferproject.types.transfer.retry.SkipRetryStrategy;
import org.junit.jupiter.api.Test;

public class TelemetryCountersTest {

  @Test
  public void newCounters_startAtZero_withListedAndBytesUnreported() {
    TelemetryCounters counters =
        new TelemetryCounters(new InMemoryIdempotentImportExecutor(monitor()));

    assertNull(counters.itemsListed());
    assertEquals(0, counters.itemsFetched());
    assertEquals(0, counters.itemsFailedToFetch());
    assertNull(counters.bytesReceived());
  }

  @Test
  public void itemsListed_sumsTheCountsOfEveryExportedPage() {
    TelemetryCounters counters = new TelemetryCounters();

    counters.recordExportedPage(page(ImmutableMap.of("photos", 3, "albums", 1)));
    counters.recordExportedPage(page(ImmutableMap.of("photos", 2)));

    assertEquals(Long.valueOf(6), counters.itemsListed());
  }

  @Test
  public void itemsListed_staysUnreported_untilAPageReportsCounts() {
    TelemetryCounters counters = new TelemetryCounters();

    counters.recordExportedPage(null);
    // DataModel.getCounts() returns null unless the model reports counts.
    counters.recordExportedPage(page(null));
    assertNull(counters.itemsListed());

    counters.recordExportedPage(page(ImmutableMap.of()));
    assertEquals(Long.valueOf(0), counters.itemsListed());
  }

  @Test
  public void itemsListed_ignoresNullCountValues() {
    TelemetryCounters counters = new TelemetryCounters();
    Map<String, Integer> countsWithNull = new HashMap<>();
    countsWithNull.put("photos", null);
    countsWithNull.put("videos", 2);

    counters.recordExportedPage(page(countsWithNull));

    assertEquals(Long.valueOf(2), counters.itemsListed());
  }

  @Test
  public void recordExportedPage_neverThrows() {
    TelemetryCounters counters = new TelemetryCounters();
    DataModel brokenPage =
        new DataModel() {
          @Override
          public Map<String, Integer> getCounts() {
            throw new IllegalStateException("broken page");
          }
        };

    counters.recordExportedPage(brokenPage);

    assertNull(counters.itemsListed());
  }

  @Test
  public void bytesReceived_sumsTheBytesImportersReport() {
    TelemetryCounters counters = new TelemetryCounters();

    counters.recordImportedPage(ImportResult.OK.copyWithBytes(100L));
    counters.recordImportedPage(ImportResult.OK);
    counters.recordImportedPage(ImportResult.OK.copyWithBytes(23L));

    assertEquals(Long.valueOf(123), counters.bytesReceived());
  }

  @Test
  public void bytesReceived_reportsZeroBytes() {
    TelemetryCounters counters = new TelemetryCounters();

    counters.recordImportedPage(ImportResult.OK.copyWithBytes(0L));

    assertEquals(Long.valueOf(0), counters.bytesReceived());
  }

  @Test
  public void recordImportedPage_neverThrows() {
    TelemetryCounters counters = new TelemetryCounters();

    counters.recordImportedPage(null);
    counters.recordImportedPage(new ImportResult(ResultType.OK, null, null, null));

    assertEquals(0, counters.itemsFetched());
    assertNull(counters.bytesReceived());
  }

  @Test
  public void itemsFetched_sumsExecutorSuccesses_insteadOfPageCounts() throws Exception {
    InMemoryIdempotentImportExecutor executor = new InMemoryIdempotentImportExecutor(monitor());
    RetryingInMemoryIdempotentImportExecutor retryingExecutor = retryingExecutor();
    TelemetryCounters counters = new TelemetryCounters(executor, retryingExecutor);

    // The page lists 4 items, but one of them fails in the retrying executor.
    succeed(executor, "album");
    succeed(retryingExecutor, "photo-1");
    succeed(retryingExecutor, "photo-2");
    fail(retryingExecutor, "photo-3");
    counters.recordImportedPage(
        ImportResult.OK.copyWithCounts(ImmutableMap.of("albums", 1, "photos", 3)));

    assertEquals(3, counters.itemsFetched());
    assertEquals(1, counters.itemsFailedToFetch());
  }

  @Test
  public void itemsFetched_usesPageCounts_whenAnExecutorDoesNotCountSuccesses() {
    IdempotentImportExecutor countingExecutor = mock(IdempotentImportExecutor.class);
    when(countingExecutor.getSuccessCount()).thenReturn(OptionalLong.of(100));
    IdempotentImportExecutor legacyExecutor = mock(IdempotentImportExecutor.class);
    when(legacyExecutor.getSuccessCount()).thenReturn(OptionalLong.empty());
    TelemetryCounters counters = new TelemetryCounters(countingExecutor, legacyExecutor);

    counters.recordImportedPage(
        ImportResult.OK.copyWithCounts(ImmutableMap.of("albums", 1, "photos", 2)));
    counters.recordImportedPage(ImportResult.OK.copyWithCounts(ImmutableMap.of("photos", 4)));

    assertEquals(7, counters.itemsFetched());
  }

  @Test
  public void itemsFetched_usesPageCounts_whenThereAreNoExecutors() {
    TelemetryCounters counters = new TelemetryCounters(null, null);

    counters.recordImportedPage(ImportResult.OK.copyWithCounts(ImmutableMap.of("photos", 5)));

    assertEquals(5, counters.itemsFetched());
    assertEquals(0, counters.itemsFailedToFetch());
  }

  @Test
  public void itemsFailedToFetch_sumsExecutorErrors() {
    IdempotentImportExecutor executor = mock(IdempotentImportExecutor.class);
    when(executor.getErrorCount()).thenReturn(2L);
    IdempotentImportExecutor retryingExecutor = mock(IdempotentImportExecutor.class);
    when(retryingExecutor.getErrorCount()).thenReturn(3L);

    assertEquals(5, new TelemetryCounters(executor, retryingExecutor).itemsFailedToFetch());
  }

  @Test
  public void sameExecutorPassedTwice_isCountedOnce() throws Exception {
    InMemoryIdempotentImportExecutor executor = new InMemoryIdempotentImportExecutor(monitor());
    succeed(executor, "photo-1");
    succeed(executor, "photo-2");
    fail(executor, "photo-3");

    TelemetryCounters counters = new TelemetryCounters(executor, executor);

    assertEquals(2, counters.itemsFetched());
    assertEquals(1, counters.itemsFailedToFetch());
  }

  private static DataModel page(Map<String, Integer> counts) {
    return new DataModel() {
      @Override
      public Map<String, Integer> getCounts() {
        return counts;
      }
    };
  }

  private static void succeed(IdempotentImportExecutor executor, String id) throws Exception {
    executor.executeAndSwallowIOExceptions(id, id, () -> "result");
  }

  private static void fail(IdempotentImportExecutor executor, String id) throws Exception {
    executor.executeAndSwallowIOExceptions(
        id,
        id,
        () -> {
          throw new IOException("Test IO exception");
        });
  }

  private static RetryingInMemoryIdempotentImportExecutor retryingExecutor() {
    return new RetryingInMemoryIdempotentImportExecutor(
        monitor(), new RetryStrategyLibrary(ImmutableList.of(), new SkipRetryStrategy()));
  }

  private static Monitor monitor() {
    return new Monitor() {};
  }
}
