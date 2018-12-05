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
package org.datatransferproject.transfer.microsoft.helper;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.types.JobAuthorization;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.types.common.models.DataModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** An implementation for testing. */
public class MockJobStore implements JobStore {
  private final Map<String, DataModel> testData = new HashMap<>();
  private final Map<String, InputStream> keyedData = new HashMap<>();

  @Override
  public <T extends DataModel> T findData(UUID jobId, String key, Class<T> type) {
    return type.cast(testData.get(createFullKey(jobId, key)));
  }

  @Override
  public void createJob(UUID jobId, PortabilityJob job) throws IOException {}

  @Override
  public void updateJob(UUID jobId, PortabilityJob job) throws IOException {}

  @Override
  public void updateJob(UUID jobId, PortabilityJob job, JobUpdateValidator validator)
      throws IOException {}

  @Override
  public void remove(UUID jobId) throws IOException {}

  @Override
  public PortabilityJob findJob(UUID jobId) {
    return null;
  }

  @Override
  public UUID findFirst(JobAuthorization.State jobState) {
    return null;
  }

  @Override
  public <T extends DataModel> void create(UUID jobId, String key, T model) {
    testData.put(createFullKey(jobId, key), model);
  }

  @Override
  public <T extends DataModel> void update(UUID jobId, String key, T model) {
    if (!testData.containsKey(createFullKey(jobId, key))) {
      throw new AssertionError("Data does not exist: " + jobId);
    }
    testData.put(createFullKey(jobId, key), model);
  }

  @Override
  public void create(UUID jobId, String key, InputStream stream) {
     keyedData.put(createFullKey(jobId, key), stream);
  }

  @Override
  public InputStream getStream(UUID jobId, String key) {
    return keyedData.get(createFullKey(jobId, key));
  }

  private static String createFullKey(UUID jobId, String key) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
    return String.format("%s-%s", jobId.toString(), key);
  }
}
