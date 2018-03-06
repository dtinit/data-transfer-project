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
package org.dataportabilityproject.dataModels;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import java.util.Collection;

/**
 * Information representing extra export calls that should be made, either for information that is
 * next under already returned items or items that didn't fit in a previous page.
 */
public class ContinuationInformation {
  private final Collection<? extends Resource> resources;
  private final PaginationInformation paginationInformation;

  public ContinuationInformation(
      Collection<? extends Resource> resources, PaginationInformation paginationInformation) {

    this.resources = (resources == null) ? ImmutableList.of() : resources;
    this.paginationInformation = paginationInformation;
  }

  public Collection<? extends Resource> getSubResources() {
    return resources;
  }

  public PaginationInformation getPaginationInformation() {
    return paginationInformation;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("resources", resources.size())
        .add("paginationInformation", paginationInformation)
        .toString();
  }
}
