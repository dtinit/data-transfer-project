package org.dataportabilityproject.serviceProviders.google.calendar;

import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import org.dataportabilityproject.dataModels.calendar.CalendarAttendeeModel;
import org.dataportabilityproject.dataModels.calendar.CalendarEventModel;
import org.dataportabilityproject.dataModels.calendar.CalendarModel;

public class GoogleCalendarToModelConverter {

  static CalendarAttendeeModel transformToModelAttendee(EventAttendee attendee) {
    return new CalendarAttendeeModel(attendee.getDisplayName(), attendee.getEmail(),
        Boolean.TRUE.equals(attendee.getOptional()));
  }

  static CalendarEventModel.CalendarEventTime getEventTime(EventDateTime dateTime) {
    if (dateTime == null) {
      return null;
    }

    OffsetDateTime offsetDateTime;

    if (dateTime.getDate() == null) {
      offsetDateTime = OffsetDateTime.parse(dateTime.getDateTime().toString());
    } else {
      offsetDateTime = OffsetDateTime.from(
          LocalDate.parse(dateTime.getDate().toString()).atStartOfDay(ZoneId.of("UTC")));
    }

    return new CalendarEventModel.CalendarEventTime(offsetDateTime, dateTime.getDate() != null);
  }

  static CalendarModel convertToCalendarModel(CalendarListEntry calendarData) {
    CalendarModel model = new CalendarModel(
        calendarData.getId(),
        calendarData.getSummary(),
        calendarData.getDescription());
    return model;
  }

  static CalendarEventModel convertToCalendarEventModel(String id, Event eventData) {
    List<EventAttendee> attendees = eventData.getAttendees();
    CalendarEventModel model = new CalendarEventModel(
        id,
        eventData.getDescription(),
        eventData.getSummary(),
        attendees == null ? null : attendees.stream()
            .map(GoogleCalendarToModelConverter::transformToModelAttendee)
            .collect(Collectors.toList()),
        eventData.getLocation(),
        getEventTime(eventData.getStart()),
        getEventTime(eventData.getEnd()));
    return model;
  }
}
