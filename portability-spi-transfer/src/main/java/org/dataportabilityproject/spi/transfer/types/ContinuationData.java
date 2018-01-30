package org.dataportabilityproject.spi.transfer.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.dataportabilityproject.types.transfer.EntityType;

import java.util.ArrayList;
import java.util.List;
import org.dataportabilityproject.types.transfer.models.ContainerResource;

/**
 * Specifies the starting point and context information for an export operation.
 *
 * REVIEW: This combines the original ContinuationInformation and ExportInformation
 */
@JsonTypeName("org.dataportability:Continuation")
public class ContinuationData extends EntityType {
    private List<ContainerResource> containerResources = new ArrayList<>();
    private final PaginationData paginationData;

    @JsonCreator
    public ContinuationData(@JsonProperty("paginationData") PaginationData paginationData) {
        this.paginationData = paginationData;
    }

    /**
     * Returns the container resources associated with the current export operation.
     */
    public List<ContainerResource> getContainerResources() {
        return containerResources;
    }

    /**
     * Returns the pagination data associated with the current operation. May be null.
     */
    public PaginationData getPaginationData() {
        return paginationData;
    }

    /**
     * Adds a container resource.
     *
     * @param resource the resource
     */
    public void addContainerResource(ContainerResource resource) {
        containerResources.add(resource);
    }
}
