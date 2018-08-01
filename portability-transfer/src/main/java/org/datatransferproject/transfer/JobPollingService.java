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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.datatransferproject.security.AsymmetricKeyGenerator;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.types.JobAuthorization;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A service that polls storage for a job to process in two steps: <br>
 * (1) find an unassigned job for this transfer worker <br>
 * (2) wait until the job is ready to process (i.e. creds are available)
 */
class JobPollingService extends AbstractScheduledService {
  private final Logger logger = LoggerFactory.getLogger(JobPollingService.class);
  private final JobStore store;
  private final AsymmetricKeyGenerator asymmetricKeyGenerator;

  @Inject
  JobPollingService(JobStore store, AsymmetricKeyGenerator asymmetricKeyGenerator) {
    this.store = store;
    this.asymmetricKeyGenerator = asymmetricKeyGenerator;
  }

  @Override
  protected void runOneIteration() throws Exception {
    if (JobMetadata.isInitialized()) {
      pollUntilJobIsReady();
    } else {
      // Poll for an unassigned job to process with this transfer worker instance.
      // Once a transfer worker instance is assigned, the client will populate storage with
      // auth data encrypted with this instances public key and the copy process can begin
      pollForUnassignedJob();
    }
  }

  // TODO: the delay should be more easily configurable
  // https://github.com/google/data-transfer-project/issues/400
  @Override
  protected Scheduler scheduler() {
    return AbstractScheduledService.Scheduler.newFixedDelaySchedule(0, 20, TimeUnit.SECONDS);
  }

  /**
   * Polls for an unassigned job, and once found, initializes the global singleton job metadata
   * object for this running instance of the transfer worker.
   */
  private void pollForUnassignedJob() {
    UUID jobId = store.findFirst(JobAuthorization.State.CREDS_AVAILABLE);
    logger.debug("Polling for a job in state CREDS_AVAILABLE");
    if (jobId == null) {
      return;
    }
    logger.debug("Polled job {}", jobId);
    Preconditions.checkState(!JobMetadata.isInitialized());
    KeyPair keyPair = asymmetricKeyGenerator.generate();
    PublicKey publicKey = keyPair.getPublic();
    // TODO: Back up private key (keyPair.getPrivate()) in case this transfer worker dies mid-copy,
    // so we don't have to make the user start from scratch. Some options are to manage this key
    // pair within our hosting platform's key management system rather than generating here, or to
    // encrypt and store the private key on the client.
    // Note: tryToClaimJob may fail if another transfer worker beat us to it. That's ok -- this transfer
    // worker will keep polling until it can claim a job.
    boolean claimed = tryToClaimJob(jobId, keyPair);
    if (claimed) {
      logger.debug(
          "Updated job {} to CREDS_ENCRYPTION_KEY_GENERATED, publicKey length: {}",
          jobId,
          publicKey.getEncoded().length);
    }
  }

  /** Claims {@link PortabilityJob} {@code jobId} and updates it with our public key in storage.
   * Returns true if the claim was successful; otherwise it returns false. */
  private boolean tryToClaimJob(UUID jobId, KeyPair keyPair) {
    // Lookup the job so we can append to its existing properties.
    PortabilityJob existingJob = store.findJob(jobId);
    // Verify no transfer worker key
    if (existingJob.jobAuthorization().workerPublicKey() != null) {
      logger.debug("public key cannot be persisted again");
      return false;
    }
    // Populate job with public key to persist
    String encodedPublicKey = BaseEncoding.base64Url().encode(keyPair.getPublic().getEncoded());

    PortabilityJob updatedJob =
        existingJob
            .toBuilder()
            .setAndValidateJobAuthorization(
                existingJob
                    .jobAuthorization()
                    .toBuilder()
                    .setWorkerPublicKey(encodedPublicKey)
                    .setState(JobAuthorization.State.CREDS_ENCRYPTION_KEY_GENERATED)
                    .build())
            .build();
    // Attempt to 'claim' this job by validating it is still in state CREDS_AVAILABLE as we
    // update it to state CREDS_ENCRYPTION_KEY_GENERATED, along with our key. If another transfer
    // instance polled the same job, and already claimed it, it will have updated the job's state
    // to CREDS_ENCRYPTION_KEY_GENERATED.
    try {
      store.updateJob(
          jobId,
          updatedJob,
          (previous, updated) ->
              Preconditions.checkState(
                  previous.jobAuthorization().state() == JobAuthorization.State.CREDS_AVAILABLE));
    } catch (IllegalStateException | IOException e) {
      logger.debug(
          "Could not 'claim' job "
              + jobId
              + ". It was probably already claimed by another transfer worker");
      return false;
    }
    JobMetadata.init(
        jobId,
        keyPair,
        existingJob.transferDataType(),
        existingJob.exportService(),
        existingJob.importService());
    return true;
  }

  /** Polls for job with populated auth data and stops this service when found. */
  private void pollUntilJobIsReady() {
    UUID jobId = JobMetadata.getJobId();
    PortabilityJob job = store.findJob(jobId);
    if (job == null) {
      logger.debug("Could not poll job {}, it was not present in the key-value store", jobId);
    } else if (job.jobAuthorization().state() == JobAuthorization.State.CREDS_ENCRYPTED) {
      logger.debug("Polled job {} in state CREDS_ENCRYPTED", jobId);
      JobAuthorization jobAuthorization = job.jobAuthorization();
      if (!Strings.isNullOrEmpty(jobAuthorization.encryptedExportAuthData())
          && !Strings.isNullOrEmpty(jobAuthorization.encryptedImportAuthData())) {
        logger.debug("Polled job {} has auth data as expected. Done polling.", jobId);
      } else {
        logger.warn(
            "Polled job {} does not have auth data as expected. "
                + "Done polling this job since it's in a bad state! Starting over.",
            jobId);
      }
      this.stopAsync();
    } else {
      logger.debug(
          "Polling job {} until it's in state CREDS_ENCRYPTED. " + "It's currently in state: {}",
          jobId,
          job.jobAuthorization().state());
    }
  }
}
