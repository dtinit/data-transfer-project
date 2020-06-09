/*
 * Copyright 2018 The Data Transfer Project Authors.
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

package org.datatransferproject.types.transfer.retry;

import static java.lang.Thread.currentThread;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import org.datatransferproject.api.launcher.Monitor;

/**
 * Class for retrying a {@link Callable} given a {@link RetryStrategyLibrary}.
 *
 * @param <T> The type that the inner {@link Callable} returns.
 */
public class RetryingCallable<T> implements Callable<T> {

  private final Callable<T> callable;
  private final RetryStrategyLibrary retryStrategyLibrary;
  private final Clock clock;
  private final Monitor monitor;
  private final String dataType;
  private final String service;

  private volatile int attempts;
  private volatile Exception mostRecentException;

  public RetryingCallable(
      Callable<T> callable,
      RetryStrategyLibrary retryStrategyLibrary,
      Clock clock,
      Monitor monitor,
      String dataType,
      String service) {
    this.callable = callable;
    this.retryStrategyLibrary = retryStrategyLibrary;
    this.clock = clock;
    this.monitor = monitor;
    this.dataType = dataType;
    this.service = service;
    this.attempts = 0;
  }

  /**
   * Tries to call the {@link Callable} given the class's {@link RetryStrategyLibrary}.
   *
   * @return Whatever is returned by the {@link Callable}.
   */
  @Override
  public T call() throws RetryException {
    while (true) {
      Instant start = clock.instant();
      attempts++;
      try {
        monitor.debug(
            () ->
                String.format(
                    "Attempt %d started, service: %s, dataType: %s",
                    attempts, service, dataType));
        return callable.call();
      } catch (Exception e) {
        mostRecentException = e;
        monitor.info(() -> "RetryingCallable caught an exception", e);
        long elapsedMillis = Duration.between(start, clock.instant()).toMillis();
        // TODO: do we want to reset anything (eg, number of retries) if we see a different
        // RetryStrategy?
        RetryStrategy strategy = retryStrategyLibrary.checkoutRetryStrategy(e);
        monitor.debug(
            () ->
                String.format(
                    "Attempt %d failed, using retry strategy: %s, service: %s, dataType: %s",
                    attempts, strategy.toString(), service, dataType));
        if (strategy.canTryAgain(attempts)) {
          long nextAttemptIntervalMillis =
              strategy.getRemainingIntervalMillis(attempts, elapsedMillis);
          monitor.debug(
              () ->
                  String.format(
                      "Strategy has %d remainingIntervalMillis after %d elapsedMillis",
                      nextAttemptIntervalMillis, elapsedMillis));
          if (nextAttemptIntervalMillis > 0L) {
            try {
              Thread.sleep(nextAttemptIntervalMillis);
              // wait is now complete, retry
            } catch (InterruptedException ie) {
              currentThread().interrupt();
              throw new RetryException(attempts, mostRecentException);
            }
          }
        } else {
          monitor.debug(
              () ->
                  String.format("Strategy canTryAgain returned false after %d retries", attempts));
          throw new RetryException(attempts, mostRecentException);
        }
      }
    }
  }
}
