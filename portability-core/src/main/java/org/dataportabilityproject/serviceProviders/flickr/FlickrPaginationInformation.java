package org.dataportabilityproject.serviceProviders.flickr;

import com.google.common.base.MoreObjects;
import org.dataportabilityproject.dataModels.PaginationInformation;

final class FlickrPaginationInformation implements PaginationInformation {
  private final int page;

  FlickrPaginationInformation(int page) {
    this.page = page;
  }

  public int getPage() {
    return page;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("page", page)
        .toString();
  }
}
