/*
 * Copyright 2018 The Data-Portability Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dataportabilityproject.serviceProviders.google;

import com.google.common.base.MoreObjects;
import org.dataportabilityproject.dataModels.PaginationInformation;

public class GooglePaginationInfo implements PaginationInformation {
  private final String pageToken;

  public GooglePaginationInfo(String pageToken) {
    this.pageToken = pageToken;
  }

  public String getPageToken() {
    return this.pageToken;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("pageToken", pageToken)
        .toString();
  }

  @Override
  public boolean equals(Object object) {
    if (object == null) {
      return false;
    }
    if (!GooglePaginationInfo.class.isAssignableFrom(object.getClass())) {
      return false;
    }
    GooglePaginationInfo other = (GooglePaginationInfo) object;
    return this.pageToken.equals(other.getPageToken());
  }
}
