package org.dataportabilityproject.spi.cloud.storage;

import java.io.IOException;
import javax.print.attribute.standard.JobState;
import org.dataportabilityproject.spi.cloud.types.PortabilityJob;

/**
 * Implementations handle storage and retrieval of {@link PortabilityJob}s.
 *
 * This class is intended to be implemented by extensions that support storage in various back-end services.
 */
public interface JobStore {

    /**
     * Inserts a new {@link PortabilityJob} keyed by its job ID in the store.
     *
     * <p>To update an existing {@link PortabilityJob} instead, use {@link #update}.
     *
     * @throws IOException if a job already exists for {@code job}'s ID, or if there was a different
     * problem inserting the job.
     */
    void create(PortabilityJob job) throws IOException;

    /**
     * Atomically updates the entry for {@code job}'s ID to {@code job}, and verifies that it
     * previously existed in {@code previousState}.
     *
     * @throws IOException if the job was not in the expected state in the store, or there was
     * another problem updating it.
     */
    void update(PortabilityJob job, JobState previousState) throws IOException;
    
    /**
     * Removes the {@link PortabilityJob} in the store keyed by {@code jobId}.
     *
     * @throws IOException if the job doesn't exist, or there was a different problem deleting it.
     */
    void remove(String jobId) throws IOException;

    /**
     * Finds the {@link PortabilityJob} keyed by {@code jobId} in the store, or null if none found.
     */
    PortabilityJob find(String jobId);

    /**
     * Finds the {@link PortabilityJob} keyed by {@code jobId} in the store, and verify it is in
     * state {@code jobState}.
     */
    PortabilityJob find(String jobId, JobState jobState);

    /**
     * Gets the ID of the first {@link PortabilityJob} in state {@code jobState} in the store, or
     * null if none found.
     */
    String findFirst(JobState jobState);
}
