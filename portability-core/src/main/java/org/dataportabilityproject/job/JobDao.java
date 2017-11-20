/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.job;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.Map;
import org.dataportabilityproject.cloud.interfaces.PersistentKeyValueStore;

/**
 * A data acccess object that provides functionality to manage persisted data for portability jobs.
 */
public class JobDao {
  // Keys for specific values in data store
  private static final String ID_DATA_KEY = "UUID";
  private static final String KEY_SEPERATOR = "-";

  /**
   * The current state of the job.
   * <p>
   * The value UNASSIGNED indicates the client has sent a request for a worker to be assigned before
   * sending all the data required for the job.
   * The value READY_TO_PROCESS indicates the client has submitted all data required, such as the
   * encrypted auth data, in order to begin processing the portability job.
   */
  enum JobState {
    UNASSIGNED,
    READY_TO_PROCESS
  }

  private final PersistentKeyValueStore storage;

  public JobDao(PersistentKeyValueStore storage) {
    this.storage = storage;
  }

  /** Returns a job in unassigned state. */
  public String findUnassignedJob() {
    String key = storage.getFirst(JobState.UNASSIGNED.name());
    if (key != null) {
      return getIdFromKey(key);
    }
    return null;
  }

  /** Looks up a job that is ready to be processed. */
  public PortabilityJob lookupJob(String id) {
    Preconditions.checkNotNull(id);
    Map<String, Object> data = storage.get(createKey(JobState.READY_TO_PROCESS, id));
    if (data == null || data.isEmpty()) {
      return null;
    }
    return PortabilityJob.mapToJob(data);
  }

  /** Inserts a new entry for the given {@code id} in storage in unassigned state.*/
  public void insertJobInUnassignedState(String id) throws IOException {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(id));
    String unassignedKey = createKey(JobState.UNASSIGNED, id);
    Map<String, Object> existing = storage.get(unassignedKey);
    Preconditions.checkArgument(existing == null, "Attempting to insert an already existing job");
    // Store the updated job info
    Map<String, Object> data = PortabilityJob.builder().setId(id).build().asMap();
    storage.put(unassignedKey, data);
  }

  /**
   * Replaces a unassigned entry in storage with the provided {@code job} in ready state.
   * TODO: Migrate updateJob() calls to use this method once the whole poll-based flow is implemented
   */
  public void updateJobToReady(PortabilityJob job) throws IOException {
    String unassignedKey = createKey(JobState.UNASSIGNED, job.id());
    Map<String, Object> existingUnassignedJob = storage.get(unassignedKey);
    Preconditions.checkArgument(existingUnassignedJob != null,
        "Attempting to update a non-existent unassigned job");
    // Store the updated job info in ready state
    String readyKey = createKey(JobState.READY_TO_PROCESS, job.id());
    // Check that job doesn't already exist in ready state
    Map<String, Object> existingReadyJob = storage.get(readyKey);
    Preconditions
        .checkArgument(existingReadyJob == null, "Attempting to insert an already existing job");
    storage.put(readyKey, job.asMap()); // Store entry in ready state
    storage.delete(unassignedKey); // TODO: Determine whether to Tombstone or Delete from unassigned once it's entered in ready state
  }

  /**
   * Inserts a new {@code job} in storage.
   * @deprecated Remove when worker flow is implemented
   */
  @Deprecated
  public void insertJob(PortabilityJob job) throws IOException {
    Map<String, Object> existing = storage.get(job.id());
    Preconditions.checkArgument(existing == null, "Attempting to insert an already existing job");
    // Store the updated job info
    Map<String, Object> data = job.asMap();
    storage.put(getString(data, ID_DATA_KEY), data);
  }

  /**
   * Returns the information for a user job or null if not found.
   * @deprecated Remove when worker flow is implemented
   */
  @Deprecated
  public PortabilityJob findExistingJob(String id) {
    Preconditions.checkNotNull(id);
    Map<String, Object> data = storage.get(id);
    if (data == null || data.isEmpty()) {
      return null;
    }
    return PortabilityJob.mapToJob(data);
  }

  /**
   * Updates an existing {@code job} in storage.
   * @deprecated Remove when worker flow is implemented
   */
  @Deprecated
  public void updateJob(PortabilityJob job) throws IOException {
    Map<String, Object> existing = storage.get(job.id());
    Preconditions.checkArgument(existing != null, "Attempting to update a non-existent job");
    // Store the updated job info
    Map<String, Object> data = job.asMap();
    storage.put(getString(data, ID_DATA_KEY), data);
  }

  // TODO: come up with a better scheme for indexing state
  private static String createKey(JobState state, String id) {
    Preconditions.checkArgument(!id.contains(KEY_SEPERATOR));
    return String.format("%s%s%s", state.name(), KEY_SEPERATOR, id);
  }

  private static String getIdFromKey(String key) {
    Preconditions.checkArgument(key.contains(KEY_SEPERATOR));
    return key.split(KEY_SEPERATOR)[1];
  }

  private static String getString(Map<String, Object> map, String key) {
    return (String) map.get(key);
  }

}
