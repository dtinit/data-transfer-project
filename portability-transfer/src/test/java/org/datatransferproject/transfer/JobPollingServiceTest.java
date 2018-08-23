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
package org.datatransferproject.transfer;

import org.datatransferproject.cloud.local.LocalJobStore;
import org.datatransferproject.security.AsymmetricKeyGenerator;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.types.JobAuthorization;
import org.datatransferproject.spi.cloud.types.JobAuthorization.State;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.spi.transfer.security.PublicKeySerializer;
import org.datatransferproject.spi.transfer.security.SecurityException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.UUID;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JobPollingServiceTest {
  private static final UUID TEST_ID = UUID.randomUUID();
  private static final KeyPair TEST_KEY_PAIR = createTestKeyPair();

  @Mock private AsymmetricKeyGenerator asymmetricKeyGenerator;

  private JobStore store;
  private JobPollingService jobPollingService;

  private static KeyPair createTestKeyPair() {
    RSAPublicKey publicKey =
        new RSAPublicKey() {
          @Override
          public BigInteger getModulus() {
            return BigInteger.ZERO;
          }

          @Override
          public BigInteger getPublicExponent() {
            return BigInteger.ZERO;
          }

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
    RSAPrivateKey privateKey =
        new RSAPrivateKey() {
          @Override
          public BigInteger getModulus() {
            return BigInteger.ZERO;
          }

          @Override
          public BigInteger getPrivateExponent() {
            return BigInteger.ZERO;
          }

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
    PublicKeySerializer serializer =
        new PublicKeySerializer() {
          @Override
          public boolean canHandle(String scheme) {
            return true;
          }

          @Override
          public String serialize(PublicKey publicKey) throws SecurityException {
            return "key";
          }
        };
    jobPollingService =
        new JobPollingService(store, asymmetricKeyGenerator, Collections.singleton(serializer));
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
                    .setEncryptionScheme("cleartext")
                    .setState(State.INITIAL)
                    .setSessionSecretKey("fooBar")
                    .build())
            .build();
    store.createJob(TEST_ID, job);

    // Verify initial authorization state
    job = store.findJob(TEST_ID);
    assertThat(job.jobAuthorization().state()).isEqualTo(State.INITIAL);
    // no auth data should exist yet
    assertThat(job.jobAuthorization().encryptedAuthData()).isNull();

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
    assertThat(job.jobAuthorization().encryptedAuthData()).isNull();

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
                    .setEncryptedAuthData("dummy export data")
                    .setState(State.CREDS_STORED)
                    .build())
            .build();
    store.updateJob(TEST_ID, job);

    // Run another iteration of the polling service
    // Worker should pick up encrypted data and update job
    jobPollingService.runOneIteration();
    job = store.findJob(TEST_ID);
    JobAuthorization jobAuthorization = job.jobAuthorization();
    assertThat(jobAuthorization.state()).isEqualTo(JobAuthorization.State.CREDS_STORED);
    assertThat(jobAuthorization.encryptedAuthData()).isNotEmpty();

    store.remove(TEST_ID);
  }
}
