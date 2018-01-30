package org.dataportabilityproject.transfer.microsoft.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.dataportabilityproject.spi.transfer.types.PaginationData;

/**
 * Encapsulates the Microsoft Graph API OData next link.
 *
 * FIXME: This needs to be registered with the system-wide TypeManager.
 */
@JsonTypeName("org.dataportability:GraphPagination")
public class GraphPagination extends PaginationData {
    private String nextLink;

    @JsonCreator
    public GraphPagination(@JsonProperty("nextLink") String nextLink) {
        this.nextLink = nextLink;
    }

    public String getNextLink() {
        return nextLink;
    }
}
