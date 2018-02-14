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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.job.PublicPrivateKeyPairGenerator;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.JobAuthorization;
import org.dataportabilityproject.spi.cloud.types.LegacyPortabilityJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A service that polls storage for a job to process in two steps:
 * <br>
 *   (1) find an unassigned job for this worker
 * <br>
 *   (2) wait until the job is ready to process (i.e. creds are available)
 */
class JobPollingService extends AbstractScheduledService {
  private final Logger logger = LoggerFactory.getLogger(JobPollingService.class);
  private final JobStore store;
  private final WorkerJobMetadata jobMetadata;

  @Inject
  JobPollingService(CloudFactory cloudFactory, WorkerJobMetadata jobMetadata) {
    this.store = cloudFactory.getJobStore();
    this.jobMetadata = jobMetadata;
  }

  @Override
  protected void runOneIteration() throws Exception {
    if (jobMetadata.isInitialized()) {
      pollUntilJobIsReady();
    } else {
      // Poll for an unassigned job to process with this worker instance.
      // Once a worker instance is assigned, the client will populate storage with
      // auth data encrypted with this instances public key and the copy process can begin
      pollForUnassignedJob();
    }
  }

  @Override
  protected Scheduler scheduler() {
    return AbstractScheduledService.Scheduler.newFixedDelaySchedule(0, 20, TimeUnit.SECONDS);
  }

  /**
   * Polls for an unassigned job, and once found, initializes the global singleton job metadata
   * object for this running instance of the worker.
   */
  private void pollForUnassignedJob() throws IOException {
    UUID jobId = store.findFirst(JobAuthorization.State.CREDS_AVAILABLE);
    logger.debug("Polling for a job CREDS_AVAILABLE");
    if (jobId == null) {
      return;
    }
    logger.debug("Polled job {}", jobId);
    Preconditions.checkState(!jobMetadata.isInitialized());
    KeyPair keyPair = PublicPrivateKeyPairGenerator.generateKeyPair();
    PublicKey publicKey = keyPair.getPublic();
    // TODO: Move storage of private key to a different location
    PrivateKey privateKey = keyPair.getPrivate();
    // Executing Job State Transition from Unassigned to Assigned
    try {
      updateJobStateToAssignedWithoutAuthData(jobId, publicKey, privateKey);
      jobMetadata.init(jobId, keyPair);
      logger.debug("Updated job {} to CREDS_ENCRYPTION_KEY_GENERATED, publicKey length: {}",
          jobId, publicKey.getEncoded().length);
    } catch (IOException e) {
      logger.debug("Failed to claim job {}; it was probably already claimed by another worker",
          jobId);
    }
  }

  /**
   * Replaces a unassigned {@link LegacyPortabilityJob} in storage with the provided {@code jobId} in
   * assigned state with {@code publicKey} and {@code privateKey}.
   */
  private void updateJobStateToAssignedWithoutAuthData(UUID jobId, PublicKey publicKey,
      PrivateKey privateKey) throws IOException {
    // Lookup the job so we can append to its existing properties.
    // update will verify the job is still in the expected state when performing the update.
    LegacyPortabilityJob existingJob = store.find(jobId);
    // Verify no worker key
    Preconditions.checkState(existingJob.workerInstancePublicKey() == null);
    Preconditions.checkState(existingJob.workerInstancePrivateKey() == null);
    // Populate job with keys to persist
    String encodedPublicKey = PublicPrivateKeyPairGenerator.encodeKey(publicKey);
    String encodedPrivateKey = PublicPrivateKeyPairGenerator.encodeKey(privateKey);

    LegacyPortabilityJob updatedJob = existingJob.toBuilder()
        .setWorkerInstancePublicKey(encodedPublicKey)
        .setWorkerInstancePrivateKey(encodedPrivateKey)
        .setJobState(JobAuthorization.State.CREDS_ENCRYPTION_KEY_GENERATED)
        .build();
    store.update(jobId, updatedJob, JobAuthorization.State.CREDS_AVAILABLE);
  }

  /**
   * Polls for job with populated auth data and stops this service when found.
   */
  private void pollUntilJobIsReady() {
    UUID jobId = jobMetadata.getJobId();
    LegacyPortabilityJob job = store.find(jobId);
    if (job == null) {
      logger.debug("Could not poll job {}, it was not present in the key-value store", jobId);
    } else if (job.jobState() == JobAuthorization.State.CREDS_ENCRYPTED) {
      logger.debug("Polled job {} in state CREDS_ENCRYPTED", jobId);
      if (!Strings.isNullOrEmpty(job.encryptedExportAuthData())
          && !Strings.isNullOrEmpty(job.encryptedImportAuthData())) {
        logger.debug("Polled job {} has auth data as expected. Done polling.", jobId);
      } else {
        logger.warn("Polled job {} does not have auth data as expected. "
            + "Done polling this job since it's in a bad state! Starting over.", jobId);
      }
      this.stopAsync();
    } else {
      logger.debug("Polling job {} until it's in state CREDS_ENCRYPTED. "
          + "It's currently in state: {}", jobId, job.jobState());
    }
  }
}
