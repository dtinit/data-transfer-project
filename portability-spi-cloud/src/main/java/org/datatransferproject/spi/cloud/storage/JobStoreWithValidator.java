package org.datatransferproject.spi.cloud.storage;

import static org.datatransferproject.spi.cloud.types.JobAuthorization.State.CREDS_AVAILABLE;
import static org.datatransferproject.spi.cloud.types.JobAuthorization.State.CREDS_ENCRYPTION_KEY_GENERATED;
import static org.datatransferproject.spi.cloud.types.JobAuthorization.State.CREDS_STORED;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.UUID;
import org.datatransferproject.spi.cloud.types.JobAuthorization;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.spi.cloud.types.PortabilityJob.State;

public abstract class JobStoreWithValidator implements JobStore {

  @Override
  public void claimJob(UUID jobId, PortabilityJob job) throws IOException {
    updateJob(jobId, job, (previous, updated) ->
        Preconditions.checkState(
            previous.jobAuthorization().state() == JobAuthorization.State.CREDS_AVAILABLE));
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

  @Override
  public void updateJobWithCredentials(UUID jobId, PortabilityJob job) throws IOException {
    updateJob(
        jobId,
        job,
        ((previous, updated) -> {
          Preconditions.checkState(
              previous.jobAuthorization().state() == CREDS_ENCRYPTION_KEY_GENERATED);
          Preconditions.checkState(updated.jobAuthorization().state() == CREDS_STORED);
        }));
  }

  @Override
  public void markJobAsFinished(UUID jobId, State state) throws IOException {
    Preconditions.checkState(state == State.ERROR || state == State.COMPLETE);
    updateJobState(jobId, state, State.IN_PROGRESS, JobAuthorization.State.CREDS_STORED);
  }

  @Override
  public void markJobAsStarted(UUID jobId) throws IOException {
    updateJobState(jobId, State.IN_PROGRESS, State.NEW, JobAuthorization.State.CREDS_STORED);
  }

  @Override
  public void markJobAsTimedOut(UUID jobId) throws IOException {
    PortabilityJob job = findJob(jobId);
    updateJob(
        jobId,
        job.toBuilder()
            .setState(PortabilityJob.State.ERROR)
            .setAndValidateJobAuthorization(
                job.jobAuthorization()
                    .toBuilder()
                    .setState(JobAuthorization.State.TIMED_OUT)
                    .build())
            .build());
  }

  /**
   * Verifies a {@code PortabilityJob} already exists for {@code jobId}, and updates the entry to
   * {@code job}.
   *
   * @throws IOException if a job didn't already exist for {@code jobId} or there was a problem
   * updating it
   */
  protected abstract void updateJob(UUID jobId, PortabilityJob job) throws IOException;

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

  private void updateJobState(
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

  public void addFailureReasonToJob(
      UUID jobId, String failureReason)
      throws IOException {
    PortabilityJob existingJob = findJob(jobId);
    PortabilityJob updatedJob = existingJob.toBuilder().setFailureReason(failureReason).build();
    updateJob(jobId, updatedJob);
  }

  public interface JobUpdateValidator {

    /**
     * Validation to do as part of an atomic update. Implementers should throw an {@code
     * IllegalStateException} if the validation fails.
     */
    void validate(PortabilityJob previous, PortabilityJob updated);
  }
}
