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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.JobAuthorization;
import org.dataportabilityproject.spi.cloud.types.PortabilityJob;

/**
 * An in-memory {@link JobStore} implementation that uses a concurrent map as its
 * store.
 */
public final class LocalJobStore implements JobStore {
  private final ConcurrentHashMap<UUID, Map<String, Object>> map;

  public LocalJobStore() {
    this.map = new ConcurrentHashMap<>();
  }

  /**
   * Inserts a new {@link PortabilityJob} keyed by its job ID in the store.
   *
   * <p>To update an existing {@link PortabilityJob} instead, use {@link #update}.
   *
   * @throws IOException if a job already exists for {@code job}'s ID, or if there was a different
   * problem inserting the job.
   */
  @Override
  public void createJob(UUID jobId, PortabilityJob job) throws IOException {
    Preconditions.checkNotNull(jobId);
    if (map.get(jobId) != null) {
      throw new IOException("An entry already exists for jobId: " + jobId);
    }
    map.put(jobId, job.toMap());
  }

  /**
   * Atomically updates the entry for {@code job}'s ID to {@code job}.
   *
   * @throws IOException if the job was not in the expected state in the store, or there was
   * another problem updating it.
   *
   * TODO(rtannenbaum): Consider validating authorization state was the previous one, when updating
   * authorization state within this transaction. Previous API allowed for passing in of a previous
   * expected state, but we shouldn't need to pass that in, given the context of the new state we
   * should know what comes before it.
   */
  @Override
  public void updateJob(UUID jobId, PortabilityJob job) throws IOException {
    Preconditions.checkNotNull(jobId);
    try {
      Map<String, Object> previousEntry = map.replace(jobId, job.toMap());
      if (previousEntry == null) {
        throw new IOException("jobId: " + jobId + " didn't exist in the map");
      }
    } catch (NullPointerException e) {
      throw new IOException(
          "Couldn't update jobId: " + jobId, e);
    }
  }

  /**
   * Removes the {@link PortabilityJob} keyed by {@code jobId} in the map.
   *
   * @throws IOException if the job doesn't exist, or there was a different problem deleting it.
   */
  @Override
  public void remove(UUID jobId) throws IOException {
    Map<String, Object> previous = map.remove(jobId);
    if (previous == null) {
      throw new IOException("jobId: " + jobId + " didn't exist in the map");
    }
  }

  /**
   * Returns the job for the id or null if not found.
   *
   * @param jobId the job id
   */
  @Override
  public PortabilityJob findJob(UUID jobId) {
    if (!map.containsKey(jobId)) {
      return null;
    }
    return PortabilityJob.fromMap(map.get(jobId));
  }

  /**
   * Finds the ID of the first {@link PortabilityJob} in state {@code jobState} in the map, or null
   * if none found.
   */
  @Override
  public synchronized UUID findFirst(JobAuthorization.State jobState) {
    // Mimic an index lookup
    for (Entry<UUID, Map<String, Object>> job : map.entrySet()) {
      Map<String, Object> properties = job.getValue();
      if (JobAuthorization.State.valueOf(
          properties.get(PortabilityJob.AUTHORIZATION_STATE).toString()) == jobState) {
        UUID jobId = job.getKey();
        return jobId;
      }
    }
    return null;
  }
}
