package org.dataportabilityproject.datatransfer.google.calendar;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.common.annotations.VisibleForTesting;
import org.dataportabilityproject.datatransfer.google.common.GoogleStaticObjects;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.transfer.provider.ImportResult;
import org.dataportabilityproject.spi.transfer.provider.ImportResult.ResultType;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.spi.transfer.types.TempCalendarData;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.models.calendar.CalendarAttendeeModel;
import org.dataportabilityproject.types.transfer.models.calendar.CalendarContainerResource;
import org.dataportabilityproject.types.transfer.models.calendar.CalendarEventModel;
import org.dataportabilityproject.types.transfer.models.calendar.CalendarModel;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;

public class GoogleCalendarImporter implements Importer<AuthData, CalendarContainerResource> {

  private final JobStore jobStore;
  private volatile Calendar calendarInterface;

  public GoogleCalendarImporter(JobStore jobStore) {
    this.jobStore = jobStore;
  }

  @VisibleForTesting
  GoogleCalendarImporter(Calendar calendarInterface, JobStore jobStore) {
    this.calendarInterface = calendarInterface;
    this.jobStore = jobStore;
  }

  private static EventAttendee transformToEventAttendee(CalendarAttendeeModel attendee) {
    return new EventAttendee()
        .setDisplayName(attendee.getDisplayName())
        .setEmail(attendee.getEmail())
        .setOptional(attendee.getOptional());
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

  static com.google.api.services.calendar.model.Calendar convertToGoogleCalendar(
      CalendarModel
          calendarModel) {
    return new com.google.api.services.calendar.model.Calendar()
        .setSummary("Copy of - " + calendarModel.getName())
        .setDescription(calendarModel.getDescription());
  }

  static Event convertToGoogleCalendarEvent(CalendarEventModel eventModel) {
    Event event = new Event()
        .setLocation(eventModel.getLocation())
        .setDescription(eventModel.getTitle())
        .setSummary(eventModel.getNotes())
        .setStart(getEventDateTime(eventModel.getStartTime()))
        .setEnd(getEventDateTime(eventModel.getEndTime()));
    if (eventModel.getAttendees() != null) {
      event.setAttendees(eventModel.getAttendees().stream()
          .map(GoogleCalendarImporter::transformToEventAttendee)
          .collect(Collectors.toList()));
    }
    return event;
  }

  @Override
  public ImportResult importItem(UUID jobId, AuthData authData, CalendarContainerResource data) {
    try {
      for (CalendarModel calendarModel : data.getCalendars()) {
        importSingleCalendar(jobId, authData, calendarModel);
      }
      for (CalendarEventModel eventModel : data.getEvents()) {
        importSingleEvent(jobId, authData, eventModel);
      }
    } catch (IOException e) {
      // TODO(olsona): should consider retrying individual failures
      return new ImportResult(ResultType.ERROR, e.getMessage());
    }
    return ImportResult.OK;
  }

  @VisibleForTesting
  void importSingleCalendar(UUID jobId, AuthData authData, CalendarModel calendarModel)
      throws IOException {
    com.google.api.services.calendar.model.Calendar toInsert = convertToGoogleCalendar(
        calendarModel);
    com.google.api.services.calendar.model.Calendar calendarResult =
        getOrCreateCalendarInterface(authData).calendars().insert(toInsert).execute();

    TempCalendarData calendarMappings = jobStore.findData(TempCalendarData.class, jobId);
    if (calendarMappings == null) {
      calendarMappings = new TempCalendarData(jobId.toString());
      jobStore.create(jobId, calendarMappings);
    }
    calendarMappings.addIdMapping(calendarModel.getId(), calendarResult.getId());
    jobStore.update(jobId, calendarMappings);
  }

  @VisibleForTesting
  void importSingleEvent(UUID jobId, AuthData authData, CalendarEventModel eventModel)
      throws IOException {
    Event event = convertToGoogleCalendarEvent(eventModel);
    // calendarMappings better not be null!
    TempCalendarData calendarMappings = jobStore.findData(TempCalendarData.class, jobId);
    String newCalendarId = calendarMappings.getImportedId(eventModel.getCalendarId());
    getOrCreateCalendarInterface(authData)
        .events()
        .insert(newCalendarId, event)
        .execute();
  }

  private Calendar getOrCreateCalendarInterface(AuthData authData) {
    return calendarInterface == null ? makeCalendarInterface(authData) : calendarInterface;
  }

  private synchronized Calendar makeCalendarInterface(AuthData authData) {
    // TODO(olsona): get credential using authData
    Credential credential = null;
    return new Calendar.Builder(GoogleStaticObjects.getHttpTransport(),
        GoogleStaticObjects.JSON_FACTORY, credential)
        .setApplicationName(GoogleStaticObjects.APP_NAME)
        .build();
  }
}
