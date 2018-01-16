package org.dataportabilityproject.spi.cloud.storage;


import org.dataportabilityproject.types.transfer.models.DataModel;

import java.io.InputStream;

/**
 * REVIEW: The original JobCache is per Job and per Service. Does that have to be the case? For example, if the data model id is random
 */
public interface JobCache {

    /**
     * Returns a model instance for the id of the given type or null if not found.
     */
    <T extends DataModel> T getData(Class<T> type, String id);

    /**
     * Stores the given model instance.
     */
    <T extends DataModel> void store(T model);

    /**
     * Stores a stream using the given key.
     */
    void store(String key, InputStream stream);

    /**
     * Returns stream data for the given key.
     */
    InputStream getStream(String key);

}
