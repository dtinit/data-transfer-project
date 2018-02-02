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
import static org.mockito.Mockito.when;

import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.cloud.local.InMemoryKeyValueStore;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.LegacyPortabilityJob;
import org.dataportabilityproject.spi.cloud.types.LegacyPortabilityJob.JobState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JobPollingServiceTest {
  private static final String TEST_ID = "a_test_id";

  @Mock private CloudFactory cloudFactory;

  private JobPollingService jobPollingService;
  private WorkerJobMetadata metadata = new WorkerJobMetadata();

  JobStore store = new InMemoryKeyValueStore(true);

  @Before
  public void setUp()  throws Exception {
    when(cloudFactory.getJobStore()).thenReturn(store);
    jobPollingService = new JobPollingService(cloudFactory, metadata);
  }

  // TODO(data-portability/issues/43): Make this an integration test which uses both the API and
  // worker, rather than simulating API calls, in case this test ever diverges from what the API
  // actually does.
  @Test
  public void pollingLifeCycle() throws Exception {
    // Initial state
    assertThat(metadata.isInitialized()).isFalse();

    // Run once with no data in the database
    jobPollingService.runOneIteration();
    assertThat(metadata.isInitialized()).isFalse();
    LegacyPortabilityJob job = store.find(TEST_ID);
    assertThat(job).isNull(); // No existing ready job

    // API inserts an job in state 'pending auth data'
    store.create(LegacyPortabilityJob.builder()
        .setId(TEST_ID)
        .setDataType(PortableDataType.PHOTOS.name())
        .setExportService("DummyExportService")
        .setImportService("DummyImportService")
        .setJobState(JobState.PENDING_AUTH_DATA).build());

    // Verify initial state 'pending auth data'
    job = store.find(TEST_ID);
    assertThat(job.jobState()).isEqualTo(JobState.PENDING_AUTH_DATA);
    assertThat(job.exportAuthData()).isNull(); // no auth data should exist yet
    assertThat(job.importAuthData()).isNull();// no auth data should exist yet

    // API atomically updates job to from 'pending auth data' to 'pending worker assignment'
    job = job.toBuilder().setJobState(JobState.PENDING_WORKER_ASSIGNMENT).build();
    store.update(job, JobState.PENDING_AUTH_DATA);

    // Verify 'pending worker assignment' state
    job = store.find(TEST_ID);
    assertThat(job.jobState()).isEqualTo(JobState.PENDING_WORKER_ASSIGNMENT);
    assertThat(job.exportAuthData()).isNull(); // no auth data should exist yet
    assertThat(job.importAuthData()).isNull();// no auth data should exist yet

    // Worker initiates the JobPollingService
    jobPollingService.runOneIteration();
    assertThat(metadata.isInitialized()).isTrue();
    assertThat(metadata.getJobId()).isEqualTo(TEST_ID);

    // Verify assigned without auth data state
    job = store.find(TEST_ID);
    assertThat(job.jobState()).isEqualTo(JobState.ASSIGNED_WITHOUT_AUTH_DATA);
    assertThat(job.workerInstancePublicKey()).isNotEmpty();

    // Client encrypts data and updates the job
    job = job.toBuilder()
        .setEncryptedExportAuthData("dummy export data")
        .setEncryptedImportAuthData("dummy import data")
        .setJobState(JobState.ASSIGNED_WITH_AUTH_DATA)
        .build();
    store.update(job, JobState.ASSIGNED_WITHOUT_AUTH_DATA);

    // Run another iteration of the polling service
    // Worker should pick up encrypted data and update job
    jobPollingService.runOneIteration();
    job = store.find(TEST_ID);
    assertThat(job.jobState()).isEqualTo(JobState.ASSIGNED_WITH_AUTH_DATA);
    assertThat(job.encryptedExportAuthData()).isNotEmpty();
    assertThat(job.encryptedImportAuthData()).isNotEmpty();

    store.remove(TEST_ID);
  }
}
