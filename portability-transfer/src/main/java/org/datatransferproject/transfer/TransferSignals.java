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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.provider.SignalHandler;
import org.datatransferproject.spi.transfer.provider.SignalRequest;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.spi.transfer.types.signals.EndTelemetry;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle.EndReason;
import org.datatransferproject.spi.transfer.types.signals.ProgressTelemetry;
import org.datatransferproject.spi.transfer.types.signals.StartTelemetry;
import org.datatransferproject.transfer.Annotations.ExportSignalHandler;
import org.datatransferproject.transfer.Annotations.ImportSignalHandler;
import org.datatransferproject.transfer.copier.TelemetryCounters;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.retry.RetryException;

/**
 * Sends a job's lifecycle signals to its export and import signal handlers, and transfer telemetry
 * to the handlers that opt in.
 *
 * <p>Each handler is called in its own {@code try}, so one handler's failure can't stop the other
 * handler's signal. Handler failures are logged at info, except Progress failures: Progress repeats
 * every heartbeat interval, so those are logged at debug.
 *
 * <p>Whether a job sends telemetry is decided once, at Start. It does when {@code
 * transferSignalEnabled} and {@code transferTelemetryEnabled} are on (both are by default), the
 * copier keeps counters, and at least one handler returns true from {@link
 * SignalHandler#supportsTelemetry()}. Handlers that don't opt in always get exactly the signals
 * they got before telemetry existed. Start carries an expiration only if {@code
 * maxWorkerLifetimeSeconds} is set.
 *
 * <p>When a job sends telemetry, the handlers that opt in also get the telemetry blocks, a
 * Progress signal every heartbeat interval, and an End when the cancel watcher stops the job. They
 * get at most one End per job.
 */
@Singleton
final class TransferSignals {

  /** How often Progress is sent when {@code telemetryHeartbeatInterval} isn't a positive number. */
  static final Duration DEFAULT_HEARTBEAT_INTERVAL = Duration.ofMinutes(15);

  /** The shortest heartbeat interval a deployment can set, so destinations aren't flooded. */
  static final Duration MIN_HEARTBEAT_INTERVAL = Duration.ofMinutes(1);

  /**
   * The longest heartbeat interval a deployment can set. A destination can then treat a job that
   * has sent nothing for much longer than this as stopped, without knowing each deployment's
   * setting.
   */
  static final Duration MAX_HEARTBEAT_INTERVAL = Duration.ofMinutes(15);

  /**
   * The longest the cancel watcher waits for End to go out before it stops the worker anyway, so a
   * slow or stuck handler can't keep a cancelled job running. Leaves room for a handler that waits
   * up to a minute for its End to go out, plus the heartbeat's shutdown timeout. When the job's
   * {@code finally} overlaps with the cancel watcher, it waits the same time for that End.
   */
  static final Duration CANCEL_END_TIMEOUT = Duration.ofSeconds(90);

  private static final String CANCEL_END_THREAD_NAME = "dtp-telemetry-cancel-end";

  private final Provider<SignalHandler> exportSignalHandlerProvider;
  private final Provider<SignalHandler> importSignalHandlerProvider;
  private final boolean signalsEnabled;
  private final boolean telemetryEnabled;
  private final Duration heartbeatInterval;
  private final Optional<Duration> maxWorkerLifetime;
  private final Duration cancelEndTimeout;
  private final Monitor monitor;
  private final Clock clock;

  /**
   * The current job's telemetry, or null if it sends none. The heartbeat and the cancel watcher
   * read it from their own threads.
   */
  @Nullable private volatile JobTelemetry jobTelemetry;

  @Inject
  TransferSignals(
      @ExportSignalHandler Provider<SignalHandler> exportSignalHandlerProvider,
      @ImportSignalHandler Provider<SignalHandler> importSignalHandlerProvider,
      @Named("transferSignalEnabled") Boolean transferSignalEnabled,
      @Named("transferTelemetryEnabled") Boolean transferTelemetryEnabled,
      @Named("telemetryHeartbeatInterval") OptionalLong telemetryHeartbeatIntervalMillis,
      @Named("maxWorkerLifetimeSeconds") OptionalLong maxWorkerLifetimeSeconds,
      Monitor monitor) {
    this(
        exportSignalHandlerProvider,
        importSignalHandlerProvider,
        transferSignalEnabled,
        transferTelemetryEnabled,
        heartbeatInterval(telemetryHeartbeatIntervalMillis),
        maxWorkerLifetimeSeconds.isPresent() && maxWorkerLifetimeSeconds.getAsLong() > 0
            ? Optional.of(Duration.ofSeconds(maxWorkerLifetimeSeconds.getAsLong()))
            : Optional.empty(),
        CANCEL_END_TIMEOUT,
        monitor,
        Clock.systemUTC());
  }

  @VisibleForTesting
  TransferSignals(
      Provider<SignalHandler> exportSignalHandlerProvider,
      Provider<SignalHandler> importSignalHandlerProvider,
      boolean transferSignalEnabled,
      boolean transferTelemetryEnabled,
      Duration heartbeatInterval,
      Optional<Duration> maxWorkerLifetime,
      Duration cancelEndTimeout,
      Monitor monitor,
      Clock clock) {
    this.exportSignalHandlerProvider = exportSignalHandlerProvider;
    this.importSignalHandlerProvider = importSignalHandlerProvider;
    this.signalsEnabled = transferSignalEnabled;
    this.telemetryEnabled = transferSignalEnabled && transferTelemetryEnabled;
    this.heartbeatInterval = heartbeatInterval;
    this.maxWorkerLifetime = maxWorkerLifetime;
    this.cancelEndTimeout = cancelEndTimeout;
    this.monitor = monitor;
    this.clock = clock;
  }

  /**
   * Reads the {@code telemetryHeartbeatInterval} setting, in milliseconds: the default if it isn't
   * a positive number, and otherwise kept between {@link #MIN_HEARTBEAT_INTERVAL} and {@link
   * #MAX_HEARTBEAT_INTERVAL}.
   */
  @VisibleForTesting
  static Duration heartbeatInterval(OptionalLong millis) {
    if (millis.isEmpty() || millis.getAsLong() <= 0) {
      return DEFAULT_HEARTBEAT_INTERVAL;
    }
    Duration interval = Duration.ofMillis(millis.getAsLong());
    if (interval.compareTo(MIN_HEARTBEAT_INTERVAL) < 0) {
      return MIN_HEARTBEAT_INTERVAL;
    }
    return interval.compareTo(MAX_HEARTBEAT_INTERVAL) > 0 ? MAX_HEARTBEAT_INTERVAL : interval;
  }

  /**
   * Sends the Start signal, and decides whether this job sends telemetry.
   *
   * <p>If a handler throws a {@link RuntimeException}, the other handler still gets Start, and then
   * the exception is rethrown, so the job fails as it did before telemetry. Setting up telemetry
   * never throws.
   *
   * @param counters the copier's telemetry counters. Only read when telemetry is enabled and a
   *     handler opts in.
   */
  void sendStart(
      UUID jobId,
      AuthData exportAuthData,
      AuthData importAuthData,
      Supplier<Optional<TelemetryCounters>> counters) {
    jobTelemetry = null;
    if (!signalsEnabled) {
      monitor.info(() -> "Transfer Signal Disabled.");
      return;
    }
    JobLifeCycle started = JobLifeCycle.JOB_STARTED();
    SignalRequest request = request(jobId, started).build();
    JobTelemetry telemetry =
        telemetryEnabled ? setUpTelemetry(jobId, exportAuthData, importAuthData, counters) : null;

    RuntimeException failure;
    if (telemetry == null) {
      failure = trySendStart(exportSignalHandlerProvider, request, exportAuthData, null);
      failure = trySendStart(importSignalHandlerProvider, request, importAuthData, failure);
    } else {
      SignalRequest withTelemetry =
          telemetry.request(started).setStartTelemetry(telemetry.startTelemetry).build();
      failure =
          trySendStart(
              telemetry.exportHandler,
              telemetry.exportOptedIn ? withTelemetry : request,
              exportAuthData,
              null);
      failure =
          trySendStart(
              telemetry.importHandler,
              telemetry.importOptedIn ? withTelemetry : request,
              importAuthData,
              failure);
      if (failure == null) {
        startHeartbeat(telemetry);
      }
      // Published after the heartbeat starts, so the cancel watcher can always stop it.
      jobTelemetry = telemetry;
    }
    if (failure != null) {
      throw failure;
    }
  }

  /**
   * Sends the End signal from the worker's {@code finally} block. Never throws.
   *
   * <p>The heartbeat stops first, so no Progress follows End. Handlers that opt in get the End
   * telemetry block, unless the cancel watcher already sent them End.
   *
   * @param endStatus the job's end state, the same for every handler
   */
  void sendEnd(
      UUID jobId, AuthData exportAuthData, AuthData importAuthData, JobLifeCycle endStatus) {
    JobTelemetry telemetry = jobTelemetry;
    jobTelemetry = null;
    if (!signalsEnabled) {
      monitor.info(() -> "Transfer Signal Disabled.");
      return;
    }
    try {
      SignalRequest request = request(jobId, endStatus).build();
      if (telemetry == null) {
        trySend(exportSignalHandlerProvider, request, exportAuthData);
        trySend(importSignalHandlerProvider, request, importAuthData);
        return;
      }
      // False if the cancel watcher already claimed End for the handlers that opt in. They then
      // get no second End, so withTelemetry stays null.
      boolean claimed = telemetry.endClaimed.compareAndSet(false, true);
      SignalRequest withTelemetry = claimed ? endRequest(telemetry, endStatus) : null;
      sendEndTo(
          telemetry.exportHandler, telemetry.exportOptedIn, request, withTelemetry, exportAuthData);
      sendEndTo(
          telemetry.importHandler, telemetry.importOptedIn, request, withTelemetry, importAuthData);
      if (!claimed) {
        // The worker exits when this job ends, so let the cancel watcher finish sending End.
        awaitCancelEnd(telemetry);
      }
    } catch (RuntimeException e) {
      monitor.info(() -> "Errored while sending transfer signals.", e);
    }
  }

  /**
   * Sends End to the handlers that opt in, just before the cancel watcher stops the worker. Other
   * handlers get nothing, as before. Does nothing if the job sends no telemetry or already sent
   * End. Never throws.
   *
   * <p>End goes out on its own daemon thread, and this waits for it for at most {@link
   * #CANCEL_END_TIMEOUT}. So a slow or stuck handler can delay a cancel, but can't stop it.
   *
   * @param endReason {@link EndReason#USER_CANCELLED} for a cancelled job, or {@link
   *     EndReason#ERRORED} for a job marked as errored
   */
  void sendEndOnCancel(EndReason endReason) {
    JobTelemetry telemetry = jobTelemetry;
    if (telemetry == null || !telemetry.endClaimed.compareAndSet(false, true)) {
      return;
    }
    try {
      Thread sender =
          new Thread(() -> sendCancelEnd(telemetry, endReason), CANCEL_END_THREAD_NAME);
      sender.setDaemon(true);
      sender.start();
      awaitCancelEnd(telemetry);
      monitor.flushLogs();
    } catch (Throwable t) {
      // Catch everything: the worker is about to stop, so give up on End rather than block the
      // cancel. Counting down again is harmless if the sender already did.
      monitor.info(() -> "Couldn't send End for the cancelled job.", t);
      telemetry.cancelEndSent.countDown();
    }
  }

  /** Sends the cancel path's End to the handlers that opt in. Runs on its own thread. */
  private void sendCancelEnd(JobTelemetry telemetry, EndReason endReason) {
    try {
      JobLifeCycle endStatus =
          JobLifeCycle.builder().setState(JobLifeCycle.State.ENDED).setEndReason(endReason).build();
      sendToOptedIn(telemetry, endRequest(telemetry, endStatus));
    } catch (Throwable t) {
      // Catch everything: this thread only sends End, and the worker is stopping anyway.
      monitor.info(() -> "Errored while sending transfer signals.", t);
    } finally {
      telemetry.cancelEndSent.countDown();
    }
  }

  /** Waits, for a bounded time, until the cancel path's End has gone out. */
  private void awaitCancelEnd(JobTelemetry telemetry) {
    try {
      if (!telemetry.cancelEndSent.await(cancelEndTimeout.toMillis(), MILLISECONDS)) {
        monitor.info(() -> "End for the cancelled job didn't go out within " + cancelEndTimeout);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /** Decides whether this job sends telemetry, and returns its state if it does. Never throws. */
  @Nullable
  private JobTelemetry setUpTelemetry(
      UUID jobId,
      AuthData exportAuthData,
      AuthData importAuthData,
      Supplier<Optional<TelemetryCounters>> counters) {
    try {
      // Opt-in first: telemetry is on by default, so jobs whose handlers don't opt in should
      // cost nothing more and log nothing new.
      SignalHandler exportHandler = exportSignalHandlerProvider.get();
      SignalHandler importHandler = importSignalHandlerProvider.get();
      boolean exportOptedIn = supportsTelemetry(exportHandler);
      boolean importOptedIn = supportsTelemetry(importHandler);
      if (!exportOptedIn && !importOptedIn) {
        return null;
      }
      Optional<TelemetryCounters> jobCounters = counters.get();
      if (jobCounters.isEmpty()) {
        monitor.info(
            () ->
                "A signal handler opts in to transfer telemetry, but the copier keeps no"
                    + " telemetry counters, so this job sends none.");
        return null;
      }
      long now = clock.millis();
      StartTelemetry startTelemetry =
          StartTelemetry.builder()
              .setTimestamp(now)
              .setExpirationTimestamp(
                  maxWorkerLifetime.map(lifetime -> now + lifetime.toMillis()).orElse(null))
              .setRecurringJobId(JobMetadata.getRecurringJobId().map(UUID::toString).orElse(null))
              .build();
      return new JobTelemetry(
          jobId,
          exportHandler,
          exportAuthData,
          exportOptedIn,
          importHandler,
          importAuthData,
          importOptedIn,
          jobCounters.get(),
          startTelemetry);
    } catch (RuntimeException e) {
      monitor.info(() -> "Couldn't set up transfer telemetry, so this job sends none.", e);
      return null;
    }
  }

  /** Asks a handler once whether it wants telemetry. An exception counts as no. */
  private boolean supportsTelemetry(SignalHandler handler) {
    try {
      return handler.supportsTelemetry();
    } catch (RuntimeException e) {
      monitor.info(
          () -> "A signal handler's supportsTelemetry() threw, so it gets no telemetry.", e);
      return false;
    }
  }

  private void startHeartbeat(JobTelemetry telemetry) {
    try {
      telemetry.heartbeat =
          TelemetryHeartbeat.start(() -> sendProgress(telemetry), heartbeatInterval, monitor);
    } catch (RuntimeException e) {
      monitor.info(
          () -> "Couldn't start the telemetry heartbeat, so this job sends no Progress.", e);
    }
  }

  /**
   * Sends Progress to the handlers that opt in. Runs on the heartbeat thread. A handler's failure
   * is logged at debug level, since one that keeps failing would log the same error every interval.
   */
  private void sendProgress(JobTelemetry telemetry) {
    if (telemetry.endClaimed.get()) {
      return;
    }
    TelemetryCounters counters = telemetry.counters;
    ProgressTelemetry progress =
        ProgressTelemetry.builder()
            .setTimestamp(clock.millis())
            .setTotalItemsListed(counters.itemsListed())
            .setTotalItemsFetched(counters.itemsFetched())
            .setTotalBytesReceived(counters.bytesReceived())
            .setTotalItemsFailedToFetch(counters.itemsFailedToFetch())
            .build();
    SignalRequest request =
        telemetry.request(JobLifeCycle.IN_PROGRESS()).setProgressTelemetry(progress).build();
    if (telemetry.exportOptedIn) {
      trySendProgress(telemetry.exportHandler, request, telemetry.exportAuthData);
    }
    if (telemetry.importOptedIn) {
      trySendProgress(telemetry.importHandler, request, telemetry.importAuthData);
    }
  }

  private void trySendProgress(
      Provider<SignalHandler> handler, SignalRequest request, AuthData authData) {
    try {
      handler.get().sendSignal(request, authData, monitor);
    } catch (Exception e) {
      monitor.debug(() -> "Errored while sending transfer telemetry Progress.", e);
    }
  }

  /** Stops the heartbeat, then builds the End request for the handlers that opt in. */
  private SignalRequest endRequest(JobTelemetry telemetry, JobLifeCycle endStatus) {
    telemetry.stopHeartbeat();
    SignalRequest.Builder request = telemetry.request(endStatus);
    try {
      TelemetryCounters counters = telemetry.counters;
      request.setEndTelemetry(
          EndTelemetry.builder()
              .setTimestamp(clock.millis())
              .setTotalItemsListed(counters.itemsListed())
              .setTotalItemsFetched(counters.itemsFetched())
              .setTotalBytesReceived(counters.bytesReceived())
              .setTotalItemsFailedToFetch(counters.itemsFailedToFetch())
              .build());
    } catch (RuntimeException e) {
      monitor.info(
          () -> "Couldn't read the telemetry counters, so End has no telemetry block.", e);
    }
    return request.build();
  }

  /** Sends End to one handler: today's request, or the telemetry one if the handler opts in. */
  private void sendEndTo(
      Provider<SignalHandler> handler,
      boolean optedIn,
      SignalRequest request,
      @Nullable SignalRequest withTelemetry,
      AuthData authData) {
    if (!optedIn) {
      trySend(handler, request, authData);
    } else if (withTelemetry != null) {
      trySend(handler, withTelemetry, authData);
    }
  }

  private void sendToOptedIn(JobTelemetry telemetry, SignalRequest request) {
    if (telemetry.exportOptedIn) {
      trySend(telemetry.exportHandler, request, telemetry.exportAuthData);
    }
    if (telemetry.importOptedIn) {
      trySend(telemetry.importHandler, request, telemetry.importAuthData);
    }
  }

  /**
   * Sends one handler its Start signal. Returns the first {@link RuntimeException} seen so far, so
   * the caller can rethrow it once both handlers have been tried.
   */
  @Nullable
  private RuntimeException trySendStart(
      Provider<SignalHandler> handler,
      SignalRequest request,
      AuthData authData,
      @Nullable RuntimeException failure) {
    try {
      handler.get().sendSignal(request, authData, monitor);
    } catch (CopyExceptionWithFailureReason | IOException | RetryException e) {
      monitor.info(() -> "Errored while sending transfer signals.", e);
    } catch (RuntimeException e) {
      if (failure == null) {
        return e;
      }
      failure.addSuppressed(e);
    }
    return failure;
  }

  /** Sends one handler a signal, catching everything, so a handler can't affect the job. */
  private void trySend(Provider<SignalHandler> handler, SignalRequest request, AuthData authData) {
    try {
      handler.get().sendSignal(request, authData, monitor);
    } catch (Exception e) {
      monitor.info(() -> "Errored while sending transfer signals.", e);
    }
  }

  /** The request every handler gets, built exactly as before telemetry. */
  private static SignalRequest.Builder request(UUID jobId, JobLifeCycle jobStatus) {
    return SignalRequest.builder()
        .setJobId(jobId.toString())
        .setDataType(JobMetadata.getDataType().getDataType())
        .setJobStatus(jobStatus)
        .setExportingService(JobMetadata.getExportService())
        .setImportingService(JobMetadata.getImportService());
  }

  /** A job's telemetry state, from its Start to its End. */
  private static final class JobTelemetry {
    private final String jobId;
    private final String dataType;
    private final String exportingService;
    private final String importingService;
    private final Provider<SignalHandler> exportHandler;
    private final AuthData exportAuthData;
    private final boolean exportOptedIn;
    private final Provider<SignalHandler> importHandler;
    private final AuthData importAuthData;
    private final boolean importOptedIn;
    private final TelemetryCounters counters;
    private final StartTelemetry startTelemetry;
    /** Set by whichever sends End first: the worker's {@code finally} or the cancel watcher. */
    private final AtomicBoolean endClaimed = new AtomicBoolean();
    /** Counted down once the cancel path's End has gone out, or failed. */
    private final CountDownLatch cancelEndSent = new CountDownLatch(1);
    @Nullable private volatile TelemetryHeartbeat heartbeat;

    private JobTelemetry(
        UUID jobId,
        SignalHandler exportHandler,
        AuthData exportAuthData,
        boolean exportOptedIn,
        SignalHandler importHandler,
        AuthData importAuthData,
        boolean importOptedIn,
        TelemetryCounters counters,
        StartTelemetry startTelemetry) {
      this.jobId = jobId.toString();
      // Read once: JobMetadata is reset when the job ends, while the heartbeat or the cancel
      // watcher may still be sending.
      this.dataType = JobMetadata.getDataType().getDataType();
      this.exportingService = JobMetadata.getExportService();
      this.importingService = JobMetadata.getImportService();
      this.exportHandler = () -> exportHandler;
      this.exportAuthData = exportAuthData;
      this.exportOptedIn = exportOptedIn;
      this.importHandler = () -> importHandler;
      this.importAuthData = importAuthData;
      this.importOptedIn = importOptedIn;
      this.counters = counters;
      this.startTelemetry = startTelemetry;
    }

    private SignalRequest.Builder request(JobLifeCycle jobStatus) {
      return SignalRequest.builder()
          .setJobId(jobId)
          .setDataType(dataType)
          .setJobStatus(jobStatus)
          .setExportingService(exportingService)
          .setImportingService(importingService);
    }

    private void stopHeartbeat() {
      TelemetryHeartbeat current = heartbeat;
      if (current != null) {
        current.close();
      }
    }
  }
}
