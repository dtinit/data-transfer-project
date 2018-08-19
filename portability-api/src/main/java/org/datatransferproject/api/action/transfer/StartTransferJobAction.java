package org.datatransferproject.api.action.transfer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import org.datatransferproject.api.action.Action;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.types.JobAuthorization;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.types.client.transfer.StartTransferJob;
import org.datatransferproject.types.client.transfer.TransferJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.PublicKey;
import java.util.UUID;

import static org.datatransferproject.api.action.ActionUtils.decodeJobId;
import static org.datatransferproject.spi.cloud.types.JobAuthorization.State.CREDS_STORED;

/** Starts a transfer job. */
public class StartTransferJobAction implements Action<StartTransferJob, TransferJob> {
  private static final Logger logger = LoggerFactory.getLogger(StartTransferJobAction.class);

  private final JobStore jobStore;

  @Inject
  StartTransferJobAction(JobStore jobStore) {
    this.jobStore = jobStore;
  }

  @Override
  public Class<StartTransferJob> getRequestType() {
    return StartTransferJob.class;
  }

  @Override
  public TransferJob handle(StartTransferJob startTransferJob) {
    String id = startTransferJob.getId();
    Preconditions.checkNotNull(id, "transfer job ID required for StartTransferJobAction");
    UUID jobId = decodeJobId(id);
    PortabilityJob job = jobStore.findJob(jobId);

    job = updateJobWithCredentials(jobId, job, startTransferJob.getEncryptedAuthData());

    return new TransferJob(
        id, job.exportService(), job.importService(), job.transferDataType(), null, null);
  }

  /**
   * Encrypt the export and import credentials with a new {@link SecretKey} and {@link PublicKey}
   * assigned to this job then update the data store to {@link JobAuthorization.State#CREDS_STORED}
   * state.
   */
  private PortabilityJob updateJobWithCredentials(UUID jobId, PortabilityJob job, String authData) {

    // Populate job with encrypted auth data
    JobAuthorization updatedJobAuthorization =
        job.jobAuthorization()
            .toBuilder()
            .setEncryptedAuthData(authData)
            .setState(CREDS_STORED)
            .build();
    job = job.toBuilder().setAndValidateJobAuthorization(updatedJobAuthorization).build();
    logger.debug("Updating job {} from CREDS_ENCRYPTION_KEY_GENERATED to CREDS_STORED", jobId);
    try {
      jobStore.updateJob(jobId, job);
      logger.debug("Updated job {} to CREDS_STORED", jobId);
    } catch (IOException e) {
      throw new RuntimeException("Unable to update job", e);
    }
    return job;
  }
}
