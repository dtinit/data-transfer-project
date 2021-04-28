package org.datatransferproject.datatransfer.google.calendar;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.common.annotations.VisibleForTesting;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.common.GoogleStaticObjects;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.models.calendar.CalendarAttendeeModel;
import org.datatransferproject.types.common.models.calendar.CalendarContainerResource;
import org.datatransferproject.types.common.models.calendar.CalendarEventModel;
import org.datatransferproject.types.common.models.calendar.CalendarModel;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;

public class GoogleCalendarImporter implements
    Importer<TokensAndUrlAuthData, CalendarContainerResource> {

  private final GoogleCredentialFactory credentialFactory;
  private volatile Calendar calendarInterface;

  public GoogleCalendarImporter(GoogleCredentialFactory credentialFactory) {
    // calendarInterface lazily initialized for each request
    this(credentialFactory, null);
  }

  @VisibleForTesting
  GoogleCalendarImporter(GoogleCredentialFactory credentialFactory,
      Calendar calendarInterface) {
    this.credentialFactory = credentialFactory;
    this.calendarInterface = calendarInterface;
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
        .setDescription(eventModel.getNotes())
        .setSummary(eventModel.getTitle())
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
  public ImportResult importItem(UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      TokensAndUrlAuthData authData,
      CalendarContainerResource data) throws Exception {
    for (CalendarModel calendarModel : data.getCalendars()) {
      idempotentExecutor.executeAndSwallowIOExceptions(
          calendarModel.getId(),
          calendarModel.getName(),
          () -> importSingleCalendar(authData, calendarModel));
    }
    for (CalendarEventModel eventModel : data.getEvents()) {
      idempotentExecutor.executeAndSwallowIOExceptions(
          Integer.toString(eventModel.hashCode()),
          eventModel.getNotes(),
          () -> importSingleEvent(idempotentExecutor, authData, eventModel));
    }
    return ImportResult.OK;
  }

  @VisibleForTesting
  String importSingleCalendar(TokensAndUrlAuthData authData, CalendarModel calendarModel)
      throws IOException {
    com.google.api.services.calendar.model.Calendar toInsert = convertToGoogleCalendar(
        calendarModel);
    com.google.api.services.calendar.model.Calendar calendarResult =
        getOrCreateCalendarInterface(authData).calendars().insert(toInsert).execute();
    return calendarResult.getId();
  }

  @VisibleForTesting
  String importSingleEvent(IdempotentImportExecutor idempotentImportExecutor,
      TokensAndUrlAuthData authData,
      CalendarEventModel eventModel)
      throws IOException {
    Event event = convertToGoogleCalendarEvent(eventModel);
    String newCalendarId = idempotentImportExecutor.getCachedValue(eventModel.getCalendarId());
    return getOrCreateCalendarInterface(authData)
        .events()
        .insert(newCalendarId, event)
        .execute()
        .getId();
  }

  private Calendar getOrCreateCalendarInterface(TokensAndUrlAuthData authData) {
    return calendarInterface == null ? makeCalendarInterface(authData) : calendarInterface;
  }

  private synchronized Calendar makeCalendarInterface(TokensAndUrlAuthData authData) {
    Credential credential = credentialFactory.createCredential(authData);
    return new Calendar.Builder(
        credentialFactory.getHttpTransport(), credentialFactory.getJsonFactory(), credential)
        .setApplicationName(GoogleStaticObjects.APP_NAME)
        .build();
  }
}
