package org.dataportabilityproject.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base model type that supports language interoperability and extensibility.
 *
 * Subtypes use {@link com.fasterxml.jackson.annotation.JsonTypeName} to define the concrete type key for de/serialization.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "ptype")
public abstract class PortableType {
}
