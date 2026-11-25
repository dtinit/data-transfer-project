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

package org.datatransferproject.datatransfer.google.signals;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.Uninterruptibles.awaitUninterruptibly;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;
import java.util.function.Supplier;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.provider.SignalRequest;
import org.datatransferproject.spi.transfer.types.signals.EndTelemetry;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle.EndReason;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle.State;
import org.datatransferproject.spi.transfer.types.signals.ProgressTelemetry;
import org.datatransferproject.spi.transfer.types.signals.StartTelemetry;
import org.datatransferproject.types.transfer.auth.TokenAuthData;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

// Some senders block until the test releases them, so a regression could hang instead of fail.
@Timeout(30)
class GoogleSignalHandlerTest {

  private static final String JOB_ID = "8b0c4c7e-3f0a-4d4e-9a52-1d3c3b6f2a10";
  private static final String NEXT_JOB_ID = "1f9e2d3c-4b5a-4c6d-8e7f-0a1b2c3d4e5f";
  private static final long NOW = 1_792_072_800_000L;
  private static final Duration END_TIMEOUT = Duration.ofSeconds(10);
  private static final JobLifeCycle COMPLETED =
      JobLifeCycle.builder()
          .setState(State.ENDED)
          .setEndReason(EndReason.SUCCESSFULLY_COMPLETED)
          .build();
  private static final StartTelemetry START_TELEMETRY =
      StartTelemetry.builder().setTimestamp(NOW).build();
  private static final EndTelemetry END_TELEMETRY =
      EndTelemetry.builder()
          .setTimestamp(NOW + 60_000)
          .setTotalItemsFetched(10L)
          .setTotalItemsFailedToFetch(0L)
          .build();

  private final TokensAndUrlAuthData authData =
      new TokensAndUrlAuthData("access-token", "refresh-token", "https://oauth2.example.com/token");
  private final Monitor monitor = mock(Monitor.class);
  private final List<SignalRequest> sent = new CopyOnWriteArrayList<>();
  /** Whether each sent report may be retried, in the order they were sent. */
  private final List<BooleanSupplier> mayRetries = new CopyOnWriteArrayList<>();
  private final TelemetrySender recordingSender =
      (request, auth, mayRetry) -> {
        mayRetries.add(mayRetry);
        sent.add(request);
      };

  @Test
  void supportsTelemetry_isTrue() {
    assertThat(handler(recordingSender).supportsTelemetry()).isTrue();
  }

  @Test
  void startAndEnd_areReported_inOrder() {
    GoogleSignalHandler handler = handler(recordingSender);

    handler.sendSignal(start(), authData, monitor);
    handler.sendSignal(end(), authData, monitor);

    // End waits until it's sent, and reports go out in order.
    assertThat(sent).containsExactly(start(), end()).inOrder();
  }

  @Test
  void progress_isReported() {
    GoogleSignalHandler handler = handler(recordingSender);

    handler.sendSignal(start(), authData, monitor);
    handler.sendSignal(progress(1), authData, monitor);

    awaitSent(2);
    assertThat(sent).containsExactly(start(), progress(1)).inOrder();
  }

  @Test
  void start_doesntWaitForTheReport() {
    CountDownLatch sending = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    GoogleSignalHandler handler =
        handler(
            (request, auth, mayRetry) -> {
              sending.countDown();
              awaitUninterruptibly(release);
              sent.add(request);
            });

    try {
      handler.sendSignal(start(), authData, monitor);

      assertThat(awaitUninterruptibly(sending, 10, SECONDS)).isTrue();
      assertThat(sent).isEmpty();
    } finally {
      release.countDown();
    }
    awaitSent(1);
  }

  @Test
  void end_waitsUntilItsSent() {
    GoogleSignalHandler handler =
        handler(
            (request, auth, mayRetry) -> {
              sleepUninterruptibly(200, MILLISECONDS);
              sent.add(request);
            });

    handler.sendSignal(end(), authData, monitor);

    assertThat(sent).containsExactly(end());
  }

  @Test
  void end_waitsAtMostTheEndTimeout() {
    CountDownLatch release = new CountDownLatch(1);
    GoogleSignalHandler handler =
        new GoogleSignalHandler(
            (request, auth, mayRetry) -> {
              awaitUninterruptibly(release);
              sent.add(request);
            },
            Duration.ofMillis(200),
            monitor);

    try {
      long startNanos = System.nanoTime();
      handler.sendSignal(end(), authData, monitor);
      Duration waited = Duration.ofNanos(System.nanoTime() - startNanos);

      // It gave up while the report was still going out, and the log says which job's End.
      assertThat(sent).isEmpty();
      assertThat(waited).isLessThan(Duration.ofSeconds(5));
      ArgumentCaptor<Supplier<String>> message = messageCaptor();
      verify(monitor).info(message.capture());
      assertThat(message.getValue().get()).contains(JOB_ID);
    } finally {
      release.countDown();
    }
  }

  @Test
  void waitingProgress_isReplacedByANewerOne() {
    CountDownLatch release = new CountDownLatch(1);
    GoogleSignalHandler handler = handler(blockStartUntil(release));

    handler.sendSignal(start(), authData, monitor);
    handler.sendSignal(progress(1), authData, monitor);
    handler.sendSignal(progress(2), authData, monitor);
    handler.sendSignal(progress(3), authData, monitor);
    release.countDown();
    // Reports go out in order, so once this End is out, any other Progress would be too.
    handler.sendSignal(nextJobsEnd(), authData, monitor);

    assertThat(sent).containsExactly(start(), progress(3), nextJobsEnd()).inOrder();
  }

  @Test
  void waitingProgress_isDroppedWhenEndArrives() throws Exception {
    CountDownLatch release = new CountDownLatch(1);
    GoogleSignalHandler handler = handler(blockStartUntil(release));
    handler.sendSignal(start(), authData, monitor);
    handler.sendSignal(progress(1), authData, monitor);
    // Lets Start finish only once this thread waits for End to go out, so after End arrived.
    Thread testThread = Thread.currentThread();
    Thread releaser =
        new Thread(
            () -> {
              awaitTimedWaiting(testThread);
              release.countDown();
            });
    releaser.start();

    handler.sendSignal(end(), authData, monitor);
    releaser.join();

    assertThat(sent).containsExactly(start(), end()).inOrder();
  }

  @Test
  void progressAfterEnd_isntReported() {
    GoogleSignalHandler handler = handler(recordingSender);
    SignalRequest nextJobsStart =
        request(JobLifeCycle.JOB_STARTED())
            .setJobId(NEXT_JOB_ID)
            .setStartTelemetry(START_TELEMETRY)
            .build();

    handler.sendSignal(start(), authData, monitor);
    handler.sendSignal(end(), authData, monitor);
    handler.sendSignal(progress(1), authData, monitor);
    // Reports go out in order, so once this one is out, the Progress would be too. It's a Start,
    // since the handler only remembers the latest job that ended, and a worker runs one job.
    handler.sendSignal(nextJobsStart, authData, monitor);

    awaitSent(3);
    assertThat(sent).containsExactly(start(), end(), nextJobsStart).inOrder();
  }

  @Test
  void start_isRetried_untilItsJobsEndArrives() {
    GoogleSignalHandler handler = handler(recordingSender);
    handler.sendSignal(start(), authData, monitor);
    awaitSent(1);
    BooleanSupplier startMayRetry = mayRetries.get(0);
    assertThat(startMayRetry.getAsBoolean()).isTrue();

    handler.sendSignal(end(), authData, monitor);

    // A Start that's still going out then stops retrying, so it doesn't hold up End.
    assertThat(startMayRetry.getAsBoolean()).isFalse();
  }

  @Test
  void end_isRetried() {
    GoogleSignalHandler handler = handler(recordingSender);

    handler.sendSignal(end(), authData, monitor);

    assertThat(mayRetries.get(0).getAsBoolean()).isTrue();
  }

  @Test
  void progress_isntRetried() {
    GoogleSignalHandler handler = handler(recordingSender);

    handler.sendSignal(progress(1), authData, monitor);
    awaitSent(1);

    // The next Progress, or End, replaces it.
    assertThat(mayRetries.get(0).getAsBoolean()).isFalse();
  }

  @ParameterizedTest
  @CsvSource({
    // exportingService, importingService, dataType
    "GOOGLE, APPLE, PHOTOS",
    "GOOGLE, GOOGLE, PHOTOS",
    "APPLE, MICROSOFT, PHOTOS",
    "APPLE, GOOGLE, CALENDAR",
    "APPLE, GOOGLE, BLOBS",
  })
  void notAnImportIntoGooglePhotos_isntReported(
      String exportingService, String importingService, String dataType) {
    GoogleSignalHandler handler = handler(recordingSender);

    handler.sendSignal(
        request(JobLifeCycle.JOB_STARTED(), exportingService, importingService, dataType)
            .setStartTelemetry(START_TELEMETRY)
            .build(),
        authData,
        monitor);
    handler.sendSignal(
        request(JobLifeCycle.IN_PROGRESS(), exportingService, importingService, dataType)
            .setProgressTelemetry(progressTelemetry(1))
            .build(),
        authData,
        monitor);
    handler.sendSignal(
        request(COMPLETED, exportingService, importingService, dataType)
            .setEndTelemetry(END_TELEMETRY)
            .build(),
        authData,
        monitor);
    // Reports go out in order, so once this End is out, any of the above would be too.
    handler.sendSignal(nextJobsEnd(), authData, monitor);

    assertThat(sent).containsExactly(nextJobsEnd());
  }

  @ParameterizedTest
  @ValueSource(strings = {"PHOTOS", "VIDEOS", "MEDIA", "photos"})
  void photosVideosAndMedia_areReported(String dataType) {
    GoogleSignalHandler handler = handler(recordingSender);

    handler.sendSignal(
        request(COMPLETED, "APPLE", "GOOGLE", dataType).setEndTelemetry(END_TELEMETRY).build(),
        authData,
        monitor);

    assertThat(sent).hasSize(1);
  }

  @ParameterizedTest
  @ValueSource(strings = {"GOOGLE", "Google", "google"})
  void serviceNames_matchIgnoringCase(String google) {
    GoogleSignalHandler handler = handler(recordingSender);

    handler.sendSignal(
        request(COMPLETED, "APPLE", google, "PHOTOS").setEndTelemetry(END_TELEMETRY).build(),
        authData,
        monitor);

    assertThat(sent).hasSize(1);
  }

  @Test
  void signalsWithoutTelemetry_arentReported() {
    GoogleSignalHandler handler = handler(recordingSender);

    // What a worker sends when its host turns telemetry off.
    handler.sendSignal(request(JobLifeCycle.JOB_STARTED()).build(), authData, monitor);
    handler.sendSignal(request(COMPLETED).build(), authData, monitor);
    // Reports go out in order, so once this End is out, either of the above would be too.
    handler.sendSignal(nextJobsEnd(), authData, monitor);

    assertThat(sent).containsExactly(nextJobsEnd());
  }

  @Test
  void endWithoutItsTelemetry_isReported_whenTheJobsStartHadTelemetry() {
    GoogleSignalHandler handler = handler(recordingSender);
    // What the worker sends when it can't read its counters at the end.
    SignalRequest endWithoutTelemetry = request(COMPLETED).build();

    handler.sendSignal(start(), authData, monitor);
    handler.sendSignal(endWithoutTelemetry, authData, monitor);

    assertThat(sent).containsExactly(start(), endWithoutTelemetry).inOrder();
  }

  @Test
  void otherAuthData_isntReported() {
    GoogleSignalHandler handler = handler(recordingSender);

    handler.sendSignal(end(), new TokenAuthData("token"), monitor);

    assertThat(sent).isEmpty();
  }

  @ParameterizedTest
  @MethodSource("senderFailures")
  void senderFailure_doesntReachTheWorker_andLaterSignalsStillGoOut(Throwable failure) {
    GoogleSignalHandler handler =
        handler(
            (request, auth, mayRetry) -> {
              if (request.startTelemetry() != null) {
                throwUnchecked(failure);
              }
              sent.add(request);
            });

    handler.sendSignal(start(), authData, monitor);
    handler.sendSignal(end(), authData, monitor);

    assertThat(sent).containsExactly(end());
    ArgumentCaptor<Supplier<String>> message = messageCaptor();
    verify(monitor).info(message.capture(), same(failure));
    assertThat(message.getValue().get()).contains(JOB_ID);
  }

  @SuppressWarnings("unchecked")
  private static ArgumentCaptor<Supplier<String>> messageCaptor() {
    return ArgumentCaptor.forClass(Supplier.class);
  }

  private static Stream<Throwable> senderFailures() {
    // Such as an Error from a dependency version that doesn't match.
    return Stream.of(
        new IllegalStateException("sender bug"), new NoClassDefFoundError("missing class"));
  }

  private static void throwUnchecked(Throwable failure) {
    if (failure instanceof Error) {
      throw (Error) failure;
    }
    throw (RuntimeException) failure;
  }

  private GoogleSignalHandler handler(TelemetrySender sender) {
    return new GoogleSignalHandler(sender, END_TIMEOUT, monitor);
  }

  /** Records every report, but holds the Start report until {@code release}. */
  private TelemetrySender blockStartUntil(CountDownLatch release) {
    return (request, auth, mayRetry) -> {
      if (request.startTelemetry() != null) {
        awaitUninterruptibly(release);
      }
      sent.add(request);
    };
  }

  /** Waits, for at most 10 seconds, until {@code thread} waits with a timeout. */
  private static void awaitTimedWaiting(Thread thread) {
    long deadline = System.nanoTime() + SECONDS.toNanos(10);
    while (thread.getState() != Thread.State.TIMED_WAITING && System.nanoTime() < deadline) {
      sleepUninterruptibly(1, MILLISECONDS);
    }
  }

  private void awaitSent(int count) {
    long deadline = System.nanoTime() + SECONDS.toNanos(10);
    while (sent.size() < count && System.nanoTime() < deadline) {
      sleepUninterruptibly(10, MILLISECONDS);
    }
    assertThat(sent.size()).isAtLeast(count);
  }

  private static SignalRequest.Builder request(JobLifeCycle jobStatus) {
    return request(jobStatus, "APPLE", "GOOGLE", "PHOTOS");
  }

  private static SignalRequest.Builder request(
      JobLifeCycle jobStatus, String exportingService, String importingService, String dataType) {
    return SignalRequest.builder()
        .setJobId(JOB_ID)
        .setDataType(dataType)
        .setJobStatus(jobStatus)
        .setExportingService(exportingService)
        .setImportingService(importingService);
  }

  private static ProgressTelemetry progressTelemetry(long fetched) {
    return ProgressTelemetry.builder()
        .setTimestamp(NOW + fetched)
        .setTotalItemsFetched(fetched)
        .setTotalItemsFailedToFetch(0L)
        .build();
  }

  private static SignalRequest start() {
    return request(JobLifeCycle.JOB_STARTED()).setStartTelemetry(START_TELEMETRY).build();
  }

  private static SignalRequest progress(long fetched) {
    return request(JobLifeCycle.IN_PROGRESS())
        .setProgressTelemetry(progressTelemetry(fetched))
        .build();
  }

  private static SignalRequest end() {
    return request(COMPLETED).setEndTelemetry(END_TELEMETRY).build();
  }

  /** Another job's End, which the handler reports, and waits for. */
  private static SignalRequest nextJobsEnd() {
    return request(COMPLETED).setJobId(NEXT_JOB_ID).setEndTelemetry(END_TELEMETRY).build();
  }
}
