package org.datatransferproject.types.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.datatransferproject.types.common.models.ContainerResource;

import java.util.Objects;

/** Contains information about how to export data. */
@JsonTypeName("org.dataportability:ExportInformation")
public class ExportInformation extends PortableType {
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
  public ExportInformation(
      @JsonProperty("paginationData") PaginationData paginationData,
      @JsonProperty("containerResource") ContainerResource containerResource) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ExportInformation that = (ExportInformation) o;
    return Objects.equals(getPaginationData(), that.getPaginationData()) &&
            Objects.equals(getContainerResource(), that.getContainerResource());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getPaginationData(), getContainerResource());
  }
}
