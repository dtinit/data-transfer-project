package org.dataportabilityproject.dataModels;

import java.util.Optional;

/**
 * Information about what data to export.
 */
public class ExportInformation {
  private final Optional<Resource> resource;
  private final Optional<PaginationInformation> pageInfo;

  public ExportInformation(Optional<Resource> resource, Optional<PaginationInformation> pageInfo) {
    this.resource = resource;
    this.pageInfo = pageInfo;
  }

  /** Information about the current resource being exported. */
  public Optional<Resource> getResource() {
    return resource;
  }

  /** Information about where to start exporting item if not at the start of a set. */
  public Optional<PaginationInformation> getPaginationInformation() {
    return this.pageInfo;
  }
}
