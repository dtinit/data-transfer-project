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
import java.time.OffsetDateTime;
import java.util.List;

public class CalendarEventModel {

    private final String calendarId;
    private final String title;
    private final String notes;
    private final List<CalendarAttendeeModel> attendees;
    private final String location;
    private final CalendarEventTime startTime;
    private final CalendarEventTime endTime;

    @JsonCreator
    public CalendarEventModel(
            @JsonProperty("calendarId") String calendarId,
            @JsonProperty("title") String title,
            @JsonProperty("notes") String notes,
            @JsonProperty("attendees") List<CalendarAttendeeModel> attendees,
            @JsonProperty("location") String location,
            @JsonProperty("startTime") CalendarEventTime startTime,
            @JsonProperty("endTime") CalendarEventTime endTime) {
        this.calendarId = calendarId;
        this.title = title;
        this.notes = notes;
        this.attendees = attendees;
        this.location = location;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getTitle() {
        return title;
    }

    public List<CalendarAttendeeModel> getAttendees() {
        return attendees;
    }

    public String getLocation() {
        return location;
    }

    public CalendarEventTime getStartTime() {
        return startTime;
    }

    public CalendarEventTime getEndTime() {
        return endTime;
    }

    public String getNotes() {
        return notes;
    }

    public String getCalendarId() {
        return calendarId;
    }

    public static class CalendarEventTime {
        private final OffsetDateTime dateTime;
        private final boolean dateOnly;

        @JsonCreator
        public CalendarEventTime(
            @JsonProperty("dateTime") OffsetDateTime dateTime,
            @JsonProperty("dateOnly") boolean dateOnly) {
            this.dateTime = dateTime;
            this.dateOnly = dateOnly;
        }

        public OffsetDateTime getDateTime() {
            return dateTime;
        }

        public boolean isDateOnly() {
            return dateOnly;
        }
    }
}
