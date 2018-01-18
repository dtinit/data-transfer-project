/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
