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

import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.LegacyPortabilityJob;
import org.dataportabilityproject.types.transfer.models.DataModel;

import java.util.HashMap;
import java.util.Map;

/**
 * An implementation for testing.
 */
public class MockJobStore implements JobStore {
    private final Map<String, DataModel> testData = new HashMap<>();

    @Override
    public <T extends DataModel> T findData(Class<T> type, String id) {
        return type.cast(testData.get(id));
    }

    @Override
    public <T extends DataModel> void create(String jobId, T model) {
        testData.put(jobId, model);
    }

    @Override
    public <T extends DataModel> void update(String jobId, T model) {
        if (!testData.containsKey(jobId)) {
            throw new AssertionError("Data does not exist: " + jobId);
        }
        testData.put(jobId, model);
    }

    @Override
    public void create(LegacyPortabilityJob job) {

    }

    @Override
    public void update(LegacyPortabilityJob job, LegacyPortabilityJob.JobState previousState) {

    }

    @Override
    public void remove(String jobId) {

    }

    @Override
    public LegacyPortabilityJob find(String jobId) {
        return null;
    }

    @Override
    public String findFirst(LegacyPortabilityJob.JobState jobState) {
        return null;
    }

    @Override
    public LegacyPortabilityJob find(String jobId, LegacyPortabilityJob.JobState jobState) {
        return null;
    }
}
