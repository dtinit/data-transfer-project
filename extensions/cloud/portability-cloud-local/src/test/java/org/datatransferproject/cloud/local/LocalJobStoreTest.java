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

package org.datatransferproject.cloud.local;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Truth;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class LocalJobStoreTest {

  private final String ITEM_NAME = "item1";
  private final UUID jobId = UUID.randomUUID();
  private final LocalJobStore localJobStore = new LocalJobStore();

  @Test
  public void addingNullDoesNotChangeTheCurrentCountsTest() {
    localJobStore.addCounts(jobId, null);
    assertTrue(localJobStore.getCounts(jobId).isEmpty());
  }

  @Test
  public void canAddNewKeysToTheCurrentCountsTest() {
    addItemToJobStoreCounts(ITEM_NAME);
    final Map<String, Integer> counts = localJobStore.getCounts(jobId);
    Truth.assertThat(counts.size()).isEqualTo(1);
    Truth.assertThat(counts.get(ITEM_NAME)).isEqualTo(1);
  }

  @Test
  public void canAddExistingKeysToCurrentCountsTest() {
    addItemToJobStoreCounts(ITEM_NAME);
    addItemToJobStoreCounts(ITEM_NAME);

    final Map<String, Integer> counts = localJobStore.getCounts(jobId);
    Truth.assertThat(counts.size()).isEqualTo(1);
    Truth.assertThat(counts.get(ITEM_NAME)).isEqualTo(2);
  }

  private void addItemToJobStoreCounts(final String itemName) {
    localJobStore.addCounts(
        jobId, new ImmutableMap.Builder<String, Integer>().put(itemName, 1).build());
  }
}
