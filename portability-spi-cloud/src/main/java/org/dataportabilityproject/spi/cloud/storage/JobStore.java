package org.dataportabilityproject.spi.cloud.storage;

import org.dataportabilityproject.datatransfer.types.job.PortabilityJob;

/**
 * Implementations handle storage and retrieval of transfer jobs.
 *
 * This class is intended to be implemented by extensions that support storage in various back-end services.
 */
public interface JobStore {

    /**
     * Creates a new job.
     */
    void create(PortabilityJob job);

    /**
     * Updates and existing job.
     *
     * REVIEW: Maybe there should just be state transition-specific methods, e.g cancel(), complete(), etc.?
     */
    void update(PortabilityJob job);

    /**
     * Removes a job.
     *
     * REVIEW: Maybe there should just be state transition-specific methods, e.g cancel(), complete(), etc.?
     */
    void remove(PortabilityJob job);


}
