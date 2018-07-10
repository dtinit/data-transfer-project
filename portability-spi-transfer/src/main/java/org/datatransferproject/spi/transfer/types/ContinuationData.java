package org.datatransferproject.spi.transfer.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import org.datatransferproject.types.transfer.EntityType;
import org.datatransferproject.types.transfer.models.ContainerResource;

/** Specifies the starting point and context information for an export operation. */
@JsonTypeName("org.dataportability:Continuation")
public class ContinuationData extends EntityType {
  private final PaginationData paginationData;
  private List<ContainerResource> containerResources = new ArrayList<>();

  @JsonCreator
  public ContinuationData(@JsonProperty("paginationData") PaginationData paginationData) {
    this.paginationData = paginationData;
  }

  /** Returns the container resources associated with the current export operation. */
  public List<ContainerResource> getContainerResources() {
    return containerResources;
  }

  /** Returns the pagination data associated with the current operation. May be null. */
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
