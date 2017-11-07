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
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import javax.annotation.Nullable;
import org.dataportabilityproject.PortabilityCopier;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.cloud.CloudFactoryFactory;
import org.dataportabilityproject.cloud.SupportedCloud;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.cloud.interfaces.PersistentKeyValueStore;
import org.dataportabilityproject.cloud.local.LocalCloudFactory;
import org.dataportabilityproject.job.JobManager;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.Secrets;

/**
 * Main class to bootstrap a portabilty worker that will operate on a single job whose state
 * is held in WorkerJobMetadata.
 */
public class WorkerMain {
  private static CloudFactory cloudFactory = new LocalCloudFactory();

  public static void main(String[] args) throws Exception {

    // TODO: Make this configurable
    SupportedCloud cloud = SupportedCloud.LOCAL;

    // TODO: Worker should only request secrets for the services it needs
    Secrets secrets = new Secrets("secrets.csv");

    // Initialize all global objects
    PersistentKeyValueStore storage = cloudFactory.getPersistentKeyValueStore();
    JobManager jobDao = new JobManager(storage);
    ServiceProviderRegistry registry = new ServiceProviderRegistry(secrets, cloudFactory);

    // Start the polling service to poll for an unassigned job and when it's ready.
    pollForJob(jobDao);

    // Start the processing
    processJob(jobDao, registry);

    System.out.println("Successfully processed jobId: " + WorkerJobMetadata.getInstance().getJobId());
    System.exit(0);
  }

  private static void pollForJob(JobManager jobDao) {
    JobPollingService poller = new JobPollingService(jobDao);
    poller.startAsync();
    poller.awaitTerminated();
  }

  private static void processJob(JobManager jobDao, ServiceProviderRegistry registry) {
    System.out.println("Begin processing jobId: " + WorkerJobMetadata.getInstance().getJobId());
    String jobId = WorkerJobMetadata.getInstance().getJobId();
    PortabilityJob job = jobDao.findExistingJob(jobId);
    Preconditions.checkNotNull(job, "Job not found, id: %s");
    PortableDataType dataType = PortableDataType.valueOf(job.dataType());
    try {
      try {
        PortabilityCopier
            .copyDataType(registry, dataType, job.exportService(), job.exportAuthData(),
                job.importService(), job.importAuthData(), jobId);
      } catch (IOException e) {
        System.err.println("Error processing jobId: " + WorkerJobMetadata.getInstance().getJobId()
            + ", error: " + e.getMessage());
        e.printStackTrace();

        System.exit(1);
      }
    } finally {
      cloudFactory.clearJobData(jobId);
    }
  }
}
