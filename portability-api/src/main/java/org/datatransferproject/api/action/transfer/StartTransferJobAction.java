package org.datatransferproject.api.action.transfer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import org.datatransferproject.api.action.Action;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.launcher.monitor.events.EventCode;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.types.JobAuthorization;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.types.client.transfer.StartTransferJob;
import org.datatransferproject.types.client.transfer.TransferJob;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.PublicKey;
import java.util.UUID;

import static java.lang.String.format;
import static org.datatransferproject.api.action.ActionUtils.decodeJobId;
import static org.datatransferproject.spi.cloud.types.JobAuthorization.State.CREDS_STORED;

/** Starts a transfer job. */
public class StartTransferJobAction implements Action<StartTransferJob, TransferJob> {
  private final JobStore jobStore;
  private final Monitor monitor;

  @Inject
  StartTransferJobAction(JobStore jobStore, Monitor monitor) {
    this.jobStore = jobStore;
    this.monitor = monitor;
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

    String authData = startTransferJob.getEncryptedAuthData();

    job = updateJobWithCredentials(jobId, job, authData);

    return new TransferJob(
        id,
        job.exportService(),
        job.importService(),
        job.transferDataType(),
        null,
        null,
        null,
        null,
        null,
        null);
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
    monitor.debug(
        () -> format("Updating job %s from CREDS_ENCRYPTION_KEY_GENERATED to CREDS_STORED", jobId),
        jobId);
    try {
      jobStore.updateJobWithCredentials(jobId, job);
      monitor.debug(() -> format("Updated job %s to CREDS_STORED", jobId), jobId,
          EventCode.API_JOB_CREDS_STORED);
    } catch (IOException e) {
      throw new RuntimeException("Unable to update job", e);
    }
    return job;
  }
}
