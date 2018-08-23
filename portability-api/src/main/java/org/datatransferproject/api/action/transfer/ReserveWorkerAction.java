package org.datatransferproject.api.action.transfer;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import org.datatransferproject.api.action.Action;
import org.datatransferproject.api.action.ActionUtils;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.types.JobAuthorization;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.types.client.transfer.ReserveWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

import static org.datatransferproject.api.action.ActionUtils.decodeJobId;
import static org.datatransferproject.spi.cloud.types.JobAuthorization.State.CREDS_AVAILABLE;

/** Reserves a worker to process a transfer job. */
public class ReserveWorkerAction implements Action<ReserveWorker, String> {
  private static final Logger logger = LoggerFactory.getLogger(ReserveWorkerAction.class);

  private final JobStore jobStore;

  @Inject
  ReserveWorkerAction(JobStore jobStore) {
    this.jobStore = jobStore;
  }

  @Override
  public Class<ReserveWorker> getRequestType() {
    return ReserveWorker.class;
  }

  @Override
  public String handle(ReserveWorker reserveWorker) {
    String id = reserveWorker.getId();
    Preconditions.checkNotNull(id, "transfer job ID required for ReserveWorkerAction");
    UUID jobId = decodeJobId(id);
    updateStateToCredsAvailable(jobId);
    return "";
  }

  /**
   * Update the job to state to {@code State.CREDS_AVAILABLE} in the store. This indicates to the
   * pool of workers that this job is available for processing.
   */
  private void updateStateToCredsAvailable(UUID jobId) {
    PortabilityJob job = jobStore.findJob(jobId);
    validateJob(job);

    // Set update job auth data
    JobAuthorization jobAuthorization =
        job.jobAuthorization().toBuilder().setState(CREDS_AVAILABLE).build();

    job = job.toBuilder().setAndValidateJobAuthorization(jobAuthorization).build();
    try {
      jobStore.updateJob(
          jobId,
          job,
          (previous, updated) ->
              Preconditions.checkState(
                  previous.jobAuthorization().state() == JobAuthorization.State.INITIAL));
      logger.debug("Updated job {} to CREDS_AVAILABLE", jobId);
    } catch (IOException e) {
      throw new RuntimeException("Unable to update job", e);
    }
  }

  // TODO: Consolidate validation with the internal PortabilityJob validation
  private void validateJob(PortabilityJob job) {
    // Validate
    String dataType = job.transferDataType();
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(dataType), "Missing valid dataTypeParam: %s", dataType);

    String exportService = job.exportService();
    Preconditions.checkArgument(
        ActionUtils.isValidExportService(exportService),
        "Missing valid exportService: %s",
        exportService);

    String importService = job.importService();
    Preconditions.checkArgument(
        ActionUtils.isValidImportService(importService),
        "Missing valid importService: %s",
        importService);
  }
}
