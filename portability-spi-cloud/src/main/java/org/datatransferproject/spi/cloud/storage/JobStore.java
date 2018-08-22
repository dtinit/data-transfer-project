package org.datatransferproject.spi.cloud.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.datatransferproject.spi.cloud.types.JobAuthorization;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.types.transfer.models.DataModel;

/**
 * A store for {@link PortabilityJob}s.
 *
 * <p>This class is intended to be implemented by extensions that support storage in various
 * back-end services.
 */
public interface JobStore {
  interface JobUpdateValidator {
    /**
     * Validation to do as part of an atomic update. Implementers should throw an
     * {@code IllegalStateException} if the validation fails.
     */
    void validate(PortabilityJob previous, PortabilityJob updated);
  }

  /**
   * Inserts a new {@link PortabilityJob} keyed by {@code jobId} in the store.
   *
   * <p>To update an existing {@link PortabilityJob} instead, use {@link #update}.
   *
   * @throws IOException if a job already exists for {@code job}'s ID, or if there was a different
   *     problem inserting the job.
   */
  void createJob(UUID jobId, PortabilityJob job) throws IOException;

  /**
   * Verifies a {@code PortabilityJob} already exists for {@code jobId}, and updates the entry to
   * {@code job}.
   *
   * @throws IOException if a job didn't already exist for {@code jobId} or there was a problem
   * updating it
   */
  void updateJob(UUID jobId, PortabilityJob job) throws IOException;

  /**
   * Verifies a {@code PortabilityJob} already exists for {@code jobId}, and updates the entry to
   * {@code job}. If {@code validator} is non-null, validator.validate() is called first, as part of
   * the atomic update.
   *
   * @throws IOException if a job didn't already exist for {@code jobId} or there was a problem
   * updating it
   * @throws IllegalStateException if validator.validate() failed
   */
  void updateJob(UUID jobId, PortabilityJob job, JobUpdateValidator validator) throws IOException;

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
   * Gets the ID of the first {@link PortabilityJob} in state {@code jobState} in the store,
   * or null if none found.
   */
  UUID findFirst(JobAuthorization.State jobState);

  default <T extends DataModel> void create(UUID jobId, String key, T model) throws IOException {
    throw new UnsupportedOperationException();
  }

  /** Updates the given model instance associated with a job. */
  default <T extends DataModel> void update(UUID jobId, String key, T model) {
    throw new UnsupportedOperationException();
  }

  /** Returns a model instance for the id of the given type or null if not found. */
  default <T extends DataModel> T findData(UUID jobId, String key, Class<T> type) {
    throw new UnsupportedOperationException();
  }

  /** Removes the data model instance. */
  default void removeData(UUID JobId, String key) {
    throw new UnsupportedOperationException();
  }

  default void create(UUID jobId, String key, InputStream stream) {
    throw new UnsupportedOperationException();
  }

  default InputStream getStream(UUID jobId, String key) {
    throw new UnsupportedOperationException();
  }
}
