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

package org.dataportabilityproject.transfer;

import static java.lang.Thread.currentThread;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;

public class RetryingCallable<T> implements Callable<T> {

  private final Callable<T> callable;
  private final RetryStrategy retryStrategy;
  private final Clock clock;

  private volatile int attempts;
  private volatile Exception mostRecentException;

  public RetryingCallable(Callable<T> callable,
      RetryStrategy retryStrategy,
      Clock clock) {
    this.callable = callable;
    this.retryStrategy = retryStrategy;
    this.clock = clock;
    this.attempts = 0;
  }

  @Override
  public T call() throws RetryException {
    Instant start = clock.instant();
    while (true) {
      attempts++;
      try {
        return callable.call();
      } catch (InterruptedException e) {
        currentThread().interrupt();
        throw new RetryException(attempts, mostRecentException != null ? mostRecentException : e);
      } catch (Exception e) {
        mostRecentException = e;
        long elapsedMillis = Duration.between(start, clock.instant()).toMillis();
        long nextAttemptIntervalMillis = retryStrategy
            .getRemainingIntervalMillis(e, attempts, elapsedMillis);
        if (nextAttemptIntervalMillis >= 0) {
          try {
            Thread.sleep(nextAttemptIntervalMillis);
          } catch (InterruptedException ie) {
            currentThread().interrupt();
            throw new RetryException(attempts, mostRecentException);
          }
        } else {
          throw new RetryException(attempts, mostRecentException);
        }
      }
    }
  }
}
