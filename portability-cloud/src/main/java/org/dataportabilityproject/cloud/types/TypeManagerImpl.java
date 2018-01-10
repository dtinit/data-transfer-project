package org.dataportabilityproject.cloud.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dataportabilityproject.spi.cloud.types.TypeManager;

/**
 * Jackson-based type manager that supports polymorphic type handling.
 */
public class TypeManagerImpl implements TypeManager {
    private ObjectMapper objectMapper;

    public TypeManagerImpl() {
        objectMapper = new ObjectMapper();
    }

    public ObjectMapper getMapper() {
        return objectMapper;
    }

    public void registerType(Class<?> type) {
        objectMapper.registerSubtypes(type);
    }
}
