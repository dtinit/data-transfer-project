/*
 * Copyright 2018 The Data-Portability Project Authors.
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
package org.dataportabilityproject.transfer.microsoft.helper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.JobAuthorization;
import org.dataportabilityproject.spi.cloud.types.JobAuthorization.State;
import org.dataportabilityproject.spi.cloud.types.LegacyPortabilityJob;
import org.dataportabilityproject.spi.cloud.types.PortabilityJob;
import org.dataportabilityproject.types.transfer.models.DataModel;

/** An implementation for testing. */
public class MockJobStore implements JobStore {
  private final Map<UUID, DataModel> testData = new HashMap<>();

  @Override
  public <T extends DataModel> T findData(Class<T> type, UUID id) {
    return type.cast(testData.get(id));
  }

  @Override
  public LegacyPortabilityJob find(UUID jobId, JobAuthorization.State jobState) {
    return null;
  }

  @Override
  public void create(UUID jobId, LegacyPortabilityJob job) throws IOException {}

  @Override
  public void createJob(UUID jobId, PortabilityJob job) throws IOException {}

  @Override
  public void update(UUID jobId, LegacyPortabilityJob job, JobAuthorization.State previousState)
      throws IOException {}

  @Override
  public void updateJob(UUID jobId, PortabilityJob job) throws IOException {}

  @Override
  public void updateJob(UUID jobId, PortabilityJob job, State previousState) throws IOException {}

  @Override
  public void remove(UUID jobId) throws IOException {}

  @Override
  public PortabilityJob findJob(UUID jobId) {
    return null;
  }

  @Override
  public LegacyPortabilityJob find(UUID jobId) {
    return null;
  }

  @Override
  public UUID findFirst(JobAuthorization.State jobState) {
    return null;
  }

  @Override
  public <T extends DataModel> void create(UUID jobId, T model) {
    testData.put(jobId, model);
  }

  @Override
  public <T extends DataModel> void update(UUID jobId, T model) {
    if (!testData.containsKey(jobId)) {
      throw new AssertionError("Data does not exist: " + jobId);
    }
    testData.put(jobId, model);
  }
}
