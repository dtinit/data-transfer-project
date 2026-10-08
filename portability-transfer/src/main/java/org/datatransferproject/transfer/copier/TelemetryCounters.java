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

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.types.common.models.DataModel;

/**
 * Counts for the current run of a transfer job, kept for transfer telemetry.
 *
 * <p>The copier records each page as it's exported and imported, and another thread can read the
 * counts at any time. Counts live in memory only. They're never read from or written to the
 * {@code JobStore}, so they start at 0 with each worker run.
 *
 * <p>Recording never throws, so counting can't fail a transfer.
 */
public final class TelemetryCounters {

  private final ImmutableList<IdempotentImportExecutor> executors;
  private final AtomicLong itemsListed = new AtomicLong();
  private volatile boolean listedReported;
  private final AtomicLong itemsInImportedPages = new AtomicLong();
  private final AtomicLong bytesReceived = new AtomicLong();
  private volatile boolean bytesReported;

  /**
   * Creates counters that read item results from the given executors.
   *
   * @param executors every executor importers record items in. Null executors are ignored, and an
   *     executor passed twice is counted once.
   */
  TelemetryCounters(IdempotentImportExecutor... executors) {
    List<IdempotentImportExecutor> distinct = new ArrayList<>();
    for (IdempotentImportExecutor executor : executors) {
      // Compare by identity: both executor bindings may return the same instance.
      if (executor != null && distinct.stream().noneMatch(e -> e == executor)) {
        distinct.add(executor);
      }
    }
    this.executors = ImmutableList.copyOf(distinct);
  }

  /** Records a page the exporter returned. */
  void recordExportedPage(@Nullable DataModel exportedData) {
    try {
      if (exportedData == null) {
        return;
      }
      // Null unless the data model reports counts.
      Map<String, Integer> counts = exportedData.getCounts();
      if (counts != null) {
        itemsListed.addAndGet(sum(counts));
        listedReported = true;
      }
    } catch (RuntimeException e) {
      // Counting is best effort and must never fail a transfer.
    }
  }

  /** Records a page the importer finished successfully. */
  void recordImportedPage(@Nullable ImportResult importResult) {
    try {
      if (importResult == null) {
        return;
      }
      Optional<Map<String, Integer>> counts = importResult.getCounts();
      if (counts.isPresent()) {
        itemsInImportedPages.addAndGet(sum(counts.get()));
      }
      Optional<Long> bytes = importResult.getBytes();
      if (bytes.isPresent()) {
        bytesReceived.addAndGet(bytes.get());
        bytesReported = true;
      }
    } catch (RuntimeException e) {
      // Counting is best effort and must never fail a transfer.
    }
  }

  /**
   * Returns the number of items in every page exported so far, from their counts, or null if no
   * exported page has reported counts.
   */
  @Nullable
  public Long itemsListed() {
    return listedReported ? itemsListed.get() : null;
  }

  /**
   * Returns the number of items imported so far.
   *
   * <p>If every executor reports {@link IdempotentImportExecutor#getSuccessCount()}, this is the
   * sum of those counts. Executors count every id they run, so this includes containers such as
   * albums, and any lookups an exporter runs through the same executor. Otherwise it's the number
   * of items in every page that imported successfully.
   */
  public long itemsFetched() {
    if (executors.isEmpty()) {
      return itemsInImportedPages.get();
    }
    long total = 0;
    for (IdempotentImportExecutor executor : executors) {
      OptionalLong successCount = executor.getSuccessCount();
      if (!successCount.isPresent()) {
        return itemsInImportedPages.get();
      }
      total += successCount.getAsLong();
    }
    return total;
  }

  /**
   * Returns the number of items that failed to import and didn't later succeed, from {@link
   * IdempotentImportExecutor#getErrorCount()} summed over the executors.
   */
  public long itemsFailedToFetch() {
    long total = 0;
    for (IdempotentImportExecutor executor : executors) {
      total += executor.getErrorCount();
    }
    return total;
  }

  /**
   * Returns the bytes imported so far, or null if no importer has reported bytes. Only pages that
   * import successfully add bytes.
   */
  @Nullable
  public Long bytesReceived() {
    return bytesReported ? bytesReceived.get() : null;
  }

  private static long sum(Map<String, Integer> counts) {
    long total = 0;
    for (Integer count : counts.values()) {
      if (count != null) {
        total += count;
      }
    }
    return total;
  }
}
