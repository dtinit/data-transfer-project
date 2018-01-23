package org.dataportabilityproject.spi.cloud.storage;

import org.dataportabilityproject.spi.cloud.types.PortabilityJob;
import org.dataportabilityproject.types.transfer.models.DataModel;

import java.io.InputStream;

/**
 * Implementations handle storage and retrieval of transfer job data.
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

    /**
     * Returns a model instance for the id of the given type or null if not found.
     */
    <T extends DataModel> T getData(Class<T> type, String id);

    /**
     * Stores the given model instance associated with a job.
     */
    <T extends DataModel> void store(String jobId, T model);

    /**
     * Stores a stream associated with a job using the given key.
     */
    void store(String key, String jobId, InputStream stream);

    /**
     * Returns stream data for the given key.
     */
    InputStream getStream(String key);


}
