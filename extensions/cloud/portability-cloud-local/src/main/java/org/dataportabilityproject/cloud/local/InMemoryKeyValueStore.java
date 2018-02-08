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
package org.dataportabilityproject.cloud.local;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.LegacyPortabilityJob;
import org.dataportabilityproject.spi.cloud.types.LegacyPortabilityJob.JobState;
import org.dataportabilityproject.spi.cloud.types.OldPortabilityJobConverter;

/**
 * An in-memory {@link JobStore} implementation that uses a concurrent map as its
 * store.
 */
public final class InMemoryKeyValueStore implements JobStore {
  private final ConcurrentHashMap<String, Map<String, Object>> map;
  private final boolean encryptedFlow;

  public InMemoryKeyValueStore(boolean encryptedFlow) {
    this.map = new ConcurrentHashMap<>();
    this.encryptedFlow = encryptedFlow;
  }

  /**
   * Inserts a new {@link LegacyPortabilityJob} keyed by its job ID in the map.
   *
   * <p>To update an existing {@link LegacyPortabilityJob} instead, use {@link #update}.
   *
   * @throws IOException if a job already exists for {@code jobId}, or if there was a different
   * problem inserting the job.
   */
  @Override
  public synchronized void create(LegacyPortabilityJob job) throws IOException {
    Preconditions.checkNotNull(job.id());
    String jobId = job.id();
    if (map.get(jobId) != null) {
      throw new IOException("An entry already exists for job " + jobId);
    }
    map.put(jobId, job.asMap());
  }

  /**
   * Finds the {@link LegacyPortabilityJob} keyed by {@code jobId} in the map, or null if not found.
   */
  @Override
  public LegacyPortabilityJob find(String key) {
    if (!map.containsKey(key)) {
      return null;
    }
    return LegacyPortabilityJob.mapToJob(map.get(key));
  }

  /**
   * Finds the {@link LegacyPortabilityJob} keyed by {@code jobId} in the map, and verify it is in
   * state {@code jobState}.
   */
  @Override
  public LegacyPortabilityJob find(String jobId, JobState jobState) {
    LegacyPortabilityJob job = find(jobId);
    Preconditions.checkNotNull(job,
        "Expected job {} to be in state {}, but the job was not found", jobId, jobState);
    Preconditions.checkState(job.jobState() == jobState,
        "Expected job {} to be in state {}, but was {}", jobState, job.jobState());
    return job;
  }

  /**
   * Finds the ID of the first {@link LegacyPortabilityJob} in state {@code jobState} in the map, or null
   * if none found.
   */
  @Override
  public synchronized String findFirst(JobState jobState) {
    // Mimic an index lookup
    for (Entry<String, Map<String, Object>> job : map.entrySet()) {
      Map<String, Object> properties = job.getValue();
      if (JobState.valueOf(properties.get(OldPortabilityJobConverter.JOB_STATE).toString())
          == jobState) {
        String jobId = job.getKey();
        return jobId;
      }
    }
    return null;
  }

  /**
   * Removes the {@link LegacyPortabilityJob} keyed by {@code jobId} in the map.
   *
   * @throws IOException if the job doesn't exist, or there was a different problem deleting it.
   */
  @Override
  public void remove(String jobId) throws IOException {
    Map<String, Object> previous = map.remove(jobId);
    if (previous == null) {
      throw new IOException("Job " + jobId + " didn't exist in the map");
    }
  }

  /**
   * Atomically updates the {@link LegacyPortabilityJob} keyed by {@code jobId} to {@code OldPortabilityJob}
   * in the map, and verifies that it was previously in the expected {@code previousState}.
   *
   * @throws IOException if the job was not in the expected state in the map, or there was another
   * problem updating it.
   */
  @Override
  public void update(LegacyPortabilityJob job, JobState previousState)
      throws IOException{
    Preconditions.checkNotNull(job.id());
    String jobId = job.id();
    try {
      Map<String, Object> previousEntry = map.replace(jobId, job.asMap());
      if (previousEntry == null) {
        throw new IOException("Job " + jobId + " didn't exist in the map");
      }
      if (getJobState(previousEntry) != previousState) {
        throw new IOException("Job " + jobId + " existed in an unexpected state. "
            + "Expected: " + previousState + " but was: " + getJobState(previousEntry));
      }
    } catch (NullPointerException e) {
      throw new IOException(
          "Couldn't update job " + jobId + " from previous state " + previousState, e);
    }
  }

  /**
   * Return {@code data}'s {@link JobState}, or null if missing.
   *
   * @param data a {@link LegacyPortabilityJob}'s representation in {@link #map}.
   */
  private JobState getJobState(Map<String, Object> data) {
    Object jobState = data.get(OldPortabilityJobConverter.JOB_STATE);
    // TODO: Remove null check once we enable encryptedFlow everywhere. Null should only be allowed
    // in legacy non-encrypted case
    if (!encryptedFlow) {
      return jobState == null ? null : JobState.valueOf(jobState.toString());
    }
    Preconditions.checkNotNull(jobState, "Job should never exist without a state");
    return JobState.valueOf(jobState.toString());
  }
}
