package org.datatransferproject.types.common;

/**
 * Contains pagination data for an export operation.
 *
 * <p>Concrete subtypes must use {@link com.fasterxml.jackson.annotation.JsonTypeName} to specify a
 * type descriminator used for deserialization.
 */

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
        @JsonSubTypes.Type(IntPaginationToken.class),
        @JsonSubTypes.Type(StringPaginationToken.class)
})
public abstract class PaginationData extends PortableType {}
