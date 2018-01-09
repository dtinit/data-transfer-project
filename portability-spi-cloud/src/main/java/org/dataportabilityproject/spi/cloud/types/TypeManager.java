package org.dataportabilityproject.spi.cloud.types;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Manages known JSON types and databinding.
 */
public interface TypeManager {

    /**
     * Returns the system-wide {@code ObjectMapper} that is configured to databind built-in and extension model types in the system.
     */
    ObjectMapper getMapper();

    /**
     * Registers a model type. Extensions that introduce new model subtypes must registered them here so they can be databound properly.
     *
     * @param type the type to register.
     */
    void registerType(Class<?> type);

}
