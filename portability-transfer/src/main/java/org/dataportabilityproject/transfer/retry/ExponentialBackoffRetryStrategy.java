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

package org.dataportabilityproject.transfer.retry;

import com.google.common.base.Preconditions;

/**
 * {@link RetryStrategy} that follows an exponential backoff strategy
 */
public class ExponentialBackoffRetryStrategy implements RetryStrategy {

  private int maxAttempts;
  private long initialIntervalMillis;
  private long multiplier;

  public ExponentialBackoffRetryStrategy(int maxAttempts, long initialIntervalMillis,
      long multiplier) {
    this.maxAttempts = maxAttempts;
    this.initialIntervalMillis = initialIntervalMillis;
    this.multiplier = multiplier;
  }

  @Override
  public boolean canTryAgain(int tries) {
    return tries <= maxAttempts;
  }

  @Override
  public long getNextIntervalMillis(int tries) {
    return (long) (initialIntervalMillis * Math.pow(multiplier, tries - 1));
  }

  @Override
  public long getRemainingIntervalMillis(int tries, long elapsedMillis) {
    Preconditions.checkArgument(tries <= maxAttempts, "No retries left");
    long intervalMillis = getNextIntervalMillis(tries);
    return intervalMillis - elapsedMillis;
  }
}
