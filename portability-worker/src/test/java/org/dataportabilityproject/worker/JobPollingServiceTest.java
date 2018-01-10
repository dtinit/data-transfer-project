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

import org.dataportabilityproject.cloud.SupportedCloud;
import org.dataportabilityproject.cloud.interfaces.PersistentKeyValueStore;
import org.dataportabilityproject.cloud.local.InMemoryPersistentKeyValueStore;
import org.dataportabilityproject.job.JobDao;
import org.dataportabilityproject.job.JobDao.JobState;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.shared.PortableDataType;
import org.junit.Before;
import org.junit.Test;

public class JobPollingServiceTest {
  private static final String TEST_ID = "a_test_id";

  private JobDao jobDao;
  private JobPollingService jobPollingService;
  private WorkerJobMetadata metadata = new WorkerJobMetadata();

  @Before
  public void setUp()  throws Exception {
    PersistentKeyValueStore persistentKeyValueStore = new InMemoryPersistentKeyValueStore();
    jobDao = new JobDao(persistentKeyValueStore);
    jobPollingService = new JobPollingService(jobDao, metadata);
  }

  @Test
  public void pollingLifeCycle() throws Exception {
    // Initial state
    assertThat(metadata.isInitialized()).isFalse();

    // Run once with no data in the database
    jobPollingService.runOneIteration();
    assertThat(metadata.isInitialized()).isFalse();
    PortabilityJob job = jobDao.findExistingJob(TEST_ID);
    assertThat(job).isNull(); // No existing ready job

    // API inserts an job pending auth data
    jobDao.insertJobInPendingAuthDataState(PortabilityJob.builder().setId(TEST_ID)
        .setDataType(PortableDataType.PHOTOS.name())
        .setExportService("DummyExportService")
        .setImportService("DummyImportService").build());

    // Verify initial state
    job = jobDao.lookupJobPendingAuthData(TEST_ID);
    assertThat(job.exportAuthData()).isNull(); // no auth data should exist yet
    assertThat(job.importAuthData()).isNull();// no auth data should exist yet

    // API updates job to pending worker assignment
    jobDao.updateJobStateToPendingWorkerAssignment(TEST_ID);

    // Verify pending worker assignment state
    job = jobDao.lookupJobPendingWorkerAssignment(TEST_ID);
    assertThat(job.exportAuthData()).isNull(); // no auth data should exist yet
    assertThat(job.importAuthData()).isNull();// no auth data should exist yet

    // Worker initiates the JobPollingService
    jobPollingService.runOneIteration();
    assertThat(metadata.isInitialized()).isTrue();
    assertThat(metadata.getJobId()).isEqualTo(TEST_ID);
    job = jobDao.lookupAssignedWithoutAuthDataJob(TEST_ID);

    // Verify assigned without auth data state
    assertThat(job.workerInstancePublicKey()).isNotEmpty();

    // Client encrypts data and updates the job
    jobDao.updateJobStateToAssigneWithAuthData(job.id(), "dummy export data", "dummy import data");

    // Run another iteration of the polling service
    // Worker picks up encrypted data and update job
    jobPollingService.runOneIteration();

    job = jobDao.lookupAssignedWithAuthDataJob(job.id());
    assertThat(job.encryptedExportAuthData()).isNotEmpty();
    assertThat(job.encryptedImportAuthData()).isNotEmpty();
    jobDao.deleteJob(job.id(), JobState.ASSIGNED_WITH_AUTH_DATA);
  }
}
