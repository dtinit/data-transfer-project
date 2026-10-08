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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.datatransferproject.api.launcher.Monitor;

/**
 * Runs a task repeatedly on its own daemon thread, for transfer telemetry.
 *
 * <p>The task never runs on the caller's thread, so a slow or failing task can't hold up the
 * transfer. The first run starts one interval after {@link #start}, and each later run starts one
 * interval after the previous run ends. A run that throws is logged and doesn't stop later runs.
 */
final class TelemetryHeartbeat implements AutoCloseable {

  static final String THREAD_NAME = "dtp-telemetry-heartbeat";
  private static final Duration DEFAULT_SHUTDOWN_TIMEOUT = Duration.ofSeconds(5);

  private final ScheduledExecutorService executor;
  private final Duration shutdownTimeout;
  private final Monitor monitor;

  private TelemetryHeartbeat(Duration shutdownTimeout, Monitor monitor) {
    this.shutdownTimeout = shutdownTimeout;
    this.monitor = monitor;
    this.executor = Executors.newSingleThreadScheduledExecutor(TelemetryHeartbeat::newThread);
  }

  /** Starts running {@code task} every {@code interval}, which must be at least 1 millisecond. */
  static TelemetryHeartbeat start(Runnable task, Duration interval, Monitor monitor) {
    return start(task, interval, DEFAULT_SHUTDOWN_TIMEOUT, monitor);
  }

  @VisibleForTesting
  static TelemetryHeartbeat start(
      Runnable task, Duration interval, Duration shutdownTimeout, Monitor monitor) {
    long intervalMillis = interval.toMillis();
    checkArgument(intervalMillis > 0, "interval must be at least 1 millisecond: %s", interval);
    TelemetryHeartbeat heartbeat = new TelemetryHeartbeat(shutdownTimeout, monitor);
    heartbeat.executor.scheduleWithFixedDelay(
        () -> heartbeat.runSafely(task), intervalMillis, intervalMillis, MILLISECONDS);
    return heartbeat;
  }

  /**
   * Stops the heartbeat. Interrupts a run in progress and waits for it to end, but never longer
   * than the shutdown timeout (5 seconds by default), even if the run ignores the interrupt.
   */
  @Override
  public void close() {
    executor.shutdownNow();
    try {
      if (!executor.awaitTermination(shutdownTimeout.toMillis(), MILLISECONDS)) {
        monitor.info(() -> "Telemetry heartbeat didn't stop within " + shutdownTimeout);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void runSafely(Runnable task) {
    try {
      task.run();
    } catch (Throwable t) {
      // Catch everything: an exception that escapes cancels all later runs.
      monitor.info(() -> "Telemetry heartbeat run failed", t);
    }
  }

  private static Thread newThread(Runnable runnable) {
    Thread thread = new Thread(runnable, THREAD_NAME);
    thread.setDaemon(true);
    return thread;
  }
}
