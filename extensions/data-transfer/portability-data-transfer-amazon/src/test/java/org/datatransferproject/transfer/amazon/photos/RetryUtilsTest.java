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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.IOException;

public class RetryUtilsTest {

  @Test
  void computeBackoffMillis_attempt1_around1Second() {
    // attempt=1: base = min(2^0, 60) = 1s, with jitter [1.0, 1.3) → [1000, 1300]ms
    long backoff = RetryUtils.computeBackoffMillis(1, 60.0);
    assertTrue(backoff >= 1000 && backoff <= 1300,
        "Expected [1000,1300] but got " + backoff);
  }

  @Test
  void computeBackoffMillis_attempt5_exponential16Seconds() {
    // attempt=5: base = min(2^4, 60) = 16s, with jitter [1.0, 1.3) → [16000, 20800]ms
    long backoff = RetryUtils.computeBackoffMillis(5, 60.0);
    assertTrue(backoff >= 16000 && backoff <= 20800,
        "Expected [16000,20800] but got " + backoff);
  }

  @Test
  void computeBackoffMillis_attempt7_reachesCap() {
    // attempt=7: base = min(2^6=64, 60) = 60s (capped), with jitter → [60000, 78000]ms
    long backoff = RetryUtils.computeBackoffMillis(7, 60.0);
    assertTrue(backoff >= 60000 && backoff <= 78000,
        "Expected [60000,78000] but got " + backoff);
  }

  @Test
  void computeBackoffMillis_attempt10_stillCappedAt60Seconds() {
    // attempt=10: base = min(2^9=512, 60) = 60s (capped), with jitter [60000, 78000]ms
    long backoff = RetryUtils.computeBackoffMillis(10, 60.0);
    assertTrue(backoff >= 60000 && backoff <= 78000,
        "Expected [60000,78000] but got " + backoff);
  }

  @Test
  void sleep_notInterrupted_sleeps() throws Exception {
    long start = System.currentTimeMillis();
    RetryUtils.sleep(50);
    long elapsed = System.currentTimeMillis() - start;
    assertTrue(elapsed >= 40, "Should have slept ~50ms but only " + elapsed + "ms");
  }

  @Test
  void sleep_interrupted_throwsIOException() throws Exception {
    Thread testThread = Thread.currentThread();

    // Interrupt from another thread after a short delay
    new Thread(() -> {
      try { Thread.sleep(30); } catch (InterruptedException e) { }
      testThread.interrupt();
    }).start();

    IOException ex = assertThrows(IOException.class, () -> RetryUtils.sleep(2000));
    assertTrue(ex.getMessage().contains("interrupted"));
    // Clear interrupt flag
    Thread.interrupted();
  }
}
