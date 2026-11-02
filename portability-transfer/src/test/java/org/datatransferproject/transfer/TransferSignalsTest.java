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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Stopwatch;
import com.google.inject.Provider;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.provider.SignalHandler;
import org.datatransferproject.spi.transfer.provider.SignalRequest;
import org.datatransferproject.spi.transfer.types.signals.EndTelemetry;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle.EndReason;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle.State;
import org.datatransferproject.spi.transfer.types.signals.ProgressTelemetry;
import org.datatransferproject.spi.transfer.types.signals.StartTelemetry;
import org.datatransferproject.transfer.copier.TelemetryCounters;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TransferSignalsTest {

  private static final long NOW = 1_760_000_000_000L;
  private static final Clock CLOCK = Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC);
  private static final Duration LIFETIME = Duration.ofDays(7);
  // Long enough that no Progress is sent during a test, unless the test shortens it.
  private static final Duration NO_PROGRESS = Duration.ofHours(1);
  // How long the cancel path waits for End: long enough for any test, short enough to fail fast.
  private static final Duration CANCEL_WAIT = Duration.ofSeconds(10);
  private static final JobLifeCycle COMPLETED = ended(EndReason.SUCCESSFULLY_COMPLETED);

  private final UUID jobId = UUID.randomUUID();
  private final UUID recurringJobId = UUID.randomUUID();
  private final AuthData exportAuthData = mock(AuthData.class);
  private final AuthData importAuthData = mock(AuthData.class);
  private final SignalHandler exportHandler = mock(SignalHandler.class);
  private final SignalHandler importHandler = mock(SignalHandler.class);
  private final TelemetryCounters counters = mock(TelemetryCounters.class);
  private final Supplier<Optional<TelemetryCounters>> withCounters = () -> Optional.of(counters);
  private final Monitor monitor = mock(Monitor.class);

  @BeforeEach
  void setUp() {
    JobMetadata.reset();
    JobMetadata.init(
        jobId,
        recurringJobId,
        new byte[0],
        DataVertical.PHOTOS,
        "Apple",
        "Google",
        Stopwatch.createUnstarted());
    when(counters.itemsListed()).thenReturn(10L);
    when(counters.itemsFetched()).thenReturn(7L);
    when(counters.bytesReceived()).thenReturn(4096L);
    when(counters.itemsFailedToFetch()).thenReturn(2L);
  }

  @AfterEach
  void tearDown() {
    JobMetadata.reset();
  }

  @Test
  void telemetryOff_everyHandlerGetsTodaysSignals() throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);
    AtomicBoolean countersRead = new AtomicBoolean();
    TransferSignals signals = signals(false, Optional.of(LIFETIME), NO_PROGRESS);

    signals.sendStart(
        jobId,
        exportAuthData,
        importAuthData,
        () -> {
          countersRead.set(true);
          return Optional.of(counters);
        });
    signals.sendEnd(jobId, exportAuthData, importAuthData, COMPLETED);

    List<SignalRequest> expected =
        List.of(todaysRequest(JobLifeCycle.JOB_STARTED()), todaysRequest(COMPLETED));
    assertEquals(expected, requestsTo(exportHandler, exportAuthData));
    assertEquals(expected, requestsTo(importHandler, importAuthData));
    verify(exportHandler, never()).supportsTelemetry();
    verify(importHandler, never()).supportsTelemetry();
    assertFalse(countersRead.get());
  }

  @Test
  void signalsDisabled_noHandlerIsCalled() throws Exception {
    Provider<SignalHandler> exportProvider = providerMock(exportHandler);
    Provider<SignalHandler> importProvider = providerMock(importHandler);
    TransferSignals signals =
        new TransferSignals(
            exportProvider,
            importProvider,
            false,
            true,
            NO_PROGRESS,
            Optional.of(LIFETIME),
            CANCEL_WAIT,
            monitor,
            CLOCK);

    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);
    signals.sendEndOnCancel(EndReason.USER_CANCELLED);
    signals.sendEnd(jobId, exportAuthData, importAuthData, COMPLETED);

    verify(exportProvider, never()).get();
    verify(importProvider, never()).get();
  }

  @Test
  void start_optedInHandlerGetsStartTelemetry_otherHandlerGetsTodaysRequest() throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);

    telemetrySignals().sendStart(jobId, exportAuthData, importAuthData, withCounters);

    assertEquals(
        List.of(todaysRequest(JobLifeCycle.JOB_STARTED())),
        requestsTo(exportHandler, exportAuthData));
    SignalRequest start = onlyRequestTo(importHandler, importAuthData);
    assertEquals(
        todaysRequestBuilder(JobLifeCycle.JOB_STARTED())
            .setStartTelemetry(
                StartTelemetry.builder()
                    .setTimestamp(NOW)
                    .setExpirationTimestamp(NOW + LIFETIME.toMillis())
                    .setRecurringJobId(recurringJobId.toString())
                    .build())
            .build(),
        start);
  }

  @Test
  void end_optedInHandlerGetsTheCountsAndTodaysEndReason() throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);
    JobLifeCycle partial = ended(EndReason.PARTIALLY_COMPLETED);
    TransferSignals signals = telemetrySignals();

    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);
    signals.sendEnd(jobId, exportAuthData, importAuthData, partial);

    assertEquals(
        List.of(todaysRequest(JobLifeCycle.JOB_STARTED()), todaysRequest(partial)),
        requestsTo(exportHandler, exportAuthData));
    SignalRequest end = requestsTo(importHandler, importAuthData).get(1);
    assertEquals(
        todaysRequestBuilder(partial)
            .setEndTelemetry(
                EndTelemetry.builder()
                    .setTimestamp(NOW)
                    .setTotalItemsListed(10L)
                    .setTotalItemsFetched(7L)
                    .setTotalBytesReceived(4096L)
                    .setTotalItemsFailedToFetch(2L)
                    .build())
            .build(),
        end);
  }

  @Test
  void eachHandlerIsAskedOnce_andTheAnswerIsKeptForTheJob() throws Exception {
    when(exportHandler.supportsTelemetry()).thenReturn(false);
    when(importHandler.supportsTelemetry()).thenReturn(true);
    TransferSignals signals = telemetrySignals();

    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);
    signals.sendEnd(jobId, exportAuthData, importAuthData, COMPLETED);

    verify(exportHandler, times(1)).supportsTelemetry();
    verify(importHandler, times(1)).supportsTelemetry();
  }

  @Test
  void bothHandlersOptIn_bothGetTelemetry() throws Exception {
    when(exportHandler.supportsTelemetry()).thenReturn(true);
    when(importHandler.supportsTelemetry()).thenReturn(true);
    TransferSignals signals = telemetrySignals();

    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);
    signals.sendEnd(jobId, exportAuthData, importAuthData, COMPLETED);

    for (SignalHandler handler : List.of(exportHandler, importHandler)) {
      AuthData authData = handler == exportHandler ? exportAuthData : importAuthData;
      List<SignalRequest> requests = requestsTo(handler, authData);
      assertNotNull(requests.get(0).startTelemetry());
      assertNotNull(requests.get(1).endTelemetry());
    }
  }

  @Test
  void progress_goesOnlyToOptedInHandlers_andStopsAtEnd() throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);
    TransferSignals signals = signals(true, Optional.of(LIFETIME), Duration.ofMillis(10));

    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);
    verify(importHandler, timeout(10_000).atLeast(2))
        .sendSignal(withState(State.IN_PROGRESS), any(), any());
    signals.sendEnd(jobId, exportAuthData, importAuthData, COMPLETED);
    List<SignalRequest> atEnd = requestsTo(importHandler, importAuthData);
    Thread.sleep(200);

    assertEquals(atEnd, requestsTo(importHandler, importAuthData));
    assertEquals(COMPLETED, atEnd.get(atEnd.size() - 1).jobStatus());
    SignalRequest progress = atEnd.get(1);
    assertEquals(
        todaysRequestBuilder(JobLifeCycle.IN_PROGRESS())
            .setProgressTelemetry(
                ProgressTelemetry.builder()
                    .setTimestamp(NOW)
                    .setTotalItemsListed(10L)
                    .setTotalItemsFetched(7L)
                    .setTotalBytesReceived(4096L)
                    .setTotalItemsFailedToFetch(2L)
                    .build())
            .build(),
        progress);
    assertEquals(
        List.of(todaysRequest(JobLifeCycle.JOB_STARTED()), todaysRequest(COMPLETED)),
        requestsTo(exportHandler, exportAuthData));
  }

  @Test
  void progress_handlerFailure_isLoggedAtDebug_andDoesNotStopTheOtherHandlerOrLaterProgress()
      throws Exception {
    when(exportHandler.supportsTelemetry()).thenReturn(true);
    when(importHandler.supportsTelemetry()).thenReturn(true);
    IOException failure = new IOException("unreachable");
    doThrow(failure).when(exportHandler).sendSignal(withState(State.IN_PROGRESS), any(), any());
    TransferSignals signals = signals(true, Optional.of(LIFETIME), Duration.ofMillis(10));

    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);

    verify(importHandler, timeout(10_000).atLeast(3))
        .sendSignal(withState(State.IN_PROGRESS), any(), any());
    signals.sendEnd(jobId, exportAuthData, importAuthData, COMPLETED);
    // Each run sends to the export handler first, so it has failed at least 3 times by now.
    verify(monitor, atLeast(3)).debug(any(), same(failure));
    verify(monitor, never()).info(any(), same(failure));
  }

  @Test
  void noWorkerLifetime_telemetryIsStillSent_withoutAnExpiration() throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);
    TransferSignals signals = signals(true, Optional.empty(), NO_PROGRESS);

    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);
    signals.sendEnd(jobId, exportAuthData, importAuthData, COMPLETED);

    List<SignalRequest> requests = requestsTo(importHandler, importAuthData);
    assertEquals(
        StartTelemetry.builder()
            .setTimestamp(NOW)
            .setRecurringJobId(recurringJobId.toString())
            .build(),
        requests.get(0).startTelemetry());
    assertNotNull(requests.get(1).endTelemetry());
  }

  @Test
  void workerLifetimeNotPositive_startHasNoExpiration() throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);
    TransferSignals signals =
        new TransferSignals(
            () -> exportHandler,
            () -> importHandler,
            true,
            true,
            OptionalLong.empty(),
            OptionalLong.of(0),
            monitor);

    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);
    signals.sendEnd(jobId, exportAuthData, importAuthData, COMPLETED);

    StartTelemetry start = requestsTo(importHandler, importAuthData).get(0).startTelemetry();
    assertNotNull(start);
    assertNull(start.expirationTimestamp());
  }

  @Test
  void heartbeatInterval_defaultsTo15Minutes_andIsKeptBetween1And15Minutes() {
    assertEquals(Duration.ofMinutes(15), TransferSignals.heartbeatInterval(OptionalLong.empty()));
    assertEquals(Duration.ofMinutes(15), TransferSignals.heartbeatInterval(OptionalLong.of(0)));
    assertEquals(Duration.ofMinutes(1), TransferSignals.heartbeatInterval(OptionalLong.of(1_000)));
    assertEquals(
        Duration.ofMinutes(5), TransferSignals.heartbeatInterval(OptionalLong.of(300_000)));
    assertEquals(
        Duration.ofMinutes(15), TransferSignals.heartbeatInterval(OptionalLong.of(7_200_000)));
  }

  @Test
  void copierWithoutCounters_sendsNoTelemetry() throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);
    TransferSignals signals = telemetrySignals();

    signals.sendStart(jobId, exportAuthData, importAuthData, Optional::empty);
    signals.sendEnd(jobId, exportAuthData, importAuthData, COMPLETED);

    assertTodaysSignalsOnly();
  }

  @Test
  void defaultSettings_noHandlerOptsIn_sendsTodaysSignals_andLogsNothingNew() throws Exception {
    List<String> logs = new ArrayList<>();
    Monitor recordingMonitor =
        new Monitor() {
          @Override
          public void severe(Supplier<String> message, Object... data) {
            logs.add(message.get());
          }

          @Override
          public void info(Supplier<String> message, Object... data) {
            logs.add(message.get());
          }

          @Override
          public void debug(Supplier<String> message, Object... data) {
            logs.add(message.get());
          }
        };
    AtomicBoolean countersRead = new AtomicBoolean();
    // Telemetry on (the default), and no worker lifetime set.
    TransferSignals signals =
        new TransferSignals(
            () -> exportHandler,
            () -> importHandler,
            true,
            true,
            NO_PROGRESS,
            Optional.empty(),
            CANCEL_WAIT,
            recordingMonitor,
            CLOCK);

    signals.sendStart(
        jobId,
        exportAuthData,
        importAuthData,
        () -> {
          countersRead.set(true);
          return Optional.of(counters);
        });
    signals.sendEnd(jobId, exportAuthData, importAuthData, COMPLETED);

    assertTodaysSignalsOnly();
    assertFalse(countersRead.get());
    assertEquals(List.of(), logs);
  }

  @Test
  void supportsTelemetryThrows_countsAsNo() throws Exception {
    when(exportHandler.supportsTelemetry()).thenReturn(true);
    when(importHandler.supportsTelemetry()).thenThrow(new IllegalStateException("broken"));
    TransferSignals signals = telemetrySignals();

    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);
    signals.sendEnd(jobId, exportAuthData, importAuthData, COMPLETED);

    assertEquals(
        List.of(todaysRequest(JobLifeCycle.JOB_STARTED()), todaysRequest(COMPLETED)),
        requestsTo(importHandler, importAuthData));
    assertNotNull(requestsTo(exportHandler, exportAuthData).get(0).startTelemetry());
  }

  @Test
  void start_checkedExceptionFromFirstHandler_secondHandlerStillGetsStart() throws Exception {
    doThrow(new IOException("unreachable"))
        .when(exportHandler)
        .sendSignal(any(), any(), any());

    signals(false, Optional.empty(), NO_PROGRESS)
        .sendStart(jobId, exportAuthData, importAuthData, withCounters);

    assertEquals(
        List.of(todaysRequest(JobLifeCycle.JOB_STARTED())),
        requestsTo(importHandler, importAuthData));
  }

  @Test
  void start_runtimeExceptionFromFirstHandler_secondHandlerStillGetsStart_thenItIsRethrown()
      throws Exception {
    IllegalStateException failure = new IllegalStateException("handler bug");
    doThrow(failure).when(exportHandler).sendSignal(any(), any(), any());
    TransferSignals signals = signals(false, Optional.empty(), NO_PROGRESS);

    IllegalStateException thrown =
        assertThrows(
            IllegalStateException.class,
            () ->
                signals.sendStart(
                    jobId, exportAuthData, importAuthData, withCounters));

    assertSame(failure, thrown);
    assertEquals(
        List.of(todaysRequest(JobLifeCycle.JOB_STARTED())),
        requestsTo(importHandler, importAuthData));
  }

  @Test
  void start_runtimeExceptionFromAHandler_optedInHandlerStillGetsTelemetryAtEnd()
      throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);
    doThrow(new IllegalStateException("handler bug"))
        .when(exportHandler)
        .sendSignal(any(), any(), any());
    TransferSignals signals = telemetrySignals();

    assertThrows(
        IllegalStateException.class,
        () -> signals.sendStart(jobId, exportAuthData, importAuthData, withCounters));
    signals.sendEnd(
        jobId, exportAuthData, importAuthData, ended(EndReason.PARTIALLY_COMPLETED));

    List<SignalRequest> requests = requestsTo(importHandler, importAuthData);
    assertNotNull(requests.get(0).startTelemetry());
    assertNotNull(requests.get(1).endTelemetry());
  }

  @Test
  void end_anyExceptionFromFirstHandler_isLoggedAtInfo_andSecondHandlerStillGetsEnd()
      throws Exception {
    IllegalStateException failure = new IllegalStateException("handler bug");
    doThrow(failure).when(exportHandler).sendSignal(withState(State.ENDED), any(), any());
    TransferSignals signals = signals(false, Optional.empty(), NO_PROGRESS);

    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);
    signals.sendEnd(jobId, exportAuthData, importAuthData, COMPLETED);

    assertEquals(
        List.of(todaysRequest(JobLifeCycle.JOB_STARTED()), todaysRequest(COMPLETED)),
        requestsTo(importHandler, importAuthData));
    verify(monitor).info(any(), same(failure));
  }

  @Test
  void endWithoutStart_everyHandlerGetsTodaysEnd() throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);

    telemetrySignals().sendEnd(jobId, exportAuthData, importAuthData, COMPLETED);

    assertEquals(List.of(todaysRequest(COMPLETED)), requestsTo(exportHandler, exportAuthData));
    assertEquals(List.of(todaysRequest(COMPLETED)), requestsTo(importHandler, importAuthData));
  }

  @Test
  void cancel_optedInHandlerGetsOneUserCancelledEnd_otherHandlerGetsNothing() throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);
    TransferSignals signals = telemetrySignals();
    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);

    signals.sendEndOnCancel(EndReason.USER_CANCELLED);

    assertEquals(
        List.of(todaysRequest(JobLifeCycle.JOB_STARTED())),
        requestsTo(exportHandler, exportAuthData));
    SignalRequest end = requestsTo(importHandler, importAuthData).get(1);
    assertEquals(ended(EndReason.USER_CANCELLED), end.jobStatus());
    assertEquals(7L, end.endTelemetry().totalItemsFetched());
    verify(monitor).flushLogs();
  }

  @Test
  void cancel_jobMarkedErrored_sendsErrored() throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);
    TransferSignals signals = telemetrySignals();
    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);

    signals.sendEndOnCancel(EndReason.ERRORED);

    assertEquals(
        ended(EndReason.ERRORED), requestsTo(importHandler, importAuthData).get(1).jobStatus());
  }

  @Test
  void cancelThenFinally_optedInHandlerGetsOneEnd_otherHandlerStillGetsTodaysEnd()
      throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);
    TransferSignals signals = telemetrySignals();
    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);

    signals.sendEndOnCancel(EndReason.USER_CANCELLED);
    signals.sendEnd(jobId, exportAuthData, importAuthData, COMPLETED);

    assertEquals(1, endsSentTo(importHandler, importAuthData));
    assertEquals(
        List.of(todaysRequest(JobLifeCycle.JOB_STARTED()), todaysRequest(COMPLETED)),
        requestsTo(exportHandler, exportAuthData));
  }

  @Test
  void finallyWhileTheCancelWatcherSendsEnd_waitsForItToFinish() throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);
    CountDownLatch cancelEndStarted = new CountDownLatch(1);
    CountDownLatch releaseCancelEnd = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              cancelEndStarted.countDown();
              releaseCancelEnd.await(10, TimeUnit.SECONDS);
              return null;
            })
        .when(importHandler)
        .sendSignal(withState(State.ENDED), any(), any());
    TransferSignals signals = telemetrySignals();
    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);
    CompletableFuture<Void> cancel =
        CompletableFuture.runAsync(() -> signals.sendEndOnCancel(EndReason.USER_CANCELLED));
    assertTrue(cancelEndStarted.await(10, TimeUnit.SECONDS));

    CompletableFuture<Void> finallyBlock =
        CompletableFuture.runAsync(
            () -> signals.sendEnd(jobId, exportAuthData, importAuthData, COMPLETED));
    Thread.sleep(100);
    assertFalse(finallyBlock.isDone());
    releaseCancelEnd.countDown();

    finallyBlock.get(10, TimeUnit.SECONDS);
    cancel.get(10, TimeUnit.SECONDS);
    assertEquals(1, endsSentTo(importHandler, importAuthData));
  }

  @Test
  void cancelWhileFinallySendsEnd_waitsForItToFinish() throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);
    CountDownLatch jobEndStarted = new CountDownLatch(1);
    CountDownLatch releaseJobEnd = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              jobEndStarted.countDown();
              releaseJobEnd.await(10, TimeUnit.SECONDS);
              return null;
            })
        .when(importHandler)
        .sendSignal(withState(State.ENDED), any(), any());
    TransferSignals signals = telemetrySignals();
    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);
    CompletableFuture<Void> finallyBlock =
        CompletableFuture.runAsync(
            () -> signals.sendEnd(jobId, exportAuthData, importAuthData, COMPLETED));
    assertTrue(jobEndStarted.await(10, TimeUnit.SECONDS));

    CompletableFuture<Void> cancel =
        CompletableFuture.runAsync(() -> signals.sendEndOnCancel(EndReason.USER_CANCELLED));
    Thread.sleep(100);
    assertFalse(cancel.isDone());
    releaseJobEnd.countDown();

    finallyBlock.get(10, TimeUnit.SECONDS);
    cancel.get(10, TimeUnit.SECONDS);
    assertEquals(1, endsSentTo(importHandler, importAuthData));
    assertEquals(COMPLETED, requestsTo(importHandler, importAuthData).get(1).jobStatus());
    verify(monitor).flushLogs();
  }

  @Test
  void claimEndBeforeWatcherPolls_watcherWaitsForSendEnd_andJobSendsPartiallyCompleted()
      throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);
    JobLifeCycle partial = ended(EndReason.PARTIALLY_COMPLETED);
    TransferSignals signals = telemetrySignals();
    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);

    // The job's finally claims End before writing ERROR to JobStore.
    signals.claimEnd();
    // The cancel watcher polls ERROR before sendEnd starts, loses the claim, and waits.
    CompletableFuture<Void> watcher =
        CompletableFuture.runAsync(() -> signals.sendEndOnCancel(EndReason.ERRORED));
    Thread.sleep(100);
    assertFalse(watcher.isDone());

    signals.sendEnd(jobId, exportAuthData, importAuthData, partial);
    // Well under CANCEL_WAIT, so this fails if the job's End doesn't wake the cancel watcher.
    watcher.get(5, TimeUnit.SECONDS);

    assertEquals(1, endsSentTo(importHandler, importAuthData));
    assertEquals(partial, requestsTo(importHandler, importAuthData).get(1).jobStatus());
  }

  @Test
  void cancelWhileFinallyIsStuckInEnd_onlyDelaysTheCancel() throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);
    CountDownLatch jobEndStarted = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              jobEndStarted.countDown();
              release.await(30, TimeUnit.SECONDS);
              return null;
            })
        .when(importHandler)
        .sendSignal(withState(State.ENDED), any(), any());
    TransferSignals signals =
        new TransferSignals(
            () -> exportHandler,
            () -> importHandler,
            true,
            true,
            NO_PROGRESS,
            Optional.of(LIFETIME),
            Duration.ofMillis(200),
            monitor,
            CLOCK);
    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);

    try {
      CompletableFuture<Void> finallyBlock =
          CompletableFuture.runAsync(
              () -> signals.sendEnd(jobId, exportAuthData, importAuthData, COMPLETED));
      assertTrue(jobEndStarted.await(10, TimeUnit.SECONDS));

      Stopwatch stopwatch = Stopwatch.createStarted();
      signals.sendEndOnCancel(EndReason.USER_CANCELLED);

      // It waited for the job's End, then gave up while the handler is still stuck.
      assertTrue(stopwatch.elapsed().compareTo(Duration.ofMillis(200)) >= 0);
      assertTrue(stopwatch.elapsed().compareTo(Duration.ofSeconds(10)) < 0);
      assertFalse(finallyBlock.isDone());
    } finally {
      release.countDown();
    }
  }

  @Test
  void cancelThenClaimEnd_optedInHandlerGetsOnlyTheCancelEnd() throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);
    TransferSignals signals = telemetrySignals();
    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);

    signals.sendEndOnCancel(EndReason.USER_CANCELLED);
    // The job's finally runs after the cancel watcher claimed End.
    signals.claimEnd();
    signals.sendEnd(jobId, exportAuthData, importAuthData, COMPLETED);

    assertEquals(1, endsSentTo(importHandler, importAuthData));
    assertEquals(
        ended(EndReason.USER_CANCELLED),
        requestsTo(importHandler, importAuthData).get(1).jobStatus());
    assertEquals(
        List.of(todaysRequest(JobLifeCycle.JOB_STARTED()), todaysRequest(COMPLETED)),
        requestsTo(exportHandler, exportAuthData));
  }

  @Test
  void claimEndThenTheJobIsSlowToSendEnd_cancelWaitsOnlyTheTimeout_andSendsNoEndOfItsOwn()
      throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);
    Duration cancelWait = Duration.ofMillis(200);
    TransferSignals signals = telemetrySignals(cancelWait);
    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);
    // The job's finally claims End, then is slow to write the final state or run its hooks.
    signals.claimEnd();

    Stopwatch stopwatch = Stopwatch.createStarted();
    signals.sendEndOnCancel(EndReason.USER_CANCELLED);

    assertTrue(stopwatch.elapsed().compareTo(cancelWait) >= 0);
    assertEquals(0, endsSentTo(importHandler, importAuthData));
    // If the worker is still running, the job's End still goes out, once.
    signals.sendEnd(jobId, exportAuthData, importAuthData, COMPLETED);
    assertEquals(1, endsSentTo(importHandler, importAuthData));
    assertEquals(COMPLETED, requestsTo(importHandler, importAuthData).get(1).jobStatus());
  }

  @Test
  void claimEnd_noProgressFollows() throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);
    CountDownLatch progressSending = new CountDownLatch(1);
    CountDownLatch endClaimed = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              progressSending.countDown();
              endClaimed.await(10, TimeUnit.SECONDS);
              return null;
            })
        .when(importHandler)
        .sendSignal(withState(State.IN_PROGRESS), any(), any());
    TransferSignals signals = signals(true, Optional.of(LIFETIME), Duration.ofMillis(10));
    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);
    assertTrue(progressSending.await(10, TimeUnit.SECONDS));

    // The job's finally claims End while a Progress is going out.
    signals.claimEnd();
    endClaimed.countDown();
    Thread.sleep(200);

    verify(importHandler, times(1)).sendSignal(withState(State.IN_PROGRESS), any(), any());
    signals.sendEnd(jobId, exportAuthData, importAuthData, COMPLETED);
    assertEquals(COMPLETED, requestsTo(importHandler, importAuthData).get(2).jobStatus());
  }

  @Test
  void cancelWaitingForTheJobsEnd_isInterrupted_returnsAtOnce_andKeepsTheInterrupt()
      throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);
    TransferSignals signals = telemetrySignals();
    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);
    signals.claimEnd();

    Stopwatch stopwatch = Stopwatch.createStarted();
    boolean stillInterrupted;
    Thread.currentThread().interrupt();
    try {
      signals.sendEndOnCancel(EndReason.USER_CANCELLED);
    } finally {
      // Also clears the interrupt, so it can't leak into other tests.
      stillInterrupted = Thread.interrupted();
    }

    assertTrue(stillInterrupted);
    assertTrue(stopwatch.elapsed().compareTo(CANCEL_WAIT) < 0);
    assertEquals(0, endsSentTo(importHandler, importAuthData));
    signals.sendEnd(jobId, exportAuthData, importAuthData, COMPLETED);
  }

  @Test
  void cancelWaitingForTheJobsEnd_neverThrows() throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);
    IllegalStateException failure = new IllegalStateException("broken monitor");
    doThrow(failure).when(monitor).flushLogs();
    TransferSignals signals = telemetrySignals(Duration.ofMillis(1));
    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);
    signals.claimEnd();

    signals.sendEndOnCancel(EndReason.USER_CANCELLED);

    verify(monitor).info(any(), same(failure));
    signals.sendEnd(jobId, exportAuthData, importAuthData, COMPLETED);
  }

  @Test
  void endOnFailure_optedInHandlerGetsTheClaimedEnd_otherHandlerGetsNothing() throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);
    JobLifeCycle partial = ended(EndReason.PARTIALLY_COMPLETED);
    TransferSignals signals = telemetrySignals();
    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);
    signals.claimEnd();
    // The cancel watcher polls the job's own ERROR, loses the claim, and waits for the job's End.
    CompletableFuture<Void> watcher =
        CompletableFuture.runAsync(() -> signals.sendEndOnCancel(EndReason.ERRORED));
    Thread.sleep(100);
    assertFalse(watcher.isDone());

    // Then the job's finally fails before it gets to sendEnd.
    signals.sendEndOnFailure(partial);
    // Well under CANCEL_WAIT, so this fails if the job's End doesn't wake the cancel watcher.
    watcher.get(5, TimeUnit.SECONDS);

    assertEquals(1, endsSentTo(importHandler, importAuthData));
    SignalRequest end = requestsTo(importHandler, importAuthData).get(1);
    assertEquals(partial, end.jobStatus());
    assertNotNull(end.endTelemetry());
    assertEquals(
        List.of(todaysRequest(JobLifeCycle.JOB_STARTED())),
        requestsTo(exportHandler, exportAuthData));
  }

  @Test
  void endOnFailure_whileTheCancelWatcherSendsEnd_sendsNoSecondEnd_andWaitsForIt()
      throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);
    CountDownLatch cancelEndStarted = new CountDownLatch(1);
    CountDownLatch releaseCancelEnd = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              cancelEndStarted.countDown();
              releaseCancelEnd.await(10, TimeUnit.SECONDS);
              return null;
            })
        .when(importHandler)
        .sendSignal(withState(State.ENDED), any(), any());
    TransferSignals signals = telemetrySignals();
    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);
    CompletableFuture<Void> cancel =
        CompletableFuture.runAsync(() -> signals.sendEndOnCancel(EndReason.USER_CANCELLED));
    assertTrue(cancelEndStarted.await(10, TimeUnit.SECONDS));

    CompletableFuture<Void> failedFinally =
        CompletableFuture.runAsync(
            () -> {
              signals.claimEnd();
              signals.sendEndOnFailure(ended(EndReason.PARTIALLY_COMPLETED));
            });
    Thread.sleep(100);
    assertFalse(failedFinally.isDone());
    releaseCancelEnd.countDown();

    failedFinally.get(10, TimeUnit.SECONDS);
    cancel.get(10, TimeUnit.SECONDS);
    assertEquals(1, endsSentTo(importHandler, importAuthData));
    assertEquals(
        ended(EndReason.USER_CANCELLED),
        requestsTo(importHandler, importAuthData).get(1).jobStatus());
    assertEquals(
        List.of(todaysRequest(JobLifeCycle.JOB_STARTED())),
        requestsTo(exportHandler, exportAuthData));
  }

  @Test
  void endOnFailure_withoutTelemetry_sendsNothing() throws Exception {
    TransferSignals signals = signals(false, Optional.of(LIFETIME), NO_PROGRESS);
    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);

    signals.claimEnd();
    signals.sendEndOnFailure(COMPLETED);

    assertEquals(0, endsSentTo(exportHandler, exportAuthData));
    assertEquals(0, endsSentTo(importHandler, importAuthData));
  }

  @Test
  void cancel_stuckHandler_onlyDelaysTheCancel_andTheJobsEnd() throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);
    CountDownLatch release = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              release.await(30, TimeUnit.SECONDS);
              return null;
            })
        .when(importHandler)
        .sendSignal(withState(State.ENDED), any(), any());
    TransferSignals signals =
        new TransferSignals(
            () -> exportHandler,
            () -> importHandler,
            true,
            true,
            NO_PROGRESS,
            Optional.of(LIFETIME),
            Duration.ofMillis(200),
            monitor,
            CLOCK);
    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);

    try {
      Stopwatch stopwatch = Stopwatch.createStarted();
      signals.sendEndOnCancel(EndReason.USER_CANCELLED);
      signals.sendEnd(jobId, exportAuthData, importAuthData, COMPLETED);

      // Both returned after the short wait, while the handler is still stuck in End.
      assertTrue(stopwatch.elapsed().compareTo(Duration.ofSeconds(10)) < 0);
      verify(importHandler, timeout(10_000)).sendSignal(withState(State.ENDED), any(), any());
    } finally {
      release.countDown();
    }
  }

  @Test
  void cancel_handlerThrowsAnError_theCancelStillCompletes() throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);
    AssertionError failure = new AssertionError("handler bug");
    doThrow(failure).when(importHandler).sendSignal(withState(State.ENDED), any(), any());
    TransferSignals signals = telemetrySignals();
    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);

    signals.sendEndOnCancel(EndReason.USER_CANCELLED);
    // The job's finally finds End already claimed and doesn't wait.
    signals.sendEnd(jobId, exportAuthData, importAuthData, COMPLETED);

    verify(importHandler, times(1)).sendSignal(withState(State.ENDED), any(), any());
    verify(monitor).info(any(), same(failure));
  }

  @Test
  void cancelAfterEnd_sendsNothingMore() throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);
    TransferSignals signals = telemetrySignals();
    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);
    signals.sendEnd(jobId, exportAuthData, importAuthData, COMPLETED);

    signals.sendEndOnCancel(EndReason.USER_CANCELLED);

    assertEquals(1, endsSentTo(importHandler, importAuthData));
    assertEquals(COMPLETED, requestsTo(importHandler, importAuthData).get(1).jobStatus());
  }

  @Test
  void cancel_withoutTelemetry_sendsNothing() throws Exception {
    TransferSignals signals = signals(false, Optional.of(LIFETIME), NO_PROGRESS);
    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);

    signals.sendEndOnCancel(EndReason.USER_CANCELLED);

    assertEquals(0, endsSentTo(exportHandler, exportAuthData));
    assertEquals(0, endsSentTo(importHandler, importAuthData));
  }

  @Test
  void cancelBeforeStart_sendsNothing() throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);

    telemetrySignals().sendEndOnCancel(EndReason.USER_CANCELLED);

    verify(exportHandler, never()).sendSignal(any(), any(), any());
    verify(importHandler, never()).sendSignal(any(), any(), any());
  }

  @Test
  void countersFailAtEnd_endIsStillSent_withoutTheTelemetryBlock() throws Exception {
    when(importHandler.supportsTelemetry()).thenReturn(true);
    when(counters.itemsFailedToFetch()).thenThrow(new IllegalStateException("not thread safe"));
    TransferSignals signals = telemetrySignals();

    signals.sendStart(jobId, exportAuthData, importAuthData, withCounters);
    signals.sendEnd(jobId, exportAuthData, importAuthData, COMPLETED);

    SignalRequest end = requestsTo(importHandler, importAuthData).get(1);
    assertEquals(COMPLETED, end.jobStatus());
    assertNull(end.endTelemetry());
  }

  @Test
  void noRecurringJob_startHasNoRecurringJobId() throws Exception {
    JobMetadata.reset();
    JobMetadata.init(
        jobId, new byte[0], DataVertical.PHOTOS, "Apple", "Google", Stopwatch.createUnstarted());
    when(importHandler.supportsTelemetry()).thenReturn(true);

    telemetrySignals().sendStart(jobId, exportAuthData, importAuthData, withCounters);

    assertNull(onlyRequestTo(importHandler, importAuthData).startTelemetry().recurringJobId());
  }

  private TransferSignals telemetrySignals() {
    return signals(true, Optional.of(LIFETIME), NO_PROGRESS);
  }

  private TransferSignals telemetrySignals(Duration cancelWait) {
    return new TransferSignals(
        () -> exportHandler,
        () -> importHandler,
        true,
        true,
        NO_PROGRESS,
        Optional.of(LIFETIME),
        cancelWait,
        monitor,
        CLOCK);
  }

  private TransferSignals signals(
      boolean telemetryEnabled, Optional<Duration> maxWorkerLifetime, Duration heartbeatInterval) {
    return new TransferSignals(
        () -> exportHandler,
        () -> importHandler,
        true,
        telemetryEnabled,
        heartbeatInterval,
        maxWorkerLifetime,
        CANCEL_WAIT,
        monitor,
        CLOCK);
  }

  private void assertTodaysSignalsOnly() throws Exception {
    List<SignalRequest> expected =
        List.of(todaysRequest(JobLifeCycle.JOB_STARTED()), todaysRequest(COMPLETED));
    assertEquals(expected, requestsTo(exportHandler, exportAuthData));
    assertEquals(expected, requestsTo(importHandler, importAuthData));
  }

  private SignalRequest todaysRequest(JobLifeCycle jobStatus) {
    return todaysRequestBuilder(jobStatus).build();
  }

  private SignalRequest.Builder todaysRequestBuilder(JobLifeCycle jobStatus) {
    return SignalRequest.builder()
        .setJobId(jobId.toString())
        .setDataType(DataVertical.PHOTOS.getDataType())
        .setJobStatus(jobStatus)
        .setExportingService("Apple")
        .setImportingService("Google");
  }

  private static JobLifeCycle ended(EndReason endReason) {
    return JobLifeCycle.builder().setState(State.ENDED).setEndReason(endReason).build();
  }

  private static SignalRequest withState(State state) {
    return argThat(request -> request != null && request.jobStatus().state() == state);
  }

  /** Every request sent to {@code handler}, in order. Also checks it got the right auth data. */
  private static List<SignalRequest> requestsTo(SignalHandler handler, AuthData authData)
      throws Exception {
    ArgumentCaptor<SignalRequest> captor = ArgumentCaptor.forClass(SignalRequest.class);
    verify(handler, atLeast(0)).sendSignal(captor.capture(), any(), any());
    verify(handler, never()).sendSignal(any(), argThat(a -> a != authData), any());
    return List.copyOf(captor.getAllValues());
  }

  private static SignalRequest onlyRequestTo(SignalHandler handler, AuthData authData)
      throws Exception {
    List<SignalRequest> requests = requestsTo(handler, authData);
    assertEquals(1, requests.size(), "requests: " + requests);
    return requests.get(0);
  }

  private static long endsSentTo(SignalHandler handler, AuthData authData) throws Exception {
    return requestsTo(handler, authData).stream()
        .filter(request -> request.jobStatus().state() == State.ENDED)
        .collect(Collectors.counting());
  }

  @SuppressWarnings("unchecked")
  private static Provider<SignalHandler> providerMock(SignalHandler handler) {
    Provider<SignalHandler> provider = mock(Provider.class);
    when(provider.get()).thenReturn(handler);
    return provider;
  }
}
