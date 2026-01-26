/*
 * Copyright 2025 The Data Transfer Project Authors.
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
 *
 */

package org.datatransferproject.datatransfer.synology.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.synology.exceptions.SynologyImportException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SynologyMediaAlbumBinderConcurrencyTest {
  @Mock private Monitor monitor;

  @Test
  public void shouldProcessAllItems() throws InterruptedException {
    int latch_timeout_sec = 5;
    int numPutJobs = 10;
    int numWhenAlbumReadyJobs = 10;
    String albumId = "1";
    String newAlbumId = "10";

    ConcurrentLinkedQueue<String> processedItems = new ConcurrentLinkedQueue<>();
    OnAlbumReadyCallback<String> addToProcessedItems =
        (__, itemKey, ___) -> {
          processedItems.add(itemKey);
        };
    SynologyMediaAlbumBinder<String> binder =
        new SynologyMediaAlbumBinder<>(addToProcessedItems, monitor);

    ExecutorService executor = Executors.newFixedThreadPool(10);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(numPutJobs + numWhenAlbumReadyJobs);

    BiFunction<String, Integer, String> getItemId =
        (offset, itemId) -> String.valueOf(Integer.parseInt(offset) + itemId);

    UUID jobId = UUID.randomUUID();

    for (int i = 0; i < numPutJobs; i++) {
      final String itemId = getItemId.apply(albumId, i);
      executor.submit(
          () -> {
            try {
              startLatch.await();
              binder.put(albumId, itemId, jobId);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } catch (SynologyImportException e) {
              throw new RuntimeException(e);
            } finally {
              doneLatch.countDown();
            }
          });
    }

    for (int i = 0; i < numWhenAlbumReadyJobs; i++) {
      executor.submit(
          () -> {
            try {
              startLatch.await();
              binder.whenAlbumReady(albumId, newAlbumId, jobId);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } catch (SynologyImportException e) {
              throw new RuntimeException(e);
            } finally {
              doneLatch.countDown();
            }
          });
    }

    startLatch.countDown();
    assertTrue(doneLatch.await(latch_timeout_sec, TimeUnit.SECONDS));
    executor.shutdown();

    assertEquals(numPutJobs, processedItems.size());
    for (int i = 0; i < 10; i++) {
      String itemId = getItemId.apply(albumId, i);
      assertTrue(processedItems.contains(itemId), "Missing item " + itemId);
    }
  }
}
