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
package org.dataportabilityproject.dataModels.calendar;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.dataportabilityproject.dataModels.ContinuationInformation;
import org.dataportabilityproject.dataModels.DataModel;

/**
 * A Wrapper for all the possible objects that can be returned by a calendar exporter.
 */
public class CalendarModelWrapper implements DataModel {
  private final Collection<CalendarModel> calendars;
  private final Collection<CalendarEventModel> events;
  private final ContinuationInformation continuationInformation;

  public CalendarModelWrapper(
      Collection<CalendarModel> calendars,
      Collection<CalendarEventModel> events,
      ContinuationInformation continuationInformation) {
    this.calendars = calendars == null ? ImmutableList.of() : calendars;
    this.events = events == null ? ImmutableList.of() : events;
    this.continuationInformation = continuationInformation;
  }
  public Collection<CalendarModel> getCalendars() {
    return calendars;
  }

  public Collection<CalendarEventModel> getEvents() {
    return events;
  }

  @Override public ContinuationInformation getContinuationInformation() {
    return continuationInformation;
  }
}
