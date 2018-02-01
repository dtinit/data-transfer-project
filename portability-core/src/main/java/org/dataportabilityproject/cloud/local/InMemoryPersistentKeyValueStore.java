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
import org.dataportabilityproject.cloud.interfaces.PersistentKeyValueStore;
import org.dataportabilityproject.job.PortabilityJob.JobState;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.job.PortabilityJobConverter;
import org.dataportabilityproject.shared.settings.CommonSettings;

/**
 * An in-memory {@link PersistentKeyValueStore} implementation that uses a concurrent map as its
 * store.
 */
public final class InMemoryPersistentKeyValueStore implements PersistentKeyValueStore {
  private final ConcurrentHashMap<String, Map<String, Object>> map;
  private final CommonSettings commonSettings;

  public InMemoryPersistentKeyValueStore(CommonSettings commonSettings) {
    map = new ConcurrentHashMap<>();
    this.commonSettings = commonSettings;
  }

  /**
   * Inserts a new {@link PortabilityJob} keyed by {@code jobId} in the map.
   *
   * <p>To update an existing {@link PortabilityJob} instead, use {@link #atomicUpdate}.
   *
   * @throws IOException if a job already exists for {@code jobId}, or if there was a different
   * problem inserting the job.
   */
  @Override
  public synchronized void put(String jobId, PortabilityJob job) throws IOException {
    if (map.get(jobId) != null) {
      throw new IOException("An entry already exists for job " + jobId);
    }
    map.put(jobId, job.asMap());
  }

  /**
   * Gets the {@link PortabilityJob} keyed by {@code jobId} in the map, or null if not found.
   */
  @Override
  public PortabilityJob get(String key) {
    if (!map.containsKey(key)) {
      return null;
    }
    return PortabilityJob.mapToJob(map.get(key));
  }

  /**
   * Gets the {@link PortabilityJob} keyed by {@code jobId} in the map, and verify it is in
   * state {@code jobState}.
   */
  @Override
  public PortabilityJob get(String jobId, JobState jobState) {
    PortabilityJob job = get(jobId);
    Preconditions.checkNotNull(job,
        "Expected job {} to be in state {}, but the job was not found", jobId, jobState);
    Preconditions.checkState(job.jobState() == jobState,
        "Expected job {} to be in state {}, but was {}", jobState, job.jobState());
    return job;
  }

  /**
   * Gets the ID of the first {@link PortabilityJob} in state {@code jobState} in the map, or null
   * if none found.
   */
  @Override
  public synchronized String getFirst(JobState jobState) {
    // Mimic an index lookup
    for (Entry<String, Map<String, Object>> job : map.entrySet()) {
      Map<String, Object> properties = job.getValue();
      if (JobState.valueOf(properties.get(PortabilityJobConverter.JOB_STATE).toString())
          == jobState) {
        String jobId = job.getKey();
        return jobId;
      }
    }
    return null;
  }

  /**
   * Deletes the {@link PortabilityJob} keyed by {@code jobId} in the map.
   *
   * @throws IOException if the job doesn't exist, or there was a different problem deleting it.
   */
  @Override
  public void delete(String jobId) throws IOException {
    Map<String, Object> previous = map.remove(jobId);
    if (previous == null) {
      throw new IOException("Job " + jobId + " didn't exist in the map");
    }
  }

  /**
   * Atomically updates the {@link PortabilityJob} keyed by {@code jobId} to {@code portabilityJob}
   * in the map, and verifies that it was previously in the expected {@code previousState}.
   *
   * @throws IOException if the job was not in the expected state in the map, or there was another
   * problem updating it.
   */
  @Override
  public void atomicUpdate(String jobId, JobState previousState, PortabilityJob job)
      throws IOException{
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
   * @param data a {@link PortabilityJob}'s representation in {@link #map}.
   */
  private JobState getJobState(Map<String, Object> data) {
    Object jobState = data.get(PortabilityJobConverter.JOB_STATE);
    // TODO: Remove null check once we enable encryptedFlow everywhere. Null should only be allowed
    // in legacy non-encrypted case
    if (!commonSettings.getEncryptedFlow()) {
      return jobState == null ? null : JobState.valueOf(jobState.toString());
    }
    Preconditions.checkNotNull(jobState, "Job should never exist without a state");
    return JobState.valueOf(jobState.toString());
  }
}
