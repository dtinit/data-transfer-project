package org.datatransferproject.api.action.transfer;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import org.datatransferproject.api.action.Action;
import org.datatransferproject.api.action.ActionUtils;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.types.JobAuthorization;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.types.client.transfer.ReserveWorker;
import org.datatransferproject.types.client.transfer.ReservedWorker;

import java.io.IOException;
import java.util.UUID;

import static java.lang.String.format;
import static org.datatransferproject.api.action.ActionUtils.decodeJobId;
import static org.datatransferproject.spi.cloud.types.JobAuthorization.State.CREDS_AVAILABLE;

/** Reserves a worker to process a transfer job. */
public class ReserveWorkerAction implements Action<ReserveWorker, ReservedWorker> {

  private final JobStore jobStore;
  private final Monitor monitor;

  @Inject
  ReserveWorkerAction(JobStore jobStore, Monitor monitor) {
    this.jobStore = jobStore;
    this.monitor = monitor;
  }

  @Override
  public Class<ReserveWorker> getRequestType() {
    return ReserveWorker.class;
  }

  @Override
  public ReservedWorker handle(ReserveWorker reserveWorker) {
    String id = reserveWorker.getId();
    Preconditions.checkNotNull(id, "transfer job ID required for ReserveWorkerAction");
    UUID jobId = decodeJobId(id);
    updateStateToCredsAvailable(jobId);
    // Instead of returning an empty string, return a ReservedWorker response with an empty public
    // key.
    // TODO(seehamrun): consider making this a ReserveWorkerResponse type that contains a status if
    // this was successful
    return new ReservedWorker("");
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
      monitor.debug(() -> format("Updated job %s to CREDS_AVAILABLE", jobId));
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
