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

public abstract class RetryStrategy {

  public boolean canTryAgain(Exception e, int tries) {
    return getNextIntervalMillis(e, tries) >= 0;
  }

  public abstract long getNextIntervalMillis(Exception e, int tries);

  public long getRemainingIntervalMillis(Exception e, int tries, long elapsedMillis) {
    return getNextIntervalMillis(e, tries) - elapsedMillis;
  }

  public static class SimpleRetryStrategy extends RetryStrategy {
    private int maxAttempts;
    private long intervalMillis;

    public SimpleRetryStrategy(int maxAttempts, long intervalMillis) {
      this.maxAttempts = maxAttempts;
      this.intervalMillis = intervalMillis;
    }

    @Override
    public long getNextIntervalMillis(Exception e, int tries) {
      // Same strategy for every exception, not a good idea in prod
      if (tries < maxAttempts) {
        return intervalMillis;
      } else {
        return -1L;
      }
    }
  }

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
    public long getNextIntervalMillis(Exception e, int tries) {
      // Same strategy for every exception, not a good idea in prod
      if (tries < maxAttempts) {
        return (long) (initialIntervalMillis * Math.pow(multiplier, tries-1));
      } else {
        return -1L;
      }
    }
  }
}
