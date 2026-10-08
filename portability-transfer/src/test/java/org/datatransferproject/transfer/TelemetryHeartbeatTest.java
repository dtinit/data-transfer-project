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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.datatransferproject.api.launcher.Monitor;
import org.junit.jupiter.api.Test;

public class TelemetryHeartbeatTest {

  private static final Duration INTERVAL = Duration.ofMillis(10);
  private static final Monitor MONITOR = new Monitor() {};

  @Test
  public void runsTheTaskRepeatedly() throws Exception {
    CountDownLatch threeRuns = new CountDownLatch(3);

    try (TelemetryHeartbeat heartbeat =
        TelemetryHeartbeat.start(threeRuns::countDown, INTERVAL, MONITOR)) {
      assertTrue(threeRuns.await(10, SECONDS));
    }
  }

  @Test
  public void waitsOneIntervalBeforeTheFirstRun() throws Exception {
    AtomicInteger runs = new AtomicInteger();

    try (TelemetryHeartbeat heartbeat =
        TelemetryHeartbeat.start(runs::incrementAndGet, Duration.ofHours(1), MONITOR)) {
      Thread.sleep(100);
      assertEquals(0, runs.get());
    }
  }

  @Test
  public void keepsRunning_afterARunThrows() throws Exception {
    assertKeepsRunningAfter(new IllegalStateException("Test exception"));
  }

  @Test
  public void keepsRunning_afterARunThrowsAnError() throws Exception {
    assertKeepsRunningAfter(new AssertionError("Test error"));
  }

  @Test
  public void logsAFailedRun() throws Exception {
    Monitor monitor = mock(Monitor.class);
    IllegalStateException failure = new IllegalStateException("Test exception");

    try (TelemetryHeartbeat heartbeat =
        TelemetryHeartbeat.start(
            () -> {
              throw failure;
            },
            INTERVAL,
            monitor)) {
      verify(monitor, timeout(10_000).atLeastOnce()).info(any(), same(failure));
    }
  }

  @Test
  public void runsOnANamedDaemonThread_thatEndsOnClose() throws Exception {
    AtomicReference<Thread> runThread = new AtomicReference<>();
    CountDownLatch ran = new CountDownLatch(1);
    TelemetryHeartbeat heartbeat =
        TelemetryHeartbeat.start(
            () -> {
              runThread.set(Thread.currentThread());
              ran.countDown();
            },
            INTERVAL,
            MONITOR);
    assertTrue(ran.await(10, SECONDS));

    heartbeat.close();

    Thread thread = runThread.get();
    assertNotSame(Thread.currentThread(), thread);
    assertTrue(thread.isDaemon());
    assertEquals(TelemetryHeartbeat.THREAD_NAME, thread.getName());
    thread.join(10_000);
    assertFalse(thread.isAlive());
  }

  @Test
  public void aBlockedRun_doesNotBlockTheCaller_andCloseInterruptsIt() throws Exception {
    CountDownLatch running = new CountDownLatch(1);
    CountDownLatch interrupted = new CountDownLatch(1);
    TelemetryHeartbeat heartbeat =
        TelemetryHeartbeat.start(
            () -> {
              running.countDown();
              try {
                new CountDownLatch(1).await();
              } catch (InterruptedException e) {
                interrupted.countDown();
              }
            },
            INTERVAL,
            MONITOR);
    assertTrue(running.await(10, SECONDS));

    heartbeat.close();

    assertTrue(interrupted.await(10, SECONDS));
  }

  @Test
  public void close_givesUpAfterTheTimeout_whenARunIgnoresInterrupts() throws Exception {
    CountDownLatch running = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    TelemetryHeartbeat heartbeat =
        TelemetryHeartbeat.start(
            () -> {
              running.countDown();
              awaitIgnoringInterrupts(release);
            },
            INTERVAL,
            Duration.ofMillis(100),
            MONITOR);
    assertTrue(running.await(10, SECONDS));

    long startNanos = System.nanoTime();
    heartbeat.close();
    Duration closeTime = Duration.ofNanos(System.nanoTime() - startNanos);

    release.countDown();
    assertTrue(closeTime.compareTo(Duration.ofSeconds(5)) < 0, "close took " + closeTime);
  }

  @Test
  public void stopsRunning_afterClose() throws Exception {
    AtomicInteger runs = new AtomicInteger();
    CountDownLatch ran = new CountDownLatch(1);
    TelemetryHeartbeat heartbeat =
        TelemetryHeartbeat.start(
            () -> {
              runs.incrementAndGet();
              ran.countDown();
            },
            INTERVAL,
            MONITOR);
    assertTrue(ran.await(10, SECONDS));

    heartbeat.close();
    int runsAtClose = runs.get();
    Thread.sleep(100);

    assertEquals(runsAtClose, runs.get());
  }

  @Test
  public void start_rejectsAnIntervalUnderOneMillisecond() {
    assertThrows(
        IllegalArgumentException.class,
        () -> TelemetryHeartbeat.start(() -> {}, Duration.ZERO, MONITOR));
    assertThrows(
        IllegalArgumentException.class,
        () -> TelemetryHeartbeat.start(() -> {}, Duration.ofNanos(1), MONITOR));
  }

  private static void assertKeepsRunningAfter(Throwable failure) throws Exception {
    AtomicInteger runs = new AtomicInteger();
    CountDownLatch runAfterFailure = new CountDownLatch(1);
    Runnable task =
        () -> {
          if (runs.incrementAndGet() == 1) {
            throwUnchecked(failure);
          }
          runAfterFailure.countDown();
        };

    try (TelemetryHeartbeat heartbeat = TelemetryHeartbeat.start(task, INTERVAL, MONITOR)) {
      assertTrue(runAfterFailure.await(10, SECONDS));
    }
  }

  private static void throwUnchecked(Throwable failure) {
    if (failure instanceof RuntimeException) {
      throw (RuntimeException) failure;
    }
    throw (Error) failure;
  }

  private static void awaitIgnoringInterrupts(CountDownLatch latch) {
    while (true) {
      try {
        latch.await();
        return;
      } catch (InterruptedException e) {
        // Keep waiting, like a run that ignores interrupts.
      }
    }
  }
}
