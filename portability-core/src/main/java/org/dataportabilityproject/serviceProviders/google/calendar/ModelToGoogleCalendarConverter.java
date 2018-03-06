package org.dataportabilityproject.serviceProviders.google.calendar;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import java.util.stream.Collectors;
import org.dataportabilityproject.dataModels.calendar.CalendarAttendeeModel;
import org.dataportabilityproject.dataModels.calendar.CalendarEventModel;
import org.dataportabilityproject.dataModels.calendar.CalendarModel;

public class ModelToGoogleCalendarConverter {

  static EventAttendee transformToEventAttendee(CalendarAttendeeModel attendee) {
    return new EventAttendee()
        .setDisplayName(attendee.getDisplayName())
        .setEmail(attendee.getEmail())
        .setOptional(attendee.isOptional());
  }

  private static EventDateTime getEventDateTime(CalendarEventModel.CalendarEventTime dateTime) {
    if (dateTime == null) {
      return null;
    }

    EventDateTime eventDateTime = new EventDateTime();

    // google's APIs want millisecond from epoch, and the timezone offset in minutes.
    if (dateTime.isDateOnly()) {
      eventDateTime.setDate(
          new DateTime(
              true,
              dateTime.getDateTime().toEpochSecond() * 1000,
              dateTime.getDateTime().getOffset().getTotalSeconds() / 60));
    } else {
      eventDateTime.setDateTime(
          new DateTime(
              dateTime.getDateTime().toEpochSecond() * 1000,
              dateTime.getDateTime().getOffset().getTotalSeconds() / 60));
    }

    return eventDateTime;
  }

  static com.google.api.services.calendar.model.Calendar convertToGoogleCalendar(
      CalendarModel calendarModel) {
    com.google.api.services.calendar.model.Calendar calendar =
        new com.google.api.services.calendar.model.Calendar()
            .setSummary("Copy of - " + calendarModel.getName())
            .setDescription(calendarModel.getDescription());
    return calendar;
  }

  static Event convertToGoogleCalendarEvent(CalendarEventModel eventModel) {
    Event event =
        new Event()
            .setLocation(eventModel.getLocation())
            .setDescription(eventModel.getTitle())
            .setSummary(eventModel.getNotes())
            .setStart(getEventDateTime(eventModel.getStartTime()))
            .setEnd(getEventDateTime(eventModel.getEndTime()));
    if (eventModel.getAttendees() != null) {
      event.setAttendees(
          eventModel
              .getAttendees()
              .stream()
              .map(ModelToGoogleCalendarConverter::transformToEventAttendee)
              .collect(Collectors.toList()));
    }
    return event;
  }
}
