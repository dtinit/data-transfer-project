package org.dataportabilityproject.dataModels;

import java.util.Collection;

/**
 * Information representing extra export calls that should be made,
 * either for information that is next under already returned items
 * or items that didn't fit in a previous page.
 */
public class ContinuationInformation {
  private final Collection<? extends  Resource> resources;
  private final PaginationInformation paginationInformation;

  public ContinuationInformation(
      Collection<? extends Resource> resources,
      PaginationInformation paginationInformation) {

    this.resources = resources;
    this.paginationInformation = paginationInformation;
  }

  public Collection<? extends Resource> getSubResources() {
    return resources;
  }
  public PaginationInformation getPaginationInformation() {
    return paginationInformation;
  }
}
