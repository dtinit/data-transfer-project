/*
 * Copyright 2018 The Data Transfer Project Authors.
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
package org.datatransferproject.types.common.models.calendar;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
public class CalendarAttendeeModel {
  private final String displayName;
  private final String email;
  private final boolean optional;

  @JsonCreator
  public CalendarAttendeeModel(
      @JsonProperty("displayName") String displayName,
      @JsonProperty("email") String email,
      @JsonProperty("optional") boolean optional) {
    this.displayName = displayName;
    this.email = email;
    this.optional = optional;
  }

  public boolean getOptional() {
    return optional;
  }

  public String getEmail() {
    return email;
  }

  public String getDisplayName() {
    return displayName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CalendarAttendeeModel that = (CalendarAttendeeModel) o;
    return getOptional() == that.getOptional() &&
            Objects.equals(getDisplayName(), that.getDisplayName()) &&
            Objects.equals(getEmail(), that.getEmail());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getDisplayName(), getEmail(), getOptional());
  }
}
