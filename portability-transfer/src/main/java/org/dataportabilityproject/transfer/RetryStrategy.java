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

import com.google.common.base.Preconditions;

/**
 * Defines a retry strategy - i.e., how many retry attempts should be made, and at what intervals.
 */
public abstract class RetryStrategy {

  /**
   * Shows whether another retry is possible or not, given the number of tries so far
   */
  public boolean canTryAgain(int tries) {
    return getNextIntervalMillis(tries) >= 0;
  }

  /**
   * Amount of time (in milliseconds) until next retry.  Should return a negative number if
   * no more retries are left.
   */
  public abstract long getNextIntervalMillis(int tries);

  /**
   * Gets milliseconds until the next retry, given elapsed time so far
   */
  public long getRemainingIntervalMillis(int tries, long elapsedMillis) {
    long intervalMillis = getNextIntervalMillis(tries);
    Preconditions.checkArgument(intervalMillis >= 0, "No retries left");
    return intervalMillis - elapsedMillis;
  }

  /**
   * {@link RetryStrategy} that allows retries on regular intervals
   */
  public static class SimpleRetryStrategy extends RetryStrategy {
    private int maxAttempts;
    private long intervalMillis;

    public SimpleRetryStrategy(int maxAttempts, long intervalMillis) {
      this.maxAttempts = maxAttempts;
      this.intervalMillis = intervalMillis;
    }

    @Override
    public long getNextIntervalMillis(int tries) {
      if (tries < maxAttempts) {
        return intervalMillis;
      } else {
        return -1L;
      }
    }
  }

  /**
   * {@link RetryStrategy} that follows an exponential backoff strategy
   */
  public static class ExponentialBackoffRetryStrategy extends RetryStrategy {
    private int maxAttempts;
    private long initialIntervalMillis;
    private long multiplier;

    public ExponentialBackoffRetryStrategy(int maxAttempts, long initialIntervalMillis, long multiplier) {
      this.maxAttempts = maxAttempts;
      this.initialIntervalMillis = initialIntervalMillis;
      this.multiplier = multiplier;
    }

    @Override
    public long getNextIntervalMillis(int tries) {
      if (tries < maxAttempts) {
        return (long) (initialIntervalMillis * Math.pow(multiplier, tries-1));
      } else {
        return -1L;
      }
    }
  }
}
