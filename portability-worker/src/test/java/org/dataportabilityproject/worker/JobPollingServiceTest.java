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
package org.dataportabilityproject.worker;

import static com.google.common.truth.Truth.assertThat;

import org.dataportabilityproject.cloud.CloudFactoryFactory;
import org.dataportabilityproject.cloud.SupportedCloud;
import org.dataportabilityproject.cloud.interfaces.PersistentKeyValueStore;
import org.dataportabilityproject.job.JobDao;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.auth.SecretAuthData;
import org.junit.Before;
import org.junit.Test;

public class JobPollingServiceTest {
  private static final String TEST_ID = "a_test_id";

  private JobDao jobDao;
  private JobPollingService jobPollingService;

  @Before
  public void setUp()  throws Exception {
    PersistentKeyValueStore persistentKeyValueStore = CloudFactoryFactory
        .getCloudFactory(SupportedCloud.LOCAL)
        .getPersistentKeyValueStore();
    jobDao = new JobDao(persistentKeyValueStore);
    jobPollingService = new JobPollingService(jobDao);
  }

  @Test
  public void pollingLifeCycle() throws Exception {
    // Initial state
    assertThat(WorkerJobMetadata.getInstance().isInitialized()).isFalse();

    // Run once with no data in the database
    jobPollingService.runOneIteration();
    assertThat(WorkerJobMetadata.getInstance().isInitialized()).isFalse();
    PortabilityJob job = jobDao.findExistingJob(TEST_ID);
    assertThat(job).isNull(); // No existing ready job

    // Client inserts an unassigned job
    jobDao.insertJobInUnassignedState(TEST_ID);
    jobPollingService.runOneIteration();
    assertThat(WorkerJobMetadata.getInstance().isInitialized()).isTrue();
    assertThat(WorkerJobMetadata.getInstance().getJobId()).isEqualTo(TEST_ID);
    job = jobDao.lookupJob(TEST_ID);
    assertThat(job).isNull(); // No existing ready job

    // Client sends complete data for a unassigned job
    PortabilityJob complete = PortabilityJob.builder()
        .setId(TEST_ID)
        .setDataType(PortableDataType.PHOTOS.name())
        .setExportService("flickr")
        .setExportAuthData(SecretAuthData.create("test_secret_1"))
        .setImportService("instragram")
        .setImportAuthData(SecretAuthData.create("test_secret_2"))
        .build();
    jobDao.updateJobToReady(complete);
    assertThat(WorkerJobMetadata.getInstance().isInitialized()).isTrue();
    assertThat(WorkerJobMetadata.getInstance().getJobId()).isEqualTo(TEST_ID);

    // Job exists in state ready to process
    job = jobDao.lookupJob(TEST_ID);
    assertThat(job).isEqualTo(complete);
  }
}
