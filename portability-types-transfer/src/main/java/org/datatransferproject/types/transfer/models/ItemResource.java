package org.datatransferproject.types.transfer.models;

import org.datatransferproject.types.common.models.DataModel;

/**
 * A resource that contains a non-container resource, such as a photo or calendar event.
 *
 * <p>Concrete subtypes must use {@link com.fasterxml.jackson.annotation.JsonTypeName} to specify a
 * type descriminator used for deserialization.
 */
public abstract class ItemResource extends DataModel {

}
