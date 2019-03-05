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

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.datatransferproject.transfer.microsoft.transformer.TransformerContext;
import org.datatransferproject.types.common.models.calendar.CalendarAttendeeModel;
import org.datatransferproject.types.common.models.calendar.CalendarEventModel;

/**
 * Maps from a transfer event type to a Graph API event resource as defined by:
 * https://developer.microsoft.com/en-us/graph/docs/api-reference/v1.0/resources/event.
 */
public class ToGraphEventTransformer
    implements BiFunction<CalendarEventModel, TransformerContext, Map<String, Object>> {

  @Override
  public Map<String, Object> apply(CalendarEventModel eventModel, TransformerContext context) {
    Map<String, Object> graphCalendar = new LinkedHashMap<>();

    graphCalendar.put("subject", eventModel.getTitle());

    copyDateTime("start", eventModel.getStartTime(), graphCalendar);
    copyDateTime("end", eventModel.getStartTime(), graphCalendar);
    copyLocation(eventModel, graphCalendar);
    copyBody(eventModel, graphCalendar);
    copyAttendees(eventModel, graphCalendar);

    return graphCalendar;
  }

  private void copyLocation(CalendarEventModel eventModel, Map<String, Object> graphCalendar) {
    if (eventModel.getLocation() == null) {
      return;
    }
    Map<String, String> graphLocation = new HashMap<>();
    graphLocation.put("displayName", eventModel.getLocation());
    graphLocation.put("locationType", "Default");
    graphCalendar.put("location", graphLocation);
  }

  private void copyDateTime(
      String key,
      CalendarEventModel.CalendarEventTime dateTime,
      Map<String, Object> graphCalendar) {
    Map<String, String> graphDateTime = new HashMap<>();
    graphDateTime.put(
        "dateTime",
        dateTime.getDateTime().atZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime().toString());
    graphDateTime.put("timeZone", "UTC");
    graphCalendar.put(key, graphDateTime);
  }

  private void copyAttendees(CalendarEventModel eventModel, Map<String, Object> graphCalendar) {
    List<CalendarAttendeeModel> attendees = eventModel.getAttendees();
    if (attendees == null) {
      return;
    }
    List<Map<String, Object>> graphAttendees = new ArrayList<>();
    attendees.forEach(
        attendee -> {
          Map<String, Object> graphAttendee = new HashMap<>();
          graphAttendee.put("type", attendee.getOptional() ? "optional" : "required");

          HashMap<String, String> emailAddress = new HashMap<>();
          emailAddress.put("address", attendee.getEmail());
          emailAddress.put("name", attendee.getDisplayName());
          graphAttendee.put("emailAddress", emailAddress);

          graphAttendees.add(graphAttendee);
        });

    graphCalendar.put("attendees", graphAttendees);
  }

  private void copyBody(CalendarEventModel eventModel, Map<String, Object> graphCalendar) {
    String notes = eventModel.getNotes();
    if (notes == null) {
      return;
    }
    Map<String, String> body = new HashMap<>();
    body.put("contentType", "HTML");
    body.put("content", notes);
    graphCalendar.put("body", body);
  }
}
