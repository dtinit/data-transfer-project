package org.datatransferproject.api.action.transfer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import org.datatransferproject.api.action.Action;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.launcher.monitor.events.EventCode;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.types.client.transfer.ReserveWorker;
import org.datatransferproject.types.client.transfer.ReservedWorker;

import java.io.IOException;
import java.util.UUID;

import static java.lang.String.format;
import static org.datatransferproject.api.action.ActionUtils.decodeJobId;

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

  private void updateStateToCredsAvailable(UUID jobId) {
    try {
      jobStore.updateJobAuthStateToCredsAvailable(jobId);
      monitor.debug(() -> format("Updated job %s to CREDS_AVAILABLE", jobId), jobId,
          EventCode.API_JOB_CREDS_AVAILABLE);
    } catch (IOException e) {
      throw new RuntimeException("Unable to update job", e);
    }
  }
}
