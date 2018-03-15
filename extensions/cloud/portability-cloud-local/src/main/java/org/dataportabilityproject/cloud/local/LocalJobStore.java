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
import java.io.InputStream;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.JobAuthorization;
import org.dataportabilityproject.spi.cloud.types.PortabilityJob;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.dataportabilityproject.types.transfer.models.DataModel;

/** An in-memory {@link JobStore} implementation that uses a concurrent map as its store. */
public final class LocalJobStore implements JobStore {
  private static ConcurrentHashMap<UUID, Map<String, Object>> SINGLETON_MAP = new ConcurrentHashMap<>();

  /**
   * Inserts a new {@link PortabilityJob} keyed by its job ID in the store.
   *
   * <p>To update an existing {@link PortabilityJob} instead, use {@link #update}.
   *
   * @throws IOException if a job already exists for {@code job}'s ID, or if there was a different
   *     problem inserting the job.
   */
  @Override
  public void createJob(UUID jobId, PortabilityJob job) throws IOException {
    Preconditions.checkNotNull(jobId);
    if (SINGLETON_MAP.get(jobId) != null) {
      throw new IOException("An entry already exists for jobId: " + jobId);
    }
    SINGLETON_MAP.put(jobId, job.toMap());
  }

  /**
   * Verifies a {@code PortabilityJob} already exists for {@code jobId}, and updates the entry to
   * {@code job}.
   *
   * @throws IOException if a job didn't already exist for {@code jobId} or there was a problem
   *     updating it
   */
  @Override
  public void updateJob(UUID jobId, PortabilityJob job) throws IOException {
    updateJob(jobId, job, null);
  }

  /**
   * Verifies a {@code PortabilityJob} already exists for {@code jobId}, and updates the entry to
   * {@code job}. If {@code validator} is non-null, validator.validate() is called first, as part of
   * the atomic update.
   *
   * @throws IOException if a job didn't already exist for {@code jobId} or there was a problem
   *     updating it
   * @throws IllegalStateException if validator.validate() failed
   */
  @Override
  public synchronized void updateJob(UUID jobId, PortabilityJob job, JobUpdateValidator validator)
      throws IOException {
    Preconditions.checkNotNull(jobId);
    try {
      Map<String, Object> previousEntry = SINGLETON_MAP.replace(jobId, job.toMap());
      if (previousEntry == null) {
        throw new IOException("jobId: " + jobId + " didn't exist in the map");
      }
      if (validator != null) {
        PortabilityJob previousJob = PortabilityJob.fromMap(previousEntry);
        validator.validate(previousJob, job);
      }
    } catch (NullPointerException | IllegalStateException e) {
      throw new IOException("Couldn't update jobId: " + jobId, e);
    }
  }

  /**
   * Removes the {@link PortabilityJob} keyed by {@code jobId} in the map.
   *
   * @throws IOException if the job doesn't exist, or there was a different problem deleting it.
   */
  @Override
  public void remove(UUID jobId) throws IOException {
    Map<String, Object> previous = SINGLETON_MAP.remove(jobId);
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
    if (!SINGLETON_MAP.containsKey(jobId)) {
      return null;
    }
    return PortabilityJob.fromMap(SINGLETON_MAP.get(jobId));
  }

  /**
   * Finds the ID of the first {@link PortabilityJob} in state {@code jobState} in the map, or null
   * if none found.
   */
  @Override
  public synchronized UUID findFirst(JobAuthorization.State jobState) {
    // Mimic an index lookup
    for (Entry<UUID, Map<String, Object>> job : SINGLETON_MAP.entrySet()) {
      Map<String, Object> properties = job.getValue();
      if (JobAuthorization.State.valueOf(
              properties.get(PortabilityJob.AUTHORIZATION_STATE).toString())
          == jobState) {
        UUID jobId = job.getKey();
        return jobId;
      }
    }
    return null;
  }

  @Override
  public <T extends DataModel> void create(UUID jobId, T model) {
    // TODO(olsona): what if the jobId is not in the map?
    SINGLETON_MAP.get(jobId).put(model.getClass().getCanonicalName(), model);
  }

  /** Updates the given model instance associated with a job. */
  @Override
  public <T extends DataModel> void update(UUID jobId, T model) {
    SINGLETON_MAP.get(jobId).put(model.getClass().getCanonicalName(), model);
  }

  /** Returns a model instance for the id of the given type or null if not found. */
  @Override
  public <T extends DataModel> T findData(Class<T> type, UUID id) {
    if (!SINGLETON_MAP.containsKey(id)) {
      return null;
    }
    if (!SINGLETON_MAP.get(id).containsKey(type.getCanonicalName())) {
      return null;
    }
    return (T) SINGLETON_MAP.get(id).get(type.getCanonicalName());
  }
}
