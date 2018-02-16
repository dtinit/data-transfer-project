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

import java.util.UUID;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.cloud.local.InMemoryKeyValueStore;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.JobAuthorization;
import org.dataportabilityproject.spi.cloud.types.JobAuthorization.State;
import org.dataportabilityproject.spi.cloud.types.PortabilityJob;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JobPollingServiceTest {
  private static final UUID TEST_ID = UUID.randomUUID();

  @Mock private CloudFactory cloudFactory;

  private JobPollingService jobPollingService;
  private WorkerJobMetadata metadata = new WorkerJobMetadata();

  JobStore store = new InMemoryKeyValueStore();

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
    PortabilityJob job = store.findJob(TEST_ID);
    assertThat(job).isNull(); // No existing ready job

    // API inserts an job in initial authorization state
    job = new PortabilityJob();
    job.setTransferDataType(PortableDataType.PHOTOS.name());
    job.setExportService("DummyExportService");
    job.setImportService("DummyImportService");
    JobAuthorization jobAuthorization = new JobAuthorization();
    jobAuthorization.setState(State.INITIAL);
    jobAuthorization.setEncryptedSessionKey("fooBar");
    job.setJobAuthorization(jobAuthorization);
    store.createJob(TEST_ID, job);

    // Verify initial authorization state
    job = store.findJob(TEST_ID);
    assertThat(job.getJobAuthorization().getState()).isEqualTo(State.INITIAL);
    // no auth data should exist yet
    assertThat(job.getJobAuthorization().getEncryptedExportAuthData()).isNull();
    assertThat(job.getJobAuthorization().getEncryptedImportAuthData()).isNull();

    // API atomically updates job to from 'initial' to 'creds available'
    jobAuthorization.setState(State.CREDS_AVAILABLE);
    job.setJobAuthorization(jobAuthorization);
    store.updateJob(TEST_ID, job);

    // Verify 'creds available' state
    job = store.findJob(TEST_ID);
    assertThat(job.getJobAuthorization().getState()).isEqualTo(State.CREDS_AVAILABLE);
    // no auth data should exist yet
    assertThat(job.getJobAuthorization().getEncryptedExportAuthData()).isNull();
    assertThat(job.getJobAuthorization().getEncryptedImportAuthData()).isNull();
    
    // Worker initiates the JobPollingService
    jobPollingService.runOneIteration();
    assertThat(metadata.isInitialized()).isTrue();
    assertThat(metadata.getJobId()).isEqualTo(TEST_ID);

    // Verify assigned without auth data state
    job = store.findJob(TEST_ID);
    assertThat(job.getJobAuthorization().getState())
        .isEqualTo(JobAuthorization.State.CREDS_ENCRYPTION_KEY_GENERATED);
    assertThat(job.getJobAuthorization().getEncryptedPublicKey()).isNotEmpty();
    assertThat(job.getJobAuthorization().getEncryptedPrivateKey()).isNotEmpty();

    // Client encrypts data and updates the job
    jobAuthorization = job.getJobAuthorization();
    jobAuthorization.setEncryptedExportAuthData("dummy export data");
    jobAuthorization.setEncryptedImportAuthData("dummy import data");
    jobAuthorization.setState(State.CREDS_ENCRYPTED);
    job.setJobAuthorization(jobAuthorization);
    store.updateJob(TEST_ID, job);

    // Run another iteration of the polling service
    // Worker should pick up encrypted data and update job
    jobPollingService.runOneIteration();
    job = store.findJob(TEST_ID);
    jobAuthorization = job.getJobAuthorization();
    assertThat(jobAuthorization.getState()).isEqualTo(JobAuthorization.State.CREDS_ENCRYPTED);
    assertThat(jobAuthorization.getEncryptedExportAuthData()).isNotEmpty();
    assertThat(jobAuthorization.getEncryptedExportAuthData()).isNotEmpty();

    store.remove(TEST_ID);
  }
}
