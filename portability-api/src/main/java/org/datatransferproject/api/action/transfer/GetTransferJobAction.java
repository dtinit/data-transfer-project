package org.datatransferproject.api.action.transfer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import org.datatransferproject.api.action.Action;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.launcher.monitor.events.EventCode;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.types.client.transfer.GetTransferJob;
import org.datatransferproject.types.client.transfer.TransferJob;

import java.util.UUID;

import static java.lang.String.format;
import static org.datatransferproject.api.action.ActionUtils.decodeJobId;

/** Requests a transfer job. */
public class GetTransferJobAction implements Action<GetTransferJob, TransferJob> {
  private JobStore jobStore;
  private final Monitor monitor;

  @Inject
  public GetTransferJobAction(JobStore jobStore, Monitor monitor) {
    this.jobStore = jobStore;
    this.monitor = monitor;
  }

  @Override
  public Class<GetTransferJob> getRequestType() {
    return GetTransferJob.class;
  }

  @Override
  public TransferJob handle(GetTransferJob transferRequest) {
    String id = transferRequest.getId();
    Preconditions.checkNotNull(id, "transfer job ID required for GetTransferJobAction");
    UUID jobId = decodeJobId(id);

    PortabilityJob job = jobStore.findJob(jobId);

    monitor.debug(() -> format("Fetched job with jobId: %s", jobId), jobId, EventCode.API_GOT_TRANSFER_JOB);

    // TODO(#553): This list of nulls should be cleaned up when we refactor TransferJob.
    return new TransferJob(id, job.exportService(), job.importService(), job.transferDataType(),
            null, null, null, null, null, null);
  }
}
