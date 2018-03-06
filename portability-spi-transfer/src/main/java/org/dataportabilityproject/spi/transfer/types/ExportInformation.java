package org.dataportabilityproject.spi.transfer.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.dataportabilityproject.types.transfer.EntityType;
import org.dataportabilityproject.types.transfer.models.ContainerResource;

/** Contains information about how to export data. */
@JsonTypeName("org.dataportability:ExportInformation")
public class ExportInformation extends EntityType {
  private final PaginationData paginationData;
  private final ContainerResource containerResource;

  /**
   * Construct a new instance.
   *
   * @param paginationData data about how to fetch the next page of data of the given resource may
   *     be null to indicate the first page of data should be fetched
   * @param containerResource the parent resource, the export call will export child items of this
   *     resource, may be null to indicate the root resource should be fetched
   */
  @JsonCreator
  public ExportInformation(PaginationData paginationData, ContainerResource containerResource) {
    this.paginationData = paginationData;
    this.containerResource = containerResource;
  }

  /**
   * Data about how to fetch the next page of data of the given resource may be null to indicate the
   * first page of data should be fetched
   */
  public PaginationData getPaginationData() {
    return paginationData;
  }

  /**
   * the parent resource, the export call will export child items of this resource, may be null to
   * indicate the root resource should be fetched
   */
  public ContainerResource getContainerResource() {
    return containerResource;
  }
}
