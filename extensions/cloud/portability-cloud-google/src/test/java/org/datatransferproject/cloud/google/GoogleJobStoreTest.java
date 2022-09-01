/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.datatransferproject.cloud.google;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Truth;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GoogleJobStoreTest {

  private static final String ITEM_NAME = "item1";
  private static final UUID JOB_ID = UUID.randomUUID();
  private static LocalDatastoreHelper localDatastoreHelper;
  private static Datastore datastore;

  @Mock
  private static GoogleTempFileStore tempFileStore;
  private static GoogleJobStore googleJobStore;


  @BeforeEach
  public void setUp() throws IOException, InterruptedException {
    localDatastoreHelper = LocalDatastoreHelper.create();
    localDatastoreHelper.start();
    System.setProperty("DATASTORE_EMULATOR_HOST", "localhost:" + localDatastoreHelper.getPort());

    datastore = localDatastoreHelper.getOptions().getService();
    googleJobStore = new GoogleJobStore(datastore, tempFileStore, new ObjectMapper());

  }

  @Test
  public void getDataKeyName() throws Exception {
    assertEquals(
        JOB_ID + "-tempTaskData",
        GoogleJobStore.getDataKeyName(JOB_ID, "tempTaskData"));
    assertEquals(
        JOB_ID + "-tempCalendarData",
        GoogleJobStore.getDataKeyName(JOB_ID, "tempCalendarData"));
  }

  @Test
  public void addingNullDoesNotChangeTheCurrentCountsTest() throws IOException {
    googleJobStore.addCounts(JOB_ID, null);
    assertTrue(googleJobStore.getCounts(JOB_ID).isEmpty());
  }

  @Test
  public void canAddNewKeysToTheCurrentCountsTest() throws IOException {
    addItemToJobStoreCounts(ITEM_NAME);
    final Map<String, Integer> counts = googleJobStore.getCounts(JOB_ID);
    Truth.assertThat(counts.size()).isEqualTo(1);
    Truth.assertThat(counts.get(ITEM_NAME)).isEqualTo(1);
  }

  @Test
  public void canAddExistingKeysToCurrentCountsTest() throws IOException {
    addItemToJobStoreCounts(ITEM_NAME);
    addItemToJobStoreCounts(ITEM_NAME);

    final Map<String, Integer> counts = googleJobStore.getCounts(JOB_ID);
    Truth.assertThat(counts.size()).isEqualTo(1);
    Truth.assertThat(counts.get(ITEM_NAME)).isEqualTo(2);
  }

  private void addItemToJobStoreCounts(final String itemName) throws IOException {
    googleJobStore.addCounts(
        JOB_ID, new ImmutableMap.Builder<String, Integer>().put(itemName, 1).build());
  }

}
