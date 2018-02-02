package org.dataportabilityproject.spi.cloud.storage;

import java.io.IOException;
import org.dataportabilityproject.spi.cloud.types.LegacyPortabilityJob;
import org.dataportabilityproject.spi.cloud.types.LegacyPortabilityJob.JobState;

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
     * Atomically updates the entry for {@code job}'s ID to {@code job}, and verifies that it
     * previously existed in {@code previousState}.
     *
     * @throws IOException if the job was not in the expected state in the store, or there was
     * another problem updating it.
     */
    void update(LegacyPortabilityJob job, JobState previousState) throws IOException;
    
    /**
     * Removes the {@link LegacyPortabilityJob} in the store keyed by {@code jobId}.
     *
     * @throws IOException if the job doesn't exist, or there was a different problem deleting it.
     */
    void remove(String jobId) throws IOException;

    /**
     * Finds the {@link LegacyPortabilityJob} keyed by {@code jobId} in the store, or null if none found.
     */
    LegacyPortabilityJob find(String jobId);

    /**
     * Finds the {@link LegacyPortabilityJob} keyed by {@code jobId} in the store, and verify it is in
     * state {@code jobState}.
     */
    LegacyPortabilityJob find(String jobId, JobState jobState);

    /**
     * Gets the ID of the first {@link LegacyPortabilityJob} in state {@code jobState} in the store, or
     * null if none found.
     */
    String findFirst(JobState jobState);
}
