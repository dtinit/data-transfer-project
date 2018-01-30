package org.dataportabilityproject.spi.cloud.storage;

import org.dataportabilityproject.spi.cloud.types.LegacyPortabilityJob;
import org.dataportabilityproject.spi.cloud.types.LegacyPortabilityJob.JobState;
import org.dataportabilityproject.spi.cloud.types.PortabilityJob;
import org.dataportabilityproject.types.transfer.models.DataModel;

import java.io.IOException;
import java.io.InputStream;

/**
 * Implementations handle storage and retrieval of {@link LegacyPortabilityJob}s.
 *
 * This class is intended to be implemented by extensions that support storage in various back-end services.
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
    void create(LegacyPortabilityJob job) throws IOException;

    /**
     * Inserts a new {@link PortabilityJob} keyed by its job ID in the store.
     *
     * <p>To update an existing {@link PortabilityJob} instead, use {@link #update}.
     *
     * @throws IOException if a job already exists for {@code job}'s ID, or if there was a different
     * problem inserting the job.
     */
    default void createJob(PortabilityJob job) throws IOException {

    }

    /**
     * Atomically updates the entry for {@code job}'s ID to {@code job}, and verifies that it
     * previously existed in {@code previousState}.
     *
     * @throws IOException if the job was not in the expected state in the store, or there was
     * another problem updating it.
     */
    void update(LegacyPortabilityJob job) throws IOException;

    /**
     * Atomically updates the entry for {@code job}'s ID to {@code job}.
     *
     * @throws IOException if the job was not in the expected state in the store, or there was
     * another problem updating it.
     */
    default void updateJob(PortabilityJob job) throws IOException {
    }

    /**
     * Removes the {@link LegacyPortabilityJob} in the store keyed by {@code jobId}.
     *
     * @throws IOException if the job doesn't exist, or there was a different problem deleting it.
     */
    void remove(String jobId) throws IOException;

    /**
     * Returns the job for the id or null if not found.
     *
     * @param id the job id
     */
    default PortabilityJob findJob(String id) {
        throw new UnsupportedOperationException();
    }

    /**
     * Finds the {@link LegacyPortabilityJob} keyed by {@code jobId} in the store, or null if none found.
     */
    LegacyPortabilityJob find(String jobId);

    /**
     * Gets the ID of the first {@link LegacyPortabilityJob} in state {@code jobState} in the store, or
     * null if none found.
     */
    String findFirst(JobState jobState);

    <T extends DataModel> void create(String jobId, T model);

    /**
     * Updates the given model instance associated with a job.
     */
    <T extends DataModel> void update(String jobId, T model);

    /**
     * Returns a model instance for the id of the given type or null if not found.
     */
    <T extends DataModel> T findData(Class<T> type, String id);

    /**
     * Removes ther data model instance.
     */
    void removeData(String id);

    /**
     * Finds the {@link LegacyPortabilityJob} keyed by {@code jobId} in the store, and verify it is in
     * state {@code jobState}.
     */
    LegacyPortabilityJob find(String jobId, JobState jobState);

    void create(String jobId, String key, InputStream stream);

    InputStream getStream(String jobId, String key);


}
