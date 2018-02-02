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
import java.util.concurrent.TimeUnit;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.cloud.interfaces.PersistentKeyValueStore;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.job.PortabilityJob.JobState;
import org.dataportabilityproject.job.PublicPrivateKeyPairGenerator;
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
  private final PersistentKeyValueStore store;
  private final WorkerJobMetadata jobMetadata;

  @Inject
  JobPollingService(CloudFactory cloudFactory, WorkerJobMetadata jobMetadata) {
    this.store = cloudFactory.getPersistentKeyValueStore();
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
    String jobId = store.findFirst(JobState.PENDING_WORKER_ASSIGNMENT);
    logger.debug("Polling for a job PENDING_WORKER_ASSIGNMENT");
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
      logger.debug("Updated job {} to ASSIGNED_WITHOUT_AUTH_DATA, publicKey length: {}",
          jobId, publicKey.getEncoded().length);
    } catch (IOException e) {
      logger.debug("Failed to claim job {}; it was probably already claimed by another worker",
          jobId);
    }
  }

  /**
   * Replaces a unassigned {@link PortabilityJob} in storage with the provided {@code jobId} in
   * assigned state with {@code publicKey} and {@code privateKey}.
   */
  private void updateJobStateToAssignedWithoutAuthData(String jobId, PublicKey publicKey,
      PrivateKey privateKey) throws IOException {
    // Lookup the job so we can append to its existing properties.
    // update will verify the job is still in the expected state when performing the update.
    PortabilityJob existingJob = store.find(jobId);
    // Verify no worker key
    Preconditions.checkState(existingJob.workerInstancePublicKey() == null);
    Preconditions.checkState(existingJob.workerInstancePrivateKey() == null);
    // Populate job with keys to persist
    String encodedPublicKey = PublicPrivateKeyPairGenerator.encodeKey(publicKey);
    String encodedPrivateKey = PublicPrivateKeyPairGenerator.encodeKey(privateKey);

    PortabilityJob updatedJob = existingJob.toBuilder()
        .setWorkerInstancePublicKey(encodedPublicKey)
        .setWorkerInstancePrivateKey(encodedPrivateKey)
        .setJobState(JobState.ASSIGNED_WITHOUT_AUTH_DATA)
        .build();
    store.update(updatedJob, JobState.PENDING_WORKER_ASSIGNMENT);
  }

  /**
   * Polls for job with populated auth data and stops this service when found.
   */
  private void pollUntilJobIsReady() {
    String jobId = jobMetadata.getJobId();
    PortabilityJob job = store.find(jobId);
    if (job == null) {
      logger.debug("Could not poll job {}, it was not present in the key-value store", jobId);
    } else if (job.jobState() == JobState.ASSIGNED_WITH_AUTH_DATA) {
      logger.debug("Polled job {} in state ASSIGNED_WITH_AUTH_DATA", jobId);
      if (!Strings.isNullOrEmpty(job.encryptedExportAuthData())
          && !Strings.isNullOrEmpty(job.encryptedImportAuthData())) {
        logger.debug("Polled job {} has auth data as expected. Done polling.", jobId);
      } else {
        logger.warn("Polled job {} does not have auth data as expected. "
            + "Done polling this job since it's in a bad state! Starting over.", jobId);
      }
      this.stopAsync();
    } else {
      logger.debug("Polling job {} until it's in state ASSIGNED_WITH_AUTH_DATA. "
          + "It's currently in state: {}", jobId, job.jobState());
    }
  }
}
