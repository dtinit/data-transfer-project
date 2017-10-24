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

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this.getClass())
        .add("resource", resource)
        .add("pageInfo", pageInfo)
        .toString();
  }
}
