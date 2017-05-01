package org.dataportabilityproject.serviceProviders.flickr;

import org.dataportabilityproject.dataModels.PaginationInformation;

final class FlickrPaginationInformation implements PaginationInformation {
  private final int page;

  FlickrPaginationInformation(int page) {
    this.page = page;
  }

  public int getPage() {
    return page;
  }
}
