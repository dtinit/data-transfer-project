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
import com.google.common.util.concurrent.AbstractScheduledService;
import java.util.concurrent.TimeUnit;
import org.dataportabilityproject.job.JobManager;
import org.dataportabilityproject.job.PortabilityJob;

/**
 * A service that polls storage for a job to process in two steps:
 * <br>
 *   (1) find an unassigned job for this worker
 * <br>
 *   (2) wait until the job is ready to process (i.e. creds are available)
 */
class JobPollingService extends AbstractScheduledService {
  private final JobManager jobDao;

  JobPollingService(JobManager jobDao) {
    this.jobDao = jobDao;
  }

  @Override
  protected void runOneIteration() throws Exception {
    if (WorkerJobMetadata.getInstance().isInitialized()) {
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
    return AbstractScheduledService.Scheduler.newFixedDelaySchedule(0, 1, TimeUnit.MINUTES);
  }

  /**
   * Polls for job with populated auth data and stops this service when found.
   */
  private void pollUntilJobIsReady() {
    PortabilityJob job = jobDao.findExistingJob(WorkerJobMetadata.getInstance().getJobId());
    Preconditions.checkNotNull(job);
    // Validate job has auth data
    if ((job.exportAuthData() != null) && (job.importAuthData() != null)) {
      // Stop polling now that we have completed data
      this.stopAsync();
    }
  }

  /**
   * Polls for an unassigned job, and once found, initializes it.
   */
  private void pollForUnassignedJob() {
    String id = jobDao.findUnassignedJob();
    if (id != null) {
      Preconditions.checkState(!WorkerJobMetadata.getInstance().isInitialized());
      WorkerJobMetadata.getInstance().init(id);
    }
  }
}
