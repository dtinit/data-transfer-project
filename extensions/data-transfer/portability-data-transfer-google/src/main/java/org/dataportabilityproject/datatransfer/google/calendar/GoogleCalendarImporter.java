package org.dataportabilityproject.datatransfer.google.calendar;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;
import org.dataportabilityproject.datatransfer.google.common.GoogleCredentialFactory;
import org.dataportabilityproject.datatransfer.google.common.GoogleStaticObjects;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.transfer.provider.ImportResult;
import org.dataportabilityproject.spi.transfer.provider.ImportResult.ResultType;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.spi.transfer.types.TempCalendarData;
import org.dataportabilityproject.types.transfer.auth.TokensAndUrlAuthData;
import org.dataportabilityproject.types.transfer.models.calendar.CalendarAttendeeModel;
import org.dataportabilityproject.types.transfer.models.calendar.CalendarContainerResource;
import org.dataportabilityproject.types.transfer.models.calendar.CalendarEventModel;
import org.dataportabilityproject.types.transfer.models.calendar.CalendarModel;

public class GoogleCalendarImporter implements
    Importer<TokensAndUrlAuthData, CalendarContainerResource> {

  private final GoogleCredentialFactory credentialFactory;
  private final JobStore jobStore;
  private volatile Calendar calendarInterface;

  public GoogleCalendarImporter(GoogleCredentialFactory credentialFactory, JobStore jobStore) {
    // calendarInterface lazily initialized for each request
    this(credentialFactory, jobStore, null);
  }

  @VisibleForTesting
  GoogleCalendarImporter(GoogleCredentialFactory credentialFactory, JobStore jobStore,
      Calendar calendarInterface) {
    this.credentialFactory = credentialFactory;
    this.jobStore = jobStore;
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
  public ImportResult importItem(UUID jobId, TokensAndUrlAuthData authData,
      CalendarContainerResource data) {
    try {
      for (CalendarModel calendarModel : data.getCalendars()) {
        importSingleCalendar(jobId, authData, calendarModel);
      }
      for (CalendarEventModel eventModel : data.getEvents()) {
        importSingleEvent(jobId, authData, eventModel);
      }
    } catch (IOException e) {
      // TODO(olsona): should consider retrying individual failures
      return new ImportResult(e);
    }
    return ImportResult.OK;
  }

  @VisibleForTesting
  void importSingleCalendar(UUID jobId, TokensAndUrlAuthData authData, CalendarModel calendarModel)
      throws IOException {
    com.google.api.services.calendar.model.Calendar toInsert = convertToGoogleCalendar(
        calendarModel);
    com.google.api.services.calendar.model.Calendar calendarResult =
        getOrCreateCalendarInterface(authData).calendars().insert(toInsert).execute();

    TempCalendarData calendarMappings = jobStore.findData(jobId, createCacheKey(), TempCalendarData.class);
    if (calendarMappings == null) {
      calendarMappings = new TempCalendarData(jobId);
      jobStore.create(jobId, createCacheKey(), calendarMappings);
    }
    calendarMappings.addIdMapping(calendarModel.getId(), calendarResult.getId());
    jobStore.update(jobId, createCacheKey(), calendarMappings);
  }

  @VisibleForTesting
  void importSingleEvent(UUID jobId, TokensAndUrlAuthData authData, CalendarEventModel eventModel)
      throws IOException {
    Event event = convertToGoogleCalendarEvent(eventModel);
    // calendarMappings better not be null!
    TempCalendarData calendarMappings = jobStore.findData(jobId, createCacheKey(), TempCalendarData.class);
    String newCalendarId = calendarMappings.getImportedId(eventModel.getCalendarId());
    getOrCreateCalendarInterface(authData)
        .events()
        .insert(newCalendarId, event)
        .execute();
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

  /** Key for cache of album mappings.
   * TODO: Add a method parameter for a {@code key} for fine grained objects.
   */
  private String createCacheKey() {
    // TODO: store objects containing individual mappings instead of single object containing all mappings
    return "tempCalendarData";
  }

}
