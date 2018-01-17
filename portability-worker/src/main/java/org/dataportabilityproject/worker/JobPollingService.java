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
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.TimeUnit;
import org.dataportabilityproject.job.JobDao;
import org.dataportabilityproject.job.PortabilityJob;
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
  private final JobDao jobDao;
  private final WorkerJobMetadata jobMetadata;

  @Inject
  JobPollingService(JobDao jobDao, WorkerJobMetadata jobMetadata) {
    this.jobDao = jobDao;
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
    String id = jobDao.findNextJobPendingWorkerAssignment();
    logger.debug("Polled pollForUnassignedJob, found id: {}", id);
    if (id != null) {
      PortabilityJob job = jobDao.lookupJobPendingWorkerAssignment(id);
      Preconditions.checkNotNull(job);
      Preconditions.checkState(!jobMetadata.isInitialized());
      jobMetadata.init(id);

      PublicKey publicKey = jobMetadata.getKeyPair().getPublic();
      // TODO: Move storage of private key to a different location
      PrivateKey privateKey = jobMetadata.getKeyPair().getPrivate();
      // Executing Job State Transition from Unassigned to Assigned
      jobDao.updateJobStateToAssignedWithoutAuthData(id, publicKey, privateKey);
      logger.debug("Completed updateJobStateToAssignedWithoutAuthData, publicKey: {}", publicKey.getEncoded().length);
    } else {
      logger.debug("findNextJobPendingWorkerAssignment result was null");
    }
  }

  /**
   * Polls for job with populated auth data and stops this service when found.
   */
  private void pollUntilJobIsReady() {
    PortabilityJob job = jobDao
        .lookupAssignedWithAuthDataJob(jobMetadata.getJobId());
    logger.debug("Polled lookupAssignedWithAuthDataJob, found id: {}",
        (job != null ? job.id() : "null"));

    // Validate job has auth data
    if ((job != null) && (!Strings.isNullOrEmpty(job.encryptedExportAuthData()))
        && (!Strings.isNullOrEmpty(job.encryptedImportAuthData()))) {
      logger.debug("Polled lookupAssignedWithAuthDataJob, found auth data, id: {}", job.id());
      // Stop polling now that we have all the data ready to start the job
      this.stopAsync();
    } else {
      logger.debug("Polled lookupAssignedWithAuthDataJob, no auth data found, id: {}", jobMetadata.getJobId());
    }
  }
}
