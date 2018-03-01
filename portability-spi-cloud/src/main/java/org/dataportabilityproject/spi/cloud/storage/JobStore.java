package org.dataportabilityproject.spi.cloud.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.dataportabilityproject.spi.cloud.types.JobAuthorization;
import org.dataportabilityproject.spi.cloud.types.LegacyPortabilityJob;
import org.dataportabilityproject.spi.cloud.types.PortabilityJob;
import org.dataportabilityproject.types.transfer.models.DataModel;

/**
 * A store for {@link PortabilityJob}s.
 *
 * This class is intended to be implemented by extensions that support storage in various back-end
 * services.
 */
public interface JobStore {

    /**
     * Inserts a new {@link LegacyPortabilityJob} keyed by its job ID in the store.
     *
     * <p>To update an existing {@link LegacyPortabilityJob} instead, use {@link #update}.
     *
     * @throws IOException if a job already exists for {@code job}'s ID, or if there was a different
     * problem inserting the job.
     */
    @Deprecated
    default void create(UUID jobId, LegacyPortabilityJob job) throws IOException {
        throw new UnsupportedOperationException(
            "This shouldn't be called any more from the new, modular code");
    }

    /**
     * Inserts a new {@link PortabilityJob} keyed by its job ID in the store.
     *
     * <p>To update an existing {@link PortabilityJob} instead, use {@link #update}.
     *
     * @throws IOException if a job already exists for {@code job}'s ID, or if there was a different
     * problem inserting the job.
     */
    void createJob(UUID jobId, PortabilityJob job) throws IOException;

    /**
     * Atomically updates the entry for {@code job}'s ID to {@code job}, and verifies that it
     * previously existed in {@code previousState}.
     *
     * @throws IOException if the job was not in the expected state in the store, or there was
     * another problem updating it.
     */
    @Deprecated
    default void update(UUID jobId, LegacyPortabilityJob job, JobAuthorization.State previousState)
        throws IOException {
        throw new UnsupportedOperationException(
            "This shouldn't be called any more from the new, modular code");
    }

    /**
     * Atomically updates the entry for {@code job}'s ID to {@code job}.
     *
     * @throws IOException if the job was not in the expected state in the store, or there was
     * another problem updating it.
     */
    void updateJob(UUID jobId, PortabilityJob job) throws IOException;

    /**
     * Removes the {@link LegacyPortabilityJob} in the store keyed by {@code jobId}.
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
     * Finds the {@link LegacyPortabilityJob} keyed by {@code jobId} in the store, or null if none
     * found.
     */
    @Deprecated
    default LegacyPortabilityJob find(UUID jobId) {
        throw new UnsupportedOperationException(
            "This shouldn't be called any more from the new, modular code");
    }

    /**
     * Finds the {@link LegacyPortabilityJob} keyed by {@code jobId} in the store, and verify it is
     * in state {@code jobState}.
     */
    @Deprecated
    default LegacyPortabilityJob find(UUID jobId, JobAuthorization.State jobState) {
        throw new UnsupportedOperationException(
            "This shouldn't be called any more from the new, modular code");
    }

    /**
     * Gets the ID of the first {@link LegacyPortabilityJob} in state {@code jobState} in the store,
     * or null if none found.
     */
    UUID findFirst(JobAuthorization.State jobState);

    default <T extends DataModel> void create(UUID jobId, T model) {
        throw new UnsupportedOperationException();
    }

    /**
     * Updates the given model instance associated with a job.
     */
    default <T extends DataModel> void update(UUID jobId, T model) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a model instance for the id of the given type or null if not found.
     */
    default <T extends DataModel> T findData(Class<T> type, UUID id) {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes the data model instance.
     */
    default void removeData(UUID id) {
        throw new UnsupportedOperationException();
    }

    default void create(UUID jobId, String key, InputStream stream) {
        throw new UnsupportedOperationException();
    }

    default InputStream getStream(UUID jobId, String key) {
        throw new UnsupportedOperationException();
    }
}
