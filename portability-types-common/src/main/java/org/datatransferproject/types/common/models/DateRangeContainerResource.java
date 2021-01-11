/*
 * Copyright 2019 The Data Transfer Project Authors.
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

package org.datatransferproject.types.common.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Objects;

/** A date range container containing start and end date for filtering. */
@JsonTypeName("org.dataportability:DateRangeContainerResource")
public class DateRangeContainerResource extends ContainerResource {
  private final Integer startDate;
  private final Integer endDate;

  @JsonCreator
  public DateRangeContainerResource(
          @JsonProperty("startDate") Integer startDate,
          @JsonProperty("endDate") Integer endDate) {
    this.startDate = startDate;
    this.endDate = endDate;
  }

  public Integer getStartDate() {
    return startDate;
  }
  public Integer getEndDate() { return endDate; }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DateRangeContainerResource that = (DateRangeContainerResource) o;
    return Objects.equals(startDate, that.startDate)
            && Objects.equals(endDate, that.endDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(startDate, endDate);
  }
}
