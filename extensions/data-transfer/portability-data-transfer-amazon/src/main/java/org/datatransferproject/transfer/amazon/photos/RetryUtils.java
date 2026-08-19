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

package org.datatransferproject.transfer.amazon.photos;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Retry utilities for Amazon Photos multipart upload polling.
 */
final class RetryUtils {

  private RetryUtils() {}

  /**
   * Computes an exponential backoff with jitter, in millis.
   * Base grows as 2^(attempt-1) seconds (1, 2, 4, 8, 16, 32, ...), capped at {@code maxSeconds},
   * then scaled by a random factor in [1.0, 1.3) to spread out retries and avoid a thundering herd.
   */
  static long computeBackoffMillis(int attempt, double maxSeconds) {
    double base = Math.min(Math.pow(2, attempt - 1), maxSeconds);
    double jitter = 1.0 + ThreadLocalRandom.current().nextDouble() * 0.3;
    return Math.round(base * jitter * 1000);
  }

  /**
   * Sleeps for the given duration. Throws IOException if the thread is interrupted
   * (e.g. due to job cancellation via DTP's worker thread interrupt mechanism).
   */
  static void sleep(long millis) throws IOException {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Polling interrupted", e);
    }
  }
}
