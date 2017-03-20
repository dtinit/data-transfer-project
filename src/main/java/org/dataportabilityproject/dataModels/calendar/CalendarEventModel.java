package org.dataportabilityproject.dataModels.calendar;


import java.time.OffsetDateTime;
import java.util.List;

public class CalendarEventModel {
    private final String title;
    private final String notes;
    private final List<CalendarAttendeeModel> attendees;
    private final String location;
    private final CalendarEventTime startTime;
    private final CalendarEventTime endTime;

    public CalendarEventModel(String title,
                              String notes,
                              List<CalendarAttendeeModel> attendees,
                              String location,
                              CalendarEventTime startTime,
                              CalendarEventTime endTime) {
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

    public static class CalendarEventTime {
        private final OffsetDateTime dateTime;
        private final boolean dateOnly;

        public CalendarEventTime(OffsetDateTime dateTime, boolean dateOnly) {
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
