package org.dataportabilityproject.worker;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.io.IOException;
import org.dataportabilityproject.PortabilityCopier;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.job.JobDao;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.shared.PortableDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class WorkerImpl {
  private final Logger logger = LoggerFactory.getLogger(WorkerImpl.class);
  private final CloudFactory cloudFactory;
  private final JobPollingService jobPollingService;
  private final JobDao jobDao;
  private final ServiceProviderRegistry registry;

  @Inject
  WorkerImpl(
      CloudFactory cloudFactory,
      JobDao jobDao,
      JobPollingService jobPollingService,
      ServiceProviderRegistry registry) {
    this.cloudFactory = cloudFactory;
    this.jobPollingService = jobPollingService;
    this.jobDao = jobDao;
    this.registry = registry;
  }

  void processJob() {
    // Start the polling service to poll for an unassigned job and when it's ready.
    pollForJob();

    // Start the processing
    PortabilityJob job = getJob(jobDao);

    // Only load the two providers that are doing actually work.
    // TODO(willard): Only load two needed services here, after converting service name to class
    // name in the DAO.

    processJob(job);
    logger.info("Successfully processed jobId: {}", WorkerJobMetadata.getInstance().getJobId());
  }

  private void pollForJob() {
    jobPollingService.startAsync();
    jobPollingService.awaitTerminated();
  }

  private PortabilityJob getJob(JobDao jobDao) {
    logger.debug("Begin processing jobId: {}", WorkerJobMetadata.getInstance().getJobId());
    String jobId = WorkerJobMetadata.getInstance().getJobId();
    PortabilityJob job = jobDao.findExistingJob(jobId);
    Preconditions.checkNotNull(job, "Job not found, id: %s");
    return job;
  }

  private void processJob(PortabilityJob job) {

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
