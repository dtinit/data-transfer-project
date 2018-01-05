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
import java.io.IOException;
import org.dataportabilityproject.PortabilityCopier;
import org.dataportabilityproject.PortabilityFlags;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.cloud.SupportedCloud;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.cloud.interfaces.PersistentKeyValueStore;
import org.dataportabilityproject.cloud.local.LocalCloudFactory;
import org.dataportabilityproject.job.JobDao;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.shared.PortableDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class to bootstrap a portabilty worker that will operate on a single job whose state
 * is held in WorkerJobMetadata.
 */
public class WorkerMain {
  private static CloudFactory cloudFactory = new LocalCloudFactory();
  private static final Logger logger = LoggerFactory.getLogger(WorkerMain.class);

  public static void main(String[] args) throws Exception {
    PortabilityFlags.parse();

    SupportedCloud cloud = PortabilityFlags.cloud();

    // Initialize all global objects
    PersistentKeyValueStore storage = cloudFactory.getPersistentKeyValueStore();
    JobDao jobDao = new JobDao(storage);

    // Start the polling service to poll for an unassigned job and when it's ready.
    pollForJob(jobDao);

    // Start the processing
    PortabilityJob job = getJob(jobDao);

    // Only load the two providers that are doing actually work.
    // TODO(willard): Only load two needed services here, after converting service name to class
    // name in the DAO.
    ServiceProviderRegistry registry = new ServiceProviderRegistry(
        cloudFactory, PortabilityFlags.supportedServiceProviders());

    processJob(job, registry);
    logger.info("Successfully processed jobId: {}", WorkerJobMetadata.getInstance().getJobId());
    System.exit(0);
  }

  private static void pollForJob(JobDao jobDao) {
    JobPollingService poller = new JobPollingService(jobDao);
    poller.startAsync();
    poller.awaitTerminated();
  }

  private static PortabilityJob getJob(JobDao jobDao) {
    System.out.println("Begin processing jobId: " + WorkerJobMetadata.getInstance().getJobId());
    String jobId = WorkerJobMetadata.getInstance().getJobId();
    PortabilityJob job = jobDao.findExistingJob(jobId);
    Preconditions.checkNotNull(job, "Job not found, id: %s");
    return job;
  }

  private static void processJob(PortabilityJob job, ServiceProviderRegistry registry) {

    PortableDataType dataType = PortableDataType.valueOf(job.dataType());
    try {
      try {
        if(true) return;
        PortabilityCopier
            .copyDataType(registry, dataType, job.exportService(), job.exportAuthData(),
                job.importService(), job.importAuthData(), job.id());
      } catch (IOException e) {
        System.err.println("Error processing jobId: " + WorkerJobMetadata.getInstance().getJobId()
            + ", error: " + e.getMessage());
        e.printStackTrace();

        System.exit(1);
      }
    } finally {
      cloudFactory.clearJobData(job.id());
    }
  }
}
