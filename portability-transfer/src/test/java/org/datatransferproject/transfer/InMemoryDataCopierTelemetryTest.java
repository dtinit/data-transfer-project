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

package org.datatransferproject.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;
import javax.annotation.Nullable;
import org.datatransferproject.api.launcher.DtpInternalMetricRecorder;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.copier.stack.PortabilityStackInMemoryDataCopier;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.idempotentexecutor.InMemoryIdempotentImportExecutor;
import org.datatransferproject.spi.transfer.idempotentexecutor.RetryingInMemoryIdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.transfer.copier.InMemoryDataCopier;
import org.datatransferproject.transfer.copier.PortabilityAbstractInMemoryDataCopier;
import org.datatransferproject.transfer.copier.PortabilityInMemoryDataCopier;
import org.datatransferproject.transfer.copier.TelemetryCounters;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.DataModel;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.errors.ErrorDetail;
import org.datatransferproject.types.transfer.retry.NoRetryStrategy;
import org.datatransferproject.types.transfer.retry.RetryStrategy;
import org.datatransferproject.types.transfer.retry.RetryStrategyLibrary;
import org.datatransferproject.types.transfer.retry.SkipRetryStrategy;
import org.datatransferproject.types.transfer.retry.UniformRetryStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests that both copiers keep {@link TelemetryCounters} through the real export and import path.
 * It lives in this package so it can set up {@link JobMetadata}.
 */
public class InMemoryDataCopierTelemetryTest {

  private static final Monitor MONITOR = new Monitor() {};

  private final UUID jobId = UUID.randomUUID();
  private final AuthData authData = mock(AuthData.class);
  private final JobStore jobStore = mock(JobStore.class);
  private final IdempotentImportExecutor executor = new InMemoryIdempotentImportExecutor(MONITOR);
  private final RetryingInMemoryIdempotentImportExecutor retryingExecutor =
      new RetryingInMemoryIdempotentImportExecutor(
          MONITOR, new RetryStrategyLibrary(ImmutableList.of(), new SkipRetryStrategy()));

  @BeforeEach
  public void setUp() {
    JobMetadata.reset();
    JobMetadata.init(
        jobId, new byte[0], DataVertical.PHOTOS, "export", "import", Stopwatch.createUnstarted());
    PortabilityJob job = mock(PortabilityJob.class);
    when(jobStore.findJob(jobId)).thenReturn(job);
  }

  @AfterEach
  public void tearDown() {
    JobMetadata.reset();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void countsEveryPage(boolean stackCopier) throws Exception {
    InMemoryDataCopier copier =
        newCopier(
            stackCopier,
            new FakeExporter(
                new FakePage(30L, "album", "photo-1", "photo-2"), new FakePage(10L, "photo-3")),
            new FakeImporter(/* ownExecutor= */ null));

    copier.copy(authData, authData, jobId, Optional.empty());

    TelemetryCounters counters = copier.getTelemetryCounters().orElseThrow();
    assertEquals(Long.valueOf(4), counters.itemsListed());
    assertEquals(4, counters.itemsFetched());
    assertEquals(0, counters.itemsFailedToFetch());
    assertEquals(Long.valueOf(40), counters.bytesReceived());
  }

  @Test
  public void countsItemsAnImporterRecordsInTheRetryingExecutor() throws Exception {
    // Like an importer that uses the retrying executor it got at startup instead of the executor
    // passed to it. A skipped failure there doesn't fail the page, so the page counts include it.
    InMemoryDataCopier copier =
        newCopier(
            false,
            new FakeExporter(new FakePage(20L, "photo-1", "photo-2", "bad-photo-3")),
            new FakeImporter(retryingExecutor));

    copier.copy(authData, authData, jobId, Optional.empty());

    TelemetryCounters counters = copier.getTelemetryCounters().orElseThrow();
    assertEquals(Long.valueOf(3), counters.itemsListed());
    assertEquals(2, counters.itemsFetched());
    assertEquals(1, counters.itemsFailedToFetch());
  }

  @Test
  public void keepsCounting_whenTheJobStoreCannotSaveCounts() throws Exception {
    doThrow(new IOException("Test IO exception")).when(jobStore).addCounts(any(), any());
    doThrow(new IOException("Test IO exception")).when(jobStore).addBytes(any(), any(Long.class));
    InMemoryDataCopier copier =
        newCopier(
            false,
            new FakeExporter(new FakePage(5L, "photo-1"), new FakePage(7L, "photo-2")),
            new FakeImporter(null));

    copier.copy(authData, authData, jobId, Optional.empty());

    TelemetryCounters counters = copier.getTelemetryCounters().orElseThrow();
    assertEquals(2, counters.itemsFetched());
    assertEquals(Long.valueOf(12), counters.bytesReceived());
  }

  @Test
  public void leavesBytesUnreported_whenTheImporterReportsNone() throws Exception {
    InMemoryDataCopier copier =
        newCopier(
            false,
            new FakeExporter(new FakePage(/* bytes= */ null, "photo-1", "photo-2")),
            new FakeImporter(null));

    copier.copy(authData, authData, jobId, Optional.empty());

    TelemetryCounters counters = copier.getTelemetryCounters().orElseThrow();
    assertEquals(Long.valueOf(2), counters.itemsListed());
    assertEquals(2, counters.itemsFetched());
    assertNull(counters.bytesReceived());
  }

  @Test
  public void leavesListedUnreported_whenPagesHaveNoCounts() throws Exception {
    InMemoryDataCopier copier =
        newCopier(
            false,
            new FakeExporter(new UncountedPage("photo-1", "photo-2")),
            new FakeImporter(null));

    copier.copy(authData, authData, jobId, Optional.empty());

    TelemetryCounters counters = copier.getTelemetryCounters().orElseThrow();
    assertNull(counters.itemsListed());
    assertEquals(2, counters.itemsFetched());
  }

  @Test
  public void getTelemetryCounters_isEmptyForCopiersThatDontKeepThem() {
    InMemoryDataCopier customCopier =
        new InMemoryDataCopier() {
          @Override
          public void copy(
              AuthData exportAuthData,
              AuthData importAuthData,
              UUID jobId,
              Optional<ExportInformation> exportInfo) {}

          @Override
          public Collection<ErrorDetail> getErrors(UUID jobId) {
            return ImmutableList.of();
          }
        };

    assertFalse(customCopier.getTelemetryCounters().isPresent());
  }

  @Test
  public void itemFixedWhenThePageIsRetried_isCountedOnce_asASuccess() throws Exception {
    FlakyImporter importer = new FlakyImporter();
    PortabilityAbstractInMemoryDataCopier copier =
        newCopier(
            false,
            new FakeExporter(new FakePage(10L, "photo-1", "flaky-photo-2")),
            importer,
            new UniformRetryStrategy(/* maxAttempts= */ 3, /* intervalMillis= */ 1, "test"));
    TelemetryCounters counters = copier.getTelemetryCounters().orElseThrow();
    importer.failedCount = counters::itemsFailedToFetch;

    copier.copy(authData, authData, jobId, Optional.empty());

    // The first attempt fails "flaky-photo-2", so the copier retries the page. Until the retry
    // runs, the item counts as failed, so a snapshot taken then would include it.
    assertEquals(ImmutableList.of(0L, 1L), importer.failedCountBeforeEachAttempt);
    assertEquals(0, counters.itemsFailedToFetch());
    assertEquals(2, counters.itemsFetched());
  }

  private PortabilityAbstractInMemoryDataCopier newCopier(
      boolean stackCopier,
      Exporter<AuthData, FakePage> exporter,
      Importer<AuthData, FakePage> importer) {
    return newCopier(stackCopier, exporter, importer, new NoRetryStrategy());
  }

  private PortabilityAbstractInMemoryDataCopier newCopier(
      boolean stackCopier,
      Exporter<AuthData, FakePage> exporter,
      Importer<AuthData, FakePage> importer,
      RetryStrategy pageRetryStrategy) {
    DtpInternalMetricRecorder metricRecorder = mock(DtpInternalMetricRecorder.class);
    RetryStrategyLibrary pageRetries =
        new RetryStrategyLibrary(ImmutableList.of(), pageRetryStrategy);
    if (stackCopier) {
      return new PortabilityStackInMemoryDataCopier(
          () -> exporter,
          () -> importer,
          () -> pageRetries,
          MONITOR,
          executor,
          retryingExecutor,
          metricRecorder,
          jobStore);
    }
    return new PortabilityInMemoryDataCopier(
        () -> exporter,
        () -> importer,
        () -> pageRetries,
        MONITOR,
        executor,
        retryingExecutor,
        metricRecorder,
        jobStore);
  }

  /** A page of items, counted under one key. */
  private static class FakePage extends DataModel {
    private final List<String> itemIds;
    @Nullable private final Long bytes;

    FakePage(@Nullable Long bytes, String... itemIds) {
      this.bytes = bytes;
      this.itemIds = ImmutableList.copyOf(itemIds);
    }

    @Override
    public Map<String, Integer> getCounts() {
      return ImmutableMap.of("items", itemIds.size());
    }
  }

  /**
   * A page with no counts, like a data model that doesn't override {@link DataModel#getCounts()}.
   */
  private static class UncountedPage extends FakePage {
    UncountedPage(String... itemIds) {
      super(/* bytes= */ null, itemIds);
    }

    @Override
    public Map<String, Integer> getCounts() {
      return null;
    }
  }

  /** Returns the given pages in order, asking for the next page until the last one. */
  private static class FakeExporter implements Exporter<AuthData, FakePage> {
    private final Iterator<FakePage> pages;

    FakeExporter(FakePage... pages) {
      this.pages = ImmutableList.copyOf(pages).iterator();
    }

    @Override
    public ExportResult<FakePage> export(
        UUID jobId, AuthData authData, Optional<ExportInformation> exportInformation) {
      FakePage page = pages.next();
      if (pages.hasNext()) {
        return new ExportResult<>(
            ResultType.CONTINUE, page, new ContinuationData(new StringPaginationToken("next")));
      }
      return new ExportResult<>(ResultType.END, page);
    }
  }

  /**
   * Imports each item through an executor, failing items whose id starts with "bad". Uses the
   * executor passed to it unless it was given its own.
   */
  private static class FakeImporter implements Importer<AuthData, FakePage> {
    @Nullable private final IdempotentImportExecutor ownExecutor;

    FakeImporter(@Nullable IdempotentImportExecutor ownExecutor) {
      this.ownExecutor = ownExecutor;
    }

    @Override
    public ImportResult importItem(
        UUID jobId, IdempotentImportExecutor passedExecutor, AuthData authData, FakePage page)
        throws Exception {
      IdempotentImportExecutor executor = ownExecutor != null ? ownExecutor : passedExecutor;
      for (String id : page.itemIds) {
        executor.executeAndSwallowIOExceptions(
            id,
            id,
            () -> {
              if (id.startsWith("bad")) {
                throw new IOException("Test IO exception");
              }
              return id;
            });
      }
      return page.bytes == null ? ImportResult.OK : ImportResult.OK.copyWithBytes(page.bytes);
    }
  }

  /**
   * Imports each item through the executor passed to it, failing ids that start with "flaky" on
   * their first attempt only. Records the failed count at the start of each attempt.
   */
  private static class FlakyImporter implements Importer<AuthData, FakePage> {
    private final Set<String> failedOnce = new HashSet<>();
    private final List<Long> failedCountBeforeEachAttempt = new ArrayList<>();
    private LongSupplier failedCount = () -> 0;

    @Override
    public ImportResult importItem(
        UUID jobId, IdempotentImportExecutor executor, AuthData authData, FakePage page)
        throws Exception {
      failedCountBeforeEachAttempt.add(failedCount.getAsLong());
      for (String id : page.itemIds) {
        executor.executeAndSwallowIOExceptions(
            id,
            id,
            () -> {
              if (id.startsWith("flaky") && failedOnce.add(id)) {
                throw new IOException("Test IO exception");
              }
              return id;
            });
      }
      return ImportResult.OK;
    }
  }
}
