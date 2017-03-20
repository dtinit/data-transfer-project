package org.dataportabilityproject.serviceProviders.google.calendar;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.google.common.collect.ImmutableList;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.dataModels.calendar.CalendarAttendeeModel;
import org.dataportabilityproject.dataModels.calendar.CalendarEventModel;
import org.dataportabilityproject.dataModels.calendar.CalendarModel;
import org.dataportabilityproject.serviceProviders.google.GoogleStaticObjects;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

//TODO(repeated events are ignored)
public class GoogleCalendarService implements Exporter<CalendarModel>, Importer<CalendarModel> {
    private final Calendar calendarClient;

    public GoogleCalendarService(Credential credential) {
        this.calendarClient = new Calendar.Builder(
                GoogleStaticObjects.getHttpTransport(), GoogleStaticObjects.JSON_FACTORY, credential)
                .setApplicationName(GoogleStaticObjects.APP_NAME)
                .build();
    }

    @Override
    public Collection<CalendarModel> export() throws IOException {
        ImmutableList.Builder<CalendarModel> results = ImmutableList.builder();

        String pageToken = null;
        do {
            Calendar.CalendarList.List listRequest  = calendarClient.calendarList().list();
            if (pageToken != null) {
                listRequest.setPageToken(pageToken);
            }
            CalendarList listResult = listRequest.execute();
            if (listResult.getItems().isEmpty()) {
                pageToken = null;
            } else {
                for (CalendarListEntry calendarData : listResult.getItems()) {
                    CalendarModel model = new CalendarModel(
                            calendarData.getSummary(),
                            calendarData.getDescription(),
                            getCalendarEvents(calendarData.getId()));

                    results.add(model);
                }

                pageToken = listResult.getNextPageToken();
            }
        } while (!Strings.isNullOrEmpty(pageToken));

        return results.build();
    }

    private List<CalendarEventModel> getCalendarEvents(String id) throws IOException {
        ImmutableList.Builder<CalendarEventModel> results = ImmutableList.builder();

        String pageToken = null;
        do {
            Calendar.Events.List listRequest  = calendarClient.events().list(id).setMaxAttendees(100);
            if (pageToken != null) {
                listRequest.setPageToken(pageToken);
            }
            Events listResult = listRequest.execute();
            if (listResult.getItems().isEmpty()) {
                pageToken = null;
            } else {
                for (Event eventData : listResult.getItems()) {
                    List<EventAttendee> attendees = eventData.getAttendees();
                    CalendarEventModel model = new CalendarEventModel(
                            eventData.getDescription(),
                            eventData.getSummary(),
                            attendees == null ? null : attendees.stream()
                                    .map(GoogleCalendarService::transformToModelAttendee)
                                    .collect(Collectors.toList()),
                            eventData.getLocation(),
                            getEventTime(eventData.getStart()),
                            getEventTime(eventData.getEnd()));
                    results.add(model);
                }

                pageToken = listResult.getNextPageToken();
            }
        } while (!Strings.isNullOrEmpty(pageToken));


        return results.build();
    }

    @Override
    public void importItem(CalendarModel model) throws IOException {
        com.google.api.services.calendar.model.Calendar toInsert = new com.google.api.services.calendar.model.Calendar()
                .setSummary("Copy of - " + model.getName())
                .setDescription(model.getDescription());
        com.google.api.services.calendar.model.Calendar calendarResult =
                calendarClient.calendars().insert(toInsert).execute();

        for (CalendarEventModel eventModel : model.getEvents()) {
            Event event = new Event()
                    .setLocation(eventModel.getLocation())
                    .setDescription(eventModel.getTitle())
                    .setSummary(eventModel.getNotes())
                    .setStart(getEventDateTime(eventModel.getStartTime()))
                    .setEnd(getEventDateTime(eventModel.getEndTime()));
            if (eventModel.getAttendees() != null) {
                event.setAttendees(eventModel.getAttendees().stream()
                        .map(GoogleCalendarService::transformToEventAttendee)
                        .collect(Collectors.toList()));
            }

            calendarClient.events().insert(calendarResult.getId(), event).execute();
        }
    }

    private static CalendarAttendeeModel transformToModelAttendee(EventAttendee attendee) {
        return new CalendarAttendeeModel(attendee.getDisplayName(), attendee.getEmail(),
                Boolean.TRUE.equals(attendee.getOptional()));
    }

    private static EventAttendee transformToEventAttendee(CalendarAttendeeModel attendee) {
        return new EventAttendee()
                .setDisplayName(attendee.getDisplayName())
                .setEmail(attendee.getEmail())
                .setOptional(attendee.isOptional());
    }

    private static CalendarEventModel.CalendarEventTime getEventTime(EventDateTime dateTime) {
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

    private static EventDateTime getEventDateTime(CalendarEventModel.CalendarEventTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        EventDateTime eventDateTime = new EventDateTime();

        // google's APIs want millisecond from epoch, and the timezone offset in minutes.
        if (dateTime.isDateOnly()) {
            eventDateTime.setDate(new DateTime(true,
                    dateTime.getDateTime().toEpochSecond() * 1000,
                    dateTime.getDateTime().getOffset().getTotalSeconds() / 60));
        } else {
            eventDateTime.setDateTime(new DateTime(
                    dateTime.getDateTime().toEpochSecond() * 1000,
                    dateTime.getDateTime().getOffset().getTotalSeconds() / 60));
        }

        return eventDateTime;
    }
}
