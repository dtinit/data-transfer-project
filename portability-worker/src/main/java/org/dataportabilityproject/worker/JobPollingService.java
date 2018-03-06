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
import com.google.common.io.BaseEncoding;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import org.dataportabilityproject.security.AsymmetricKeyGenerator;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.JobAuthorization;
import org.dataportabilityproject.spi.cloud.types.PortabilityJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A service that polls storage for a job to process in two steps:
 * <br>
 * (1) find an unassigned job for this worker
 * <br>
 * (2) wait until the job is ready to process (i.e. creds are available)
 */
class JobPollingService extends AbstractScheduledService {
    private final Logger logger = LoggerFactory.getLogger(JobPollingService.class);
    private final JobStore store;
    private final WorkerJobMetadata jobMetadata;
    private final AsymmetricKeyGenerator asymmetricKeyGenerator;

    @Inject
    JobPollingService(JobStore store, WorkerJobMetadata jobMetadata,
                      AsymmetricKeyGenerator asymmetricKeyGenerator) {
        this.store = store;
        this.jobMetadata = jobMetadata;
        this.asymmetricKeyGenerator = asymmetricKeyGenerator;
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
        KeyPair keyPair = asymmetricKeyGenerator.generate();
        PublicKey publicKey = keyPair.getPublic();
        // TODO: Move storage of private key to a different location. One option is to manage this
        // key pair within our cloud platform's key management system rather than generating here.
        PrivateKey privateKey = keyPair.getPrivate();
        // Executing Job State Transition from Unassigned (auth state INITIAL) to Assigned (auth
        // state CREDS_ENCRYPTION_KEY_GENERATED).
        try {
            keyGenerated(jobId, keyPair);
            logger.debug("Updated job {} to CREDS_ENCRYPTION_KEY_GENERATED, publicKey length: {}",
                    jobId, publicKey.getEncoded().length);
        } catch (IOException e) {
            logger.debug(
                "Failed to claim job {}; it was probably already claimed by another worker", jobId);
        }
    }

    /**
     * Updates a unassigned {@link PortabilityJob} in storage with the provided {@code jobId} in
     * CREDS_ENCRYPTION_KEY_GENERATED state with {@code publicKey} and {@code privateKey}.
     */
    private void keyGenerated(UUID jobId, KeyPair keyPair) throws IOException {
        // Lookup the job so we can append to its existing properties.
        PortabilityJob existingJob = store.findJob(jobId);
        // Verify no worker key
        if (existingJob.jobAuthorization().encodedPublicKey() != null) {
            throw new IOException("public key cannot be persisted again");
        }
        // Populate job with public key to persist
        String encodedPublicKey = BaseEncoding.base64Url().encode(keyPair.getPublic().getEncoded());

        PortabilityJob updatedJob = existingJob.toBuilder()
                .setAndValidateJobAuthorization(existingJob.jobAuthorization().toBuilder()
                        .setEncodedPublicKey(encodedPublicKey)
                        .setState(JobAuthorization.State.CREDS_ENCRYPTION_KEY_GENERATED)
                        .build())
                .build();
        store.updateJob(jobId, updatedJob);
        jobMetadata.init(jobId, keyPair, existingJob.transferDataType(),
            existingJob.exportService(), existingJob.importService());
    }

    /**
     * Polls for job with populated auth data and stops this service when found.
     */
    private void pollUntilJobIsReady() {
        UUID jobId = jobMetadata.getJobId();
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
                logger.warn("Polled job {} does not have auth data as expected. "
                        + "Done polling this job since it's in a bad state! Starting over.", jobId);
            }
            this.stopAsync();
        } else {
            logger.debug("Polling job {} until it's in state CREDS_ENCRYPTED. "
                    + "It's currently in state: {}", jobId, job.jobAuthorization().state());
        }
    }
}
