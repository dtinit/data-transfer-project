package org.datatransferproject.spi.cloud.storage;

import static org.datatransferproject.spi.cloud.types.JobAuthorization.State.CREDS_AVAILABLE;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.UUID;
import org.datatransferproject.spi.cloud.types.JobAuthorization;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.spi.cloud.types.PortabilityJob.State;

public abstract class JobStoreWithValidator implements JobStore {

  public interface JobUpdateValidator {

    /**
     * Validation to do as part of an atomic update. Implementers should throw an {@code
     * IllegalStateException} if the validation fails.
     */
    void validate(PortabilityJob previous, PortabilityJob updated);
  }

  /**
   * Verifies a {@code PortabilityJob} already exists for {@code jobId}, and updates the entry to
   * {@code job}. If {@code validator} is non-null, validator.validate() is called first, as part of
   * the atomic update.
   *
   * @throws IOException if a job didn't already exist for {@code jobId} or there was a problem
   * updating it
   * @throws IllegalStateException if validator.validate() failed
   */
  protected abstract void updateJob(UUID jobId, PortabilityJob job, JobUpdateValidator validator)
      throws IOException;

  @Override
  public void claimJob(UUID jobId, PortabilityJob job) throws IOException {
    updateJob(jobId, job, (previous, updated) ->
        Preconditions.checkState(
            previous.jobAuthorization().state() == JobAuthorization.State.CREDS_AVAILABLE));
  }

  @Override
  public void updateJobState(
      UUID jobId, State state, State prevState, JobAuthorization.State prevAuthState)
      throws IOException {
    PortabilityJob existingJob = findJob(jobId);
    PortabilityJob updatedJob = existingJob.toBuilder().setState(state).build();

    updateJob(
        jobId,
        updatedJob,
        ((previous, updated) -> {
          Preconditions.checkState(previous.state() == prevState);
          Preconditions.checkState(previous.jobAuthorization().state() == prevAuthState);
        }));
  }

  @Override
  public void updateJobAuthStateToCredsAvailable(UUID jobId) throws IOException {
    PortabilityJob job = findJob(jobId);
    // Set update job auth data
    JobAuthorization jobAuthorization =
        job.jobAuthorization().toBuilder().setState(CREDS_AVAILABLE).build();
    job = job.toBuilder().setAndValidateJobAuthorization(jobAuthorization).build();
    updateJob(
        jobId,
        job,
        (previous, updated) -> validateForUpdateStateToCredsAvailable(previous));
  }


  private static void validateForUpdateStateToCredsAvailable(PortabilityJob job) {
    String dataType = job.transferDataType();
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(dataType), "Missing valid dataTypeParam: %s", dataType);

    String exportService = job.exportService();
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(exportService), "Missing valid exportService: %s", exportService);

    String importService = job.importService();
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(importService), "Missing valid importService: %s", importService);
    Preconditions.checkState(job.jobAuthorization().state() == JobAuthorization.State.INITIAL);
  }
}
