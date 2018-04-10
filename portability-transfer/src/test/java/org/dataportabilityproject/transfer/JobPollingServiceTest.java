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
package org.dataportabilityproject.transfer;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;
import org.dataportabilityproject.cloud.local.LocalJobStore;
import org.dataportabilityproject.security.AsymmetricKeyGenerator;
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
  private static final KeyPair TEST_KEY_PAIR = createTestKeyPair();

  @Mock private AsymmetricKeyGenerator asymmetricKeyGenerator;

  private JobStore store;
  private JobPollingService jobPollingService;

  private static final KeyPair createTestKeyPair() {
    PublicKey publicKey =
        new PublicKey() {
          @Override
          public String getAlgorithm() {
            return "RSA";
          }

          @Override
          public String getFormat() {
            return "";
          }

          @Override
          public byte[] getEncoded() {
            return "DummyPublicKey".getBytes();
          }
        };
    PrivateKey privateKey =
        new PrivateKey() {
          @Override
          public String getAlgorithm() {
            return "RSA";
          }

          @Override
          public String getFormat() {
            return "";
          }

          @Override
          public byte[] getEncoded() {
            return "DummyPrivateKey".getBytes();
          }
        };
    return new KeyPair(publicKey, privateKey);
  }

  @Before
  public void setUp() throws Exception {
    store = new LocalJobStore();
    jobPollingService = new JobPollingService(store, asymmetricKeyGenerator);
  }

  // TODO(data-transfer-project/issues/43): Make this an integration test which uses both the API
  // and transfer worker, rather than simulating API calls, in case this test ever diverges from
  // what the API actually does.
  @Test
  public void pollingLifeCycle() throws Exception {

    when(asymmetricKeyGenerator.generate()).thenReturn(TEST_KEY_PAIR);
    // Initial state
    assertThat(JobMetadata.isInitialized()).isFalse();

    // Run once with no data in the database
    jobPollingService.runOneIteration();
    assertThat(JobMetadata.isInitialized()).isFalse();
    PortabilityJob job = store.findJob(TEST_ID);
    assertThat(job).isNull(); // No existing ready job

    // API inserts an job in initial authorization state
    job =
        PortabilityJob.builder()
            .setTransferDataType("photo")
            .setExportService("DummyExportService")
            .setImportService("DummyImportService")
            .setAndValidateJobAuthorization(
                JobAuthorization.builder()
                    .setState(State.INITIAL)
                    .setSessionSecretKey("fooBar")
                    .build())
            .build();
    store.createJob(TEST_ID, job);

    // Verify initial authorization state
    job = store.findJob(TEST_ID);
    assertThat(job.jobAuthorization().state()).isEqualTo(State.INITIAL);
    // no auth data should exist yet
    assertThat(job.jobAuthorization().encryptedExportAuthData()).isNull();
    assertThat(job.jobAuthorization().encryptedImportAuthData()).isNull();

    // API atomically updates job to from 'initial' to 'creds available'
    job =
        job.toBuilder()
            .setAndValidateJobAuthorization(
                job.jobAuthorization().toBuilder().setState(State.CREDS_AVAILABLE).build())
            .build();
    store.updateJob(TEST_ID, job);

    // Verify 'creds available' state
    job = store.findJob(TEST_ID);
    assertThat(job.jobAuthorization().state()).isEqualTo(State.CREDS_AVAILABLE);
    // no auth data should exist yet
    assertThat(job.jobAuthorization().encryptedExportAuthData()).isNull();
    assertThat(job.jobAuthorization().encryptedImportAuthData()).isNull();

    // Worker initiates the JobPollingService
    jobPollingService.runOneIteration();
    assertThat(JobMetadata.isInitialized()).isTrue();
    assertThat(JobMetadata.getJobId()).isEqualTo(TEST_ID);

    // Verify assigned without auth data state
    job = store.findJob(TEST_ID);
    assertThat(job.jobAuthorization().state())
        .isEqualTo(JobAuthorization.State.CREDS_ENCRYPTION_KEY_GENERATED);
    assertThat(job.jobAuthorization().authPublicKey()).isNotEmpty();

    // Client encrypts data and updates the job
    job =
        job.toBuilder()
            .setAndValidateJobAuthorization(
                job.jobAuthorization()
                    .toBuilder()
                    .setEncryptedExportAuthData("dummy export data")
                    .setEncryptedImportAuthData("dummy import data")
                    .setState(State.CREDS_ENCRYPTED)
                    .build())
            .build();
    store.updateJob(TEST_ID, job);

    // Run another iteration of the polling service
    // Worker should pick up encrypted data and update job
    jobPollingService.runOneIteration();
    job = store.findJob(TEST_ID);
    JobAuthorization jobAuthorization = job.jobAuthorization();
    assertThat(jobAuthorization.state()).isEqualTo(JobAuthorization.State.CREDS_ENCRYPTED);
    assertThat(jobAuthorization.encryptedExportAuthData()).isNotEmpty();
    assertThat(jobAuthorization.encryptedImportAuthData()).isNotEmpty();

    store.remove(TEST_ID);
  }
}
