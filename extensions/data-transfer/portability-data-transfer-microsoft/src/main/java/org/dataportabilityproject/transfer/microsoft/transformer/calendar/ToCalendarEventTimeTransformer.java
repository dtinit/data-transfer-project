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
package org.dataportabilityproject.transfer.microsoft.transformer.calendar;

import org.dataportabilityproject.transfer.microsoft.transformer.TransformerContext;
import org.dataportabilityproject.types.transfer.models.calendar.CalendarAttendeeModel;
import org.dataportabilityproject.types.transfer.models.calendar.CalendarEventModel;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static org.dataportabilityproject.transfer.microsoft.transformer.TransformConstants.CALENDAR_ID;

/**
 *
 */
public class ToCalendarEventModelTransformer implements BiFunction<Map<String, Object>, TransformerContext, CalendarEventModel> {

    @Override
    public CalendarEventModel apply(Map<String, Object> event, TransformerContext context) {
        String calendarId = context.getProperty(CALENDAR_ID);

        String title = (String) event.getOrDefault("subject", "");
        String location = (String) event.getOrDefault("location", "");

        List<CalendarAttendeeModel> attendees;

        CalendarEventModel.CalendarEventTime startTime = parseTime((Map<String, String>) event.get("start"), context);
        if (startTime == null) {
            context.problem("Could not parse start time. Skipping event.");
            return null;
        }
        CalendarEventModel.CalendarEventTime endTime = parseTime((Map<String, String>) event.get("end"), context);
        if (endTime == null) {
            context.problem("Could not parse end time. Skipping event.");
            return null;
        }


        return new CalendarEventModel(calendarId, title, null, null, location, startTime, endTime);
    }

    /**
     * Parses microsoft.graph.dateTimeTimeZone defined at https://developer.microsoft.com/en-us/graph/docs/api-reference/v1.0/resources/dateTimeTimeZone
     */
    CalendarEventModel.CalendarEventTime parseTime(Map<String, String> time, TransformerContext context) {
        if (time == null) {
            return null;
        }
        String dateTimeValue = time.get("dateTime");
        String timeZone = time.get("timeZone");
        if (dateTimeValue == null || timeZone == null) {
            return null;
        }

        try {
            OffsetDateTime dateTime = ZonedDateTime.of(LocalDateTime.parse(dateTimeValue), ZoneId.of(timeZone)).toOffsetDateTime();
            return new CalendarEventModel.CalendarEventTime(dateTime, false);
        } catch (DateTimeException e) {
            context.problem(e.getMessage());
            return null;
        }
    }

}
