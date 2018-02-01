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

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.dataportabilityproject.cloud.interfaces.PersistentKeyValueStore;
import org.dataportabilityproject.job.JobDao.JobState;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.job.PortabilityJobConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-memory implementation of backend storage.
 */
public final class InMemoryPersistentKeyValueStore implements PersistentKeyValueStore {
  private static final Logger logger =
      LoggerFactory.getLogger(InMemoryPersistentKeyValueStore.class);

  private final ConcurrentHashMap<String, Map<String, Object>> map;

  public InMemoryPersistentKeyValueStore() {
    map = new ConcurrentHashMap<>();
  }

  @Override
  public synchronized void put(String jobId, PortabilityJob job) throws IOException {
    if (map.get(jobId) != null) {
      throw new IOException("An entry already exists for job " + jobId);
    }
    map.put(jobId, job.asMap());
  }

  @Override
  public PortabilityJob get(String key) {
    if (!map.containsKey(key)) {
      return null;
    }
    return PortabilityJob.mapToJob(map.get(key));
  }

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

  @Override
  public void delete(String jobId) throws IOException {
    Map<String, Object> previous = map.remove(jobId);
    if (previous == null) {
      throw new IOException("Job " + jobId + " didn't exist in the database");
    }
  }

  /**
   * Atomically update map.
   */
  @Override
  public void atomicUpdate(String jobId, JobState previousState, PortabilityJob job)
      throws IOException{
    try {
      Map<String, Object> previousEntry = map.replace(jobId, job.asMap());
      if (previousEntry == null) {
        throw new IOException("Job " + jobId + " didn't exist in the database");
      }
      if (previousState != null && getJobState(previousEntry) != previousState) {
        throw new IOException("Job " + jobId + " existed in an unexpected state. "
            + "Expected: " + previousState + " but was: " + getJobState(previousEntry));
      }
    } catch (NullPointerException e) {
      throw new IOException(
          "Couldn't update job " + jobId + " from previous state " + previousState, e);
    }
  }

  private JobState getJobState(Map<String, Object> data) {
    return JobState.valueOf(data.get(PortabilityJobConverter.JOB_STATE).toString());
  }
}
