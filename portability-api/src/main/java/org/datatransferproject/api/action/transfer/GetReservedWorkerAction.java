package org.datatransferproject.api.action.transfer;

import com.google.api.client.util.Preconditions;
import com.google.inject.Inject;
import org.datatransferproject.api.action.Action;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.types.client.transfer.GetReservedWorker;
import org.datatransferproject.types.client.transfer.ReservedWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.datatransferproject.api.action.ActionUtils.decodeJobId;
import static org.datatransferproject.spi.cloud.types.JobAuthorization.State.CREDS_ENCRYPTION_KEY_GENERATED;

/** Requests the worker that was reserved for a transfer job. */
public class GetReservedWorkerAction implements Action<GetReservedWorker, ReservedWorker> {
  private static final Logger logger = LoggerFactory.getLogger(GetReservedWorkerAction.class);

  private JobStore jobStore;

  @Inject
  public GetReservedWorkerAction(JobStore jobStore) {
    this.jobStore = jobStore;
  }

  @Override
  public Class<GetReservedWorker> getRequestType() {
    return GetReservedWorker.class;
  }

  @Override
  public ReservedWorker handle(GetReservedWorker workerRequest) {
    String id = workerRequest.getId();
    UUID jobId = decodeJobId(id);

    PortabilityJob job = jobStore.findJob(jobId);
    Preconditions.checkNotNull(
        job, "Couldn't lookup worker for job " + id + " because the job doesn't exist");
    if (job.jobAuthorization().state() != CREDS_ENCRYPTION_KEY_GENERATED) {
      logger.debug("Job {} has not entered state CREDS_ENCRYPTION_KEY_GENERATED yet", jobId);
      return new ReservedWorker(null);
    }
    logger.debug(
        "Got job {} in state CREDS_ENCRYPTION_KEY_GENERATED, returning its public key", jobId);
    return new ReservedWorker(job.jobAuthorization().authPublicKey());
  }
}
