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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.datatransferproject.types.common.models.DataVertical.MEDIA;
import static org.datatransferproject.types.common.models.DataVertical.PHOTOS;
import static org.datatransferproject.types.common.models.DataVertical.VIDEOS;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.google.GoogleTransferExtension;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.spi.transfer.provider.SignalHandler;
import org.datatransferproject.spi.transfer.provider.SignalRequest;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

/**
 * Reports transfer telemetry to Google for transfers into Google Photos.
 *
 * <p>It opts in to telemetry, and only reports jobs whose destination is Google and whose data is
 * photos, videos or media, since it reports with the user's Google Photos import token. It never
 * reports a job that Google exports, so a worker that exports from Google makes no calls.
 *
 * <p>It reports the signals that carry telemetry, and an End without its telemetry block for a job
 * whose Start had telemetry: the worker sends that End when it can't read its counters. Other
 * signals, such as those a worker sends when its host turns telemetry off, aren't reported.
 *
 * <p>Reports go out on the handler's own daemon thread, in the order they arrive, so the transfer
 * never waits for one. A Progress that's still waiting to go out is replaced by a newer one, and
 * dropped when End arrives, since End has the final counts; so is a Progress that arrives after
 * End. Because the worker exits as soon as the job ends, {@link #sendSignal} waits for End to go
 * out, for at most {@link #END_TIMEOUT}. So that End isn't held up, Progress isn't retried, and
 * Start stops retrying once End arrives. Exceptions are logged, never thrown.
 */
public final class GoogleSignalHandler implements SignalHandler<TokensAndUrlAuthData> {

  /** The longest {@link #sendSignal} waits for End to go out. */
  static final Duration END_TIMEOUT = Duration.ofSeconds(60);

  private static final String THREAD_NAME = "dtp-google-telemetry";

  /** The data types that DTP imports into Google Photos. */
  private static final ImmutableSet<String> PHOTOS_DATA_TYPES =
      ImmutableSet.of(PHOTOS.getDataType(), VIDEOS.getDataType(), MEDIA.getDataType());

  // The reporting thread stops after this long without work, so an idle handler keeps no thread.
  private static final long KEEP_ALIVE_SECONDS = 30;

  private final TelemetrySender sender;
  private final Duration endTimeout;
  private final Monitor monitor;
  private final ExecutorService executor;

  /** The newest Progress waiting to go out, if any. */
  private final AtomicReference<Report> pendingProgress = new AtomicReference<>();

  /** The job whose Start had telemetry, if any. */
  @Nullable private volatile String telemetryJobId;

  /** The job whose End has arrived, if any. */
  @Nullable private volatile String endedJobId;

  public GoogleSignalHandler(GoogleCredentialFactory credentialFactory, Monitor monitor) {
    this(new GoogleTelemetryClient(credentialFactory, monitor), END_TIMEOUT, monitor);
  }

  @VisibleForTesting
  GoogleSignalHandler(TelemetrySender sender, Duration endTimeout, Monitor monitor) {
    this.sender = sender;
    this.endTimeout = endTimeout;
    this.monitor = monitor;
    this.executor =
        new ThreadPoolExecutor(
            0,
            1,
            KEEP_ALIVE_SECONDS,
            SECONDS,
            new LinkedBlockingQueue<>(),
            GoogleSignalHandler::newThread);
  }

  /** Opts in for every job. {@link #sendSignal} decides which signals are reported. */
  @Override
  public boolean supportsTelemetry() {
    return true;
  }

  @Override
  public void sendSignal(SignalRequest signalRequest, AuthData authData, Monitor unusedMonitor) {
    try {
      if (!(authData instanceof TokensAndUrlAuthData) || !isImportIntoGooglePhotos(signalRequest)) {
        return;
      }
      String jobId = signalRequest.jobId();
      Report report = new Report(signalRequest, (TokensAndUrlAuthData) authData);
      if (signalRequest.startTelemetry() != null) {
        telemetryJobId = jobId;
        executor.execute(() -> send(report, () -> !jobId.equals(endedJobId)));
      } else if (signalRequest.progressTelemetry() != null) {
        // Replaces the Progress that's waiting, if any, which already has a send queued.
        if (pendingProgress.getAndSet(report) == null) {
          executor.execute(this::sendPendingProgress);
        }
      } else if (isEnd(signalRequest)
          && (signalRequest.endTelemetry() != null || jobId.equals(telemetryJobId))) {
        // From now on, the job's Start stops retrying and its Progress isn't sent.
        endedJobId = jobId;
        sendAndWait(report);
      }
    } catch (RuntimeException e) {
      monitor.info(() -> "Couldn't report transfer telemetry to Google.", e);
    }
  }

  private static boolean isImportIntoGooglePhotos(SignalRequest request) {
    return isGoogle(request.importingService())
        && !isGoogle(request.exportingService())
        && PHOTOS_DATA_TYPES.stream().anyMatch(type -> type.equalsIgnoreCase(request.dataType()));
  }

  private static boolean isGoogle(String service) {
    return GoogleTransferExtension.SERVICE_ID.equalsIgnoreCase(service);
  }

  private static boolean isEnd(SignalRequest request) {
    return request.jobStatus().state() == JobLifeCycle.State.ENDED;
  }

  private void sendPendingProgress() {
    Report progress = pendingProgress.getAndSet(null);
    // A Progress for a job whose End has arrived, whether it was waiting or arrived later, isn't
    // needed: End has the final counts.
    if (progress != null && !progress.request.jobId().equals(endedJobId)) {
      // Not retried: the next Progress, or End, replaces it.
      send(progress, () -> false);
    }
  }

  /** Sends End, and waits for it to go out, for at most the End timeout. */
  private void sendAndWait(Report end) {
    CountDownLatch sent = new CountDownLatch(1);
    executor.execute(
        () -> {
          try {
            send(end, () -> true);
          } finally {
            sent.countDown();
          }
        });
    try {
      if (!sent.await(endTimeout.toMillis(), MILLISECONDS)) {
        monitor.info(() -> "Transfer telemetry End didn't go out to Google within " + endTimeout);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void send(Report report, BooleanSupplier mayRetry) {
    try {
      sender.send(report.request, report.authData, mayRetry);
    } catch (Throwable t) {
      // Catches everything: on this thread, an uncaught throwable reaches the worker's default
      // handler, which can exit the worker in the middle of the transfer.
      monitor.info(() -> "Couldn't report transfer telemetry to Google.", t);
    }
  }

  private static Thread newThread(Runnable runnable) {
    Thread thread = new Thread(runnable, THREAD_NAME);
    thread.setDaemon(true);
    return thread;
  }

  /** A signal to report, and the auth data to report it with. */
  private static final class Report {
    private final SignalRequest request;
    private final TokensAndUrlAuthData authData;

    private Report(SignalRequest request, TokensAndUrlAuthData authData) {
      this.request = request;
      this.authData = authData;
    }
  }
}
