/*
 * Copyright 2018 The Data Transfer Project Authors.
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
package org.datatransferproject.transfer.microsoft.transformer.calendar;

import static java.util.Collections.emptyList;
import static org.datatransferproject.transfer.microsoft.transformer.TransformConstants.CALENDAR_ID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.datatransferproject.transfer.microsoft.transformer.TransformerContext;
import org.datatransferproject.transfer.microsoft.transformer.common.TransformerHelper;
import org.datatransferproject.types.common.models.calendar.CalendarAttendeeModel;
import org.datatransferproject.types.common.models.calendar.CalendarEventModel;

/**
 * Maps from a Graph API calendar event resource as defined by:
 * https://developer.microsoft.com/en-us/graph/docs/api-reference/beta/resources/event
 */
public class ToCalendarEventModelTransformer
    implements BiFunction<Map<String, Object>, TransformerContext, CalendarEventModel> {

  @Override
  @SuppressWarnings("unchecked")
  public CalendarEventModel apply(Map<String, Object> event, TransformerContext context) {
    if (!"singleInstance"
        .equals(event.get("type"))) { // support single instances for now;recurring events later
      return null;
    }
    String calendarId = context.getProperty(CALENDAR_ID);

    String title = (String) event.getOrDefault("subject", "");
    String location = TransformerHelper.getOrDefault("location", "displayName", event, "");

    // Notes is itemBody resource type defined as: { "content": "string",  "contentType": "String"}
    String notes = TransformerHelper.getOrDefault("body", "content", event, "");

    List<Map<String, Object>> rawAttendees =
        (List<Map<String, Object>>) event.getOrDefault("attendees", emptyList());
    List<CalendarAttendeeModel> attendees = new ArrayList<>();
    for (Object rawAttendee : rawAttendees) {
      CalendarAttendeeModel attendee = context.transform(CalendarAttendeeModel.class, rawAttendee);
      if (attendee != null) {
        attendees.add(attendee);
      }
    }

    CalendarEventModel.CalendarEventTime startTime =
        context.transform(CalendarEventModel.CalendarEventTime.class, event.get("start"));
    if (startTime == null) {
      context.problem("Could not parse start time. Skipping event.");
      return null;
    }

    CalendarEventModel.CalendarEventTime endTime =
        context.transform(CalendarEventModel.CalendarEventTime.class, event.get("end"));
    if (endTime == null) {
      context.problem("Could not parse end time. Skipping event.");
      return null;
    }

    return new CalendarEventModel(
        calendarId, title, notes, attendees, location, startTime, endTime, null);
  }
}
