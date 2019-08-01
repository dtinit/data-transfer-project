package org.datatransferproject.spi.cloud.storage;

import org.datatransferproject.spi.cloud.types.JobAuthorization;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.spi.cloud.types.PortabilityJob.State;
import org.datatransferproject.types.transfer.errors.ErrorDetail;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

/**
 * A store for {@link PortabilityJob}s.
 *
 * <p>This class is intended to be implemented by extensions that support storage in various
 * back-end services.
 */
public interface JobStore extends TemporaryPerJobDataStore {
  /**
   * Inserts a new {@link PortabilityJob} keyed by {@code jobId} in the store.
   *
   * <p>To update an existing {@link PortabilityJob} instead, use {@link #update}.
   *
   * @throws IOException if a job already exists for {@code job}'s ID, or if there was a different
   * problem inserting the job.
   */
  void createJob(UUID jobId, PortabilityJob job) throws IOException;

  /**
   * Called by a transfer worker to claim the job matching {@code jobId}, and updates the entry to
   * {@code job} to set the new state and auth public key. This should be atomic and not allow
   * multiple workers to claim the same job.
   *
   * @throws IOException if a job didn't already exist for {@code jobId} or there was a problem
   *     updating it
   * @throws IllegalStateException if fails to successfully claim the job.
   */
  void claimJob(UUID jobId, PortabilityJob job) throws IOException;

  /**
   * Update the jobs auth state to {@code JobAuthorization.State.CREDS_AVAILABLE} in the store. This
   * indicates to the pool of workers that this job is available for processing.
   */
  void updateJobAuthStateToCredsAvailable(UUID jobId) throws IOException;

  /**
   * Updates the job to the new version with keys added and the auth state to {@link
   * JobAuthorization.State#CREDS_STORED} state.
   */
  void updateJobWithCredentials(UUID jobId, PortabilityJob job) throws IOException;

  /**
   * Stores errors related to a transfer job.
   *
   * @throws IOException if a job didn't already exist for {@code jobId} or there was a problem
   * updating it
   */
  void addErrorsToJob(UUID jobId, Collection<ErrorDetail> errors) throws IOException;

  /**
   * Updates a job to mark as finished.
   * @param state The new state of the job. Can be {@code State.ERROR} or {@code State.COMPLETE}.
   * @throws IOException if unable to update the job
   */
  void markJobAsFinished(UUID jobId, State state) throws IOException;

  /**
   * Updates a job to mark as in progress.
   * @throws IOException if unable to update the job
   */
  void markJobAsStarted(UUID jobId) throws IOException;

  /**
   * Called when a worker has been waiting for credentials for a job but has not received them and
   * timed out. The job should be set to {@code State.Error} and {@code
   * JobAuthorization.State.TIMED_OUT}.
   *
   * @throws IOException if unable to update the job
   */
  void markJobAsTimedOut(UUID jobId) throws IOException;

  /**
   * Removes the {@link PortabilityJob} in the store keyed by {@code jobId}.
   *
   * @throws IOException if the job doesn't exist, or there was a different problem deleting it.
   */
  void remove(UUID jobId) throws IOException;

  /**
   * Returns the job for the id or null if not found.
   *
   * @param jobId the job id
   */
  PortabilityJob findJob(UUID jobId);

  /**
   * Gets the ID of the first {@link PortabilityJob} in state {@code jobState} in the store, or null
   * if none found.
   */
  UUID findFirst(JobAuthorization.State jobState);
}
