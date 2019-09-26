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

import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Truth;
import java.util.Map;
import org.junit.Test;

public class LocalJobStoreTest {

  private final String ITEM_NAME = "item1";

  private final LocalJobStore localJobStore = new LocalJobStore();

  @Test
  public void addingNullDoesNotChangeTheCurrentCountsTest() {
    localJobStore.addCounts(null);
    assertTrue(localJobStore.getCounts().isEmpty());
  }

  @Test
  public void canAddNewKeysToTheCurrentCountsTest() {
    addItemToJobStoreCounts(ITEM_NAME);

    final Map<String, Integer> counts = localJobStore.getCounts();
    Truth.assertThat(counts.size()).isEqualTo(1);
    Truth.assertThat(counts.get(ITEM_NAME)).isEqualTo(1);
  }

  @Test
  public void canAddExistingKeysToCurrentCountsTest() {
    addItemToJobStoreCounts(ITEM_NAME);
    addItemToJobStoreCounts(ITEM_NAME);

    final Map<String, Integer> counts = localJobStore.getCounts();
    Truth.assertThat(counts.size()).isEqualTo(1);
    Truth.assertThat(counts.get(ITEM_NAME)).isEqualTo(2);
  }

  private void addItemToJobStoreCounts(final String itemName) {
    localJobStore.addCounts(new ImmutableMap.Builder<String, Integer>().put(itemName, 1).build());
  }
}
