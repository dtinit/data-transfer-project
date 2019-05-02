/*
 * Copyright 2018 The Data Transfer Project Authors.
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
package org.datatransferproject.cloud.local;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.types.JobAuthorization;
import org.datatransferproject.spi.cloud.types.JobAuthorization.State;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.types.common.models.DataModel;
import org.datatransferproject.types.transfer.errors.ErrorDetail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

/** An in-memory {@link JobStore} implementation that uses a concurrent map as its store. */
public final class LocalJobStore implements JobStore {
  private static ConcurrentHashMap<UUID, Map<String, Object>> JOB_MAP = new ConcurrentHashMap<>();
  private static ConcurrentHashMap<String, Map<Class<? extends DataModel>, DataModel>> DATA_MAP =
      new ConcurrentHashMap<>();
  private static LocalTempFileStore localTempFileStore = new LocalTempFileStore();
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final Monitor monitor;

  /** Ctor for testing with a null monitor. */
  public LocalJobStore() {
    this(new Monitor() {});
  }

  public LocalJobStore(Monitor monitor) {
    this.monitor = monitor;
  }

  /**
   * Inserts a new {@link PortabilityJob} keyed by its job ID in the store.
   *
   * <p>To update an existing {@link PortabilityJob} instead, use {@link JobStore#update}.
   *
   * @throws IOException if a job already exists for {@code job}'s ID, or if there was a different
   *     problem inserting the job.
   */
  @Override
  public void createJob(UUID jobId, PortabilityJob job) throws IOException {
    Preconditions.checkNotNull(jobId);
    monitor.debug(() -> format("Creating job %s in local storage", jobId));
    if (JOB_MAP.get(jobId) != null) {
      throw new IOException("An entry already exists for jobId: " + jobId);
    }
    JOB_MAP.put(jobId, job.toMap());
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
      Map<String, Object> previousEntry = JOB_MAP.replace(jobId, job.toMap());
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

  @Override
  public void addErrorsToJob(UUID jobId, Collection<ErrorDetail> errors) throws IOException {
    // This is a no-op currently as nothing in DTP reads the errors currently.
    if (errors != null && !errors.isEmpty()) {
      for (ErrorDetail error : errors) {
        monitor.info(() -> "Added error: %s", OBJECT_MAPPER.writeValueAsString(error));
      }
    }
  }

  /**
   * Removes the {@link PortabilityJob} keyed by {@code jobId} in the map.
   *
   * @throws IOException if the job doesn't exist, or there was a different problem deleting it.
   */
  @Override
  public void remove(UUID jobId) throws IOException {
    monitor.debug(() -> format("Remove job %s from local storage", jobId));
    Map<String, Object> previous = JOB_MAP.remove(jobId);
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
    if (!JOB_MAP.containsKey(jobId)) {
      return null;
    }
    return PortabilityJob.fromMap(JOB_MAP.get(jobId));
  }

  /**
   * Finds the ID of the first {@link PortabilityJob} in state {@code jobState} in the map, or null
   * if none found.
   */
  @Override
  public synchronized UUID findFirst(JobAuthorization.State jobState) {
    // Mimic an index lookup
    for (Entry<UUID, Map<String, Object>> job : JOB_MAP.entrySet()) {
      Map<String, Object> properties = job.getValue();
      State state = State.valueOf(properties.get(PortabilityJob.AUTHORIZATION_STATE).toString());
      UUID jobKey = job.getKey();
      monitor.debug(
          () ->
              format(
                  "Looking up first job in state %s: found job %s (state %s)",
                  jobState, jobKey, state));
      if (state == jobState) {
        return jobKey;
      }
    }
    return null;
  }

  @Override
  public <T extends DataModel> void create(UUID jobId, String key, T model) {
    if (!DATA_MAP.containsKey(createFullKey(jobId, key))) {
      DATA_MAP.put(createFullKey(jobId, key), new ConcurrentHashMap<>());
    }
    DATA_MAP.get(createFullKey(jobId, key)).put(model.getClass(), model);
  }

  /** Updates the given model instance associated with a job. */
  @Override
  public <T extends DataModel> void update(UUID jobId, String key, T model) {
    // TODO: do we want to do any checking here to make sure there's something to update?
    create(jobId, key, model);
  }

  /** Returns a model instance for the id of the given type or null if not found. */
  @Override
  public <T extends DataModel> T findData(UUID jobId, String key, Class<T> type) {
    if (!DATA_MAP.containsKey(createFullKey(jobId, key))) {
      return null;
    }
    if (!DATA_MAP.get(createFullKey(jobId, key)).containsKey(type)) {
      return null;
    }
    return (T) DATA_MAP.get(createFullKey(jobId, key)).get(type);
  }

  @Override
  public void create(UUID jobId, String key, InputStream stream) throws IOException {
    localTempFileStore.writeInputStream(makeFileName(jobId, key), stream);
  }

  @Override
  public InputStream getStream(UUID jobId, String key) throws IOException {
    return localTempFileStore.getInputStream(makeFileName(jobId, key));
  }

  private static String createFullKey(UUID jobId, String key) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
    return format("%s-%s", jobId.toString(), key);
  }

  private static String makeFileName(UUID jobId, String inputName) {
    String replace = inputName.replace("/", "_");
    return createFullKey(jobId, replace);
  }
}
