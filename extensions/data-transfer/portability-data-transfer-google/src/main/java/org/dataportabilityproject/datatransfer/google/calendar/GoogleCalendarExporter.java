package org.dataportabilityproject.datatransfer.google.calendar;

import static org.dataportabilityproject.datatransfer.google.common.GoogleStaticObjects.CALENDAR_TOKEN_PREFIX;
import static org.dataportabilityproject.datatransfer.google.common.GoogleStaticObjects.EVENT_TOKEN_PREFIX;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.dataportabilityproject.datatransfer.google.common.GoogleStaticObjects;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.transfer.provider.ExportResult;
import org.dataportabilityproject.spi.transfer.provider.ExportResult.ResultType;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.types.ContinuationData;
import org.dataportabilityproject.spi.transfer.types.ExportInformation;
import org.dataportabilityproject.spi.transfer.types.IdOnlyContainerResource;
import org.dataportabilityproject.spi.transfer.types.PaginationData;
import org.dataportabilityproject.spi.transfer.types.StringPaginationToken;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.models.calendar.CalendarAttendeeModel;
import org.dataportabilityproject.types.transfer.models.calendar.CalendarContainerResource;
import org.dataportabilityproject.types.transfer.models.calendar.CalendarEventModel;
import org.dataportabilityproject.types.transfer.models.calendar.CalendarModel;

public class GoogleCalendarExporter implements Exporter<AuthData, CalendarContainerResource> {

  private volatile Calendar calendarInterface;
  private JobStore jobStore;  // TODO(olsona): use jobStore

  public GoogleCalendarExporter(JobStore jobStore) {
    this.jobStore = jobStore;
  }

  @VisibleForTesting
  GoogleCalendarExporter(Calendar calendarInterface, JobStore jobStore) {
    this.calendarInterface = calendarInterface;
    this.jobStore = jobStore;
  }

  @Override
  public ExportResult<CalendarContainerResource> export(AuthData authData) {
    return exportCalendars(authData, Optional.empty());
  }

  @Override
  public ExportResult<CalendarContainerResource> export(AuthData authData,
      ExportInformation exportInformation) {
    StringPaginationToken paginationToken = (StringPaginationToken) exportInformation
        .getPaginationData();
    if (paginationToken.getToken().startsWith(CALENDAR_TOKEN_PREFIX)) {
      // Next thing to export is more calendars
      return exportCalendars(authData, Optional.of(paginationToken));
    } else {
      // Next thing to export is events
      IdOnlyContainerResource idOnlyContainerResource = (IdOnlyContainerResource) exportInformation
          .getContainerResource();
      return getCalendarEvents(authData, idOnlyContainerResource.getId(),
          Optional.of(paginationToken));
    }
  }

  private ExportResult<CalendarContainerResource> exportCalendars(AuthData authData,
      Optional<PaginationData> pageData) {
    Calendar.CalendarList.List listRequest;
    CalendarList listResult;

    // Get calendar information
    try {
      listRequest = getOrCreateCalendarInterface(authData).calendarList().list();

      if (pageData.isPresent()) {
        StringPaginationToken paginationToken = (StringPaginationToken) pageData.get();
        Preconditions.checkState(paginationToken.getToken().startsWith(CALENDAR_TOKEN_PREFIX),
            "Token is not applicable");
        listRequest.setPageToken(((StringPaginationToken) pageData.get()).getToken()
            .substring(CALENDAR_TOKEN_PREFIX.length()));
      }

      listResult = listRequest.execute();
    } catch (IOException e) {
      return new ExportResult<CalendarContainerResource>(ExportResult.ResultType.ERROR,
          e.getMessage());
    }

    // Set up continuation data
    PaginationData nextPageData = null;
    if (listResult.getNextPageToken() != null) {
      nextPageData = new StringPaginationToken(
          CALENDAR_TOKEN_PREFIX + listResult.getNextPageToken());
    }
    ContinuationData continuationData = new ContinuationData(nextPageData);

    // Process calendar list
    List<CalendarModel> calendarModels = new ArrayList<>(listResult.getItems().size());
    for (CalendarListEntry calendarData : listResult.getItems()) {
      CalendarModel model = convertToCalendarModel(calendarData);
      continuationData.addContainerResource(new IdOnlyContainerResource(calendarData.getId()));
      calendarModels.add(model);
    }
    CalendarContainerResource calendarContainerResource = new CalendarContainerResource(
        calendarModels, null);

    // Get result type
    ExportResult.ResultType resultType = ResultType.CONTINUE;
    if (calendarModels.isEmpty()) {
      resultType = ResultType.END;
    }

    return new ExportResult<CalendarContainerResource>(resultType, calendarContainerResource,
        continuationData);
  }

  private ExportResult<CalendarContainerResource> getCalendarEvents(AuthData authData, String id,
      Optional<PaginationData> pageData) {
    Calendar.Events.List listRequest;
    Events listResult;

    // Get event information
    try {
      listRequest = getOrCreateCalendarInterface(authData).events().list(id)
          .setMaxAttendees(GoogleStaticObjects.MAX_ATTENDEES);
      if (pageData.isPresent()) {
        StringPaginationToken paginationToken = (StringPaginationToken) pageData.get();
        Preconditions.checkState(paginationToken.getToken().startsWith(EVENT_TOKEN_PREFIX),
            "Token is not applicable");
        listRequest.setPageToken(((StringPaginationToken) pageData.get()).getToken()
            .substring(EVENT_TOKEN_PREFIX.length()));
      }
      listResult = listRequest.execute();
    } catch (IOException e) {
      return new ExportResult<CalendarContainerResource>(ExportResult.ResultType.ERROR,
          e.getMessage());
    }

    // Set up continuation data
    PaginationData nextPageData = null;
    if (listResult.getNextPageToken() != null) {
      nextPageData = new StringPaginationToken(EVENT_TOKEN_PREFIX + listResult.getNextPageToken());
    }
    ContinuationData continuationData = new ContinuationData(nextPageData);

    // Process event list
    List<CalendarEventModel> eventModels = new ArrayList<>(listResult.getItems().size());
    for (Event eventData : listResult.getItems()) {
      CalendarEventModel model = convertToCalendarEventModel(id, eventData);
      eventModels.add(model);
    }
    CalendarContainerResource calendarContainerResource = new CalendarContainerResource(null,
        eventModels);

    // Get result type
    ExportResult.ResultType resultType = ResultType.CONTINUE;
    if (nextPageData == null) {
      resultType = ResultType.END;
    }

    return new ExportResult<CalendarContainerResource>(resultType, calendarContainerResource,
        continuationData);
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

  private static CalendarAttendeeModel transformToModelAttendee(EventAttendee attendee) {
    return new CalendarAttendeeModel(attendee.getDisplayName(), attendee.getEmail(),
        Boolean.TRUE.equals(attendee.getOptional()));
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

  private static CalendarModel convertToCalendarModel(CalendarListEntry calendarData) {
    return new CalendarModel(
        calendarData.getId(),
        calendarData.getSummary(),
        calendarData.getDescription());
  }

  private static CalendarEventModel convertToCalendarEventModel(String id, Event eventData) {
    List<EventAttendee> attendees = eventData.getAttendees();
    return new CalendarEventModel(
        id,
        eventData.getDescription(),
        eventData.getSummary(),
        attendees == null ? null : attendees.stream()
            .map(GoogleCalendarExporter::transformToModelAttendee)
            .collect(Collectors.toList()),
        eventData.getLocation(),
        getEventTime(eventData.getStart()),
        getEventTime(eventData.getEnd()));
  }
}
