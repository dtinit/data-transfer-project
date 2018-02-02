/*
* Copyright 2017 The Data-Portability Project Authors.
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
package org.dataportabilityproject.worker;

import com.google.gson.Gson;
import com.google.inject.Inject;
import java.io.IOException;
import org.dataportabilityproject.PortabilityCopier;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.job.Crypter;
import org.dataportabilityproject.job.CrypterFactory;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.OldPortabilityJob;
import org.dataportabilityproject.spi.cloud.types.OldPortabilityJob.JobState;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class WorkerImpl {
  private static final Logger logger = LoggerFactory.getLogger(WorkerImpl.class);
  private static final Gson GSON = new Gson();

  private final CloudFactory cloudFactory;
  private final JobPollingService jobPollingService;
  private final JobStore store;
  private final ServiceProviderRegistry registry;
  private final WorkerJobMetadata workerJobMetadata;

  @Inject
  WorkerImpl(
      CloudFactory cloudFactory,
      JobPollingService jobPollingService,
      ServiceProviderRegistry registry,
      WorkerJobMetadata workerJobMetadata) {
    this.cloudFactory = cloudFactory;
    this.jobPollingService = jobPollingService;
    this.store = cloudFactory.getJobStore();
    this.registry = registry;
    this.workerJobMetadata = workerJobMetadata;
  }

  void processJob() {
    // Start the polling service to poll for an unassigned job and when it's ready.
    pollForJob();

    // Start the processing
    String jobId = workerJobMetadata.getJobId();
    logger.debug("Begin processing jobId: {}", jobId);
    OldPortabilityJob job = store.find(jobId, JobState.ASSIGNED_WITH_AUTH_DATA);

    // Only load the two providers that are doing actually work.
    // TODO(willard): Only load two needed services here, after converting service name to class
    // name in storage.

    processJob(job);
    logger.info("Successfully processed jobId: {}", workerJobMetadata.getJobId());
  }

  private void pollForJob() {
    jobPollingService.startAsync();
    jobPollingService.awaitTerminated();
  }

  private void processJob(OldPortabilityJob job) {

    PortableDataType dataType = PortableDataType.valueOf(job.dataType());
    try {
      Crypter decrypter = CrypterFactory.create(workerJobMetadata.getKeyPair().getPrivate());
      String serializedExportAuthData = decrypter.decrypt(job.encryptedExportAuthData());
      AuthData exportAuthData = deSerialize(serializedExportAuthData);
      String serializedImportAuthData = decrypter.decrypt(job.encryptedImportAuthData());
      AuthData importAuthData = deSerialize(serializedImportAuthData);
      PortabilityCopier
          .copyDataType(registry, dataType, job.exportService(), exportAuthData,
              job.importService(), importAuthData, job.id());
    } catch (IOException e) {
      logger.error("Error processing jobId: {}" + workerJobMetadata.getJobId(), e);
    } finally {
      cloudFactory.clearJobData(job.id());
    }
  }


  // TODO: Switch to using Jackson in the new transfer types
  private static AuthData deSerialize(String serialized) {
    return GSON.fromJson(serialized, AuthData.class);
  }
}
