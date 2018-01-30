package org.dataportabilityproject.types.transfer.models;

import org.dataportabilityproject.types.transfer.PortableType;

/**
 * A resource that contains data items such as a photo album or song list.
 * <p>
 * Concrete subtypes must use {@link com.fasterxml.jackson.annotation.JsonTypeName} to specify a type descriminator used for deserialization.
 */
public abstract class ContainerResource extends PortableType {

}
