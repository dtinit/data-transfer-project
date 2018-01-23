package org.dataportabilityproject.spi.transfer.types;

import org.dataportabilityproject.types.transfer.PortableType;

/**
 * Contains pagination data for an export operation.
 * <p>
 * Concrete subtypes must use {@link com.fasterxml.jackson.annotation.JsonTypeName} to specify a type descriminator used for deserialization.
 */
public abstract class PaginationData extends PortableType {
}
