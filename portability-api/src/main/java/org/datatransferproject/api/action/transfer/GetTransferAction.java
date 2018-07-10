package org.datatransferproject.api.action.transfer;

import com.google.inject.Inject;
import org.datatransferproject.api.action.Action;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.types.client.transfer.GetTransfer;
import org.datatransferproject.types.client.transfer.Transfer;

import java.util.UUID;

import static org.datatransferproject.api.action.ActionUtils.decodeJobId;

/** Returns a transfer request */
public class GetTransferAction implements Action<GetTransfer, Transfer> {
  private JobStore jobStore;

  @Inject
  public GetTransferAction(JobStore jobStore) {
    this.jobStore = jobStore;
  }

  @Override
  public Class<GetTransfer> getRequestType() {
    return GetTransfer.class;
  }

  @Override
  public Transfer handle(GetTransfer transferRequest) {
    String id = transferRequest.getId();
    UUID jobId = decodeJobId(id);

    PortabilityJob job = jobStore.findJob(jobId);

    // TODO implement state
    Transfer.State state = Transfer.State.CREATED;

    return new Transfer(
        id, state, null, job.exportService(), job.exportService(), job.transferDataType());
  }
}
