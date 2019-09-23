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

/**
 * {@link RetryStrategy} that doesn't allow for any more retries.  Useful for fatal errors.
 */
public class NoRetryStrategy implements RetryStrategy {

  public NoRetryStrategy() {}

  @Override
  public boolean canTryAgain(int tries) {
    return false;
  }

  @Override
  public long getNextIntervalMillis(int tries) {
    return -1L;
  }

  @Override
  public long getRemainingIntervalMillis(int tries, long elapsedMillis) {
    return -1L;
  }

  @Override
  public String toString() {
    return "NoRetryStrategy{}";
  }

}
