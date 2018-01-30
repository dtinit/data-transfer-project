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
package org.dataportabilityproject.types.transfer.models.calendar;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.dataportabilityproject.types.transfer.models.ContainerResource;

/**
 * A Wrapper for all the possible objects that can be returned by a calendar exporter.
 */
@JsonTypeName("CalendarContainerResource")
public class CalendarContainerResource extends ContainerResource {
  private final Collection<CalendarModel> calendars;
  private final Collection<CalendarEventModel> events;

  @JsonCreator
  public CalendarContainerResource(
      @JsonProperty("calendars") Collection<CalendarModel> calendars,
      @JsonProperty("events") Collection<CalendarEventModel> events) {
    this.calendars = calendars == null ? ImmutableList.of() : calendars;
    this.events = events == null ? ImmutableList.of() : events;
  }
  public Collection<CalendarModel> getCalendars() {
    return calendars;
  }

  public Collection<CalendarEventModel> getEvents() {
    return events;
  }
}
