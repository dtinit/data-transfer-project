package org.dataportabilityproject.datatransfer.google.calendar;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.dataportabilityproject.datatransfer.google.common.GoogleStaticObjects;
import org.dataportabilityproject.spi.transfer.provider.ExportResult;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.store.TransferStore;
import org.dataportabilityproject.spi.transfer.types.*;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.models.ContainerResource;
import org.dataportabilityproject.types.transfer.models.calendar.CalendarAttendeeModel;
import org.dataportabilityproject.types.transfer.models.calendar.CalendarContainerResource;
import org.dataportabilityproject.types.transfer.models.calendar.CalendarEventModel;
import org.dataportabilityproject.types.transfer.models.calendar.CalendarModel;

public class GoogleCalendarExporter implements Exporter<AuthData, CalendarContainerResource> {

  private Calendar calendarInterface;
  private TransferStore transferStore;

  // For testing purposes
  GoogleCalendarExporter(Calendar calendarInterface, TransferStore transferStore) {
    this.calendarInterface = calendarInterface;
    this.transferStore = transferStore;
  }

  @Override
  public ExportResult<CalendarContainerResource> export(AuthData authData) {
    setUpCalendarInterface(authData);

    return null;
  }

  @Override
  public ExportResult<CalendarContainerResource> export(AuthData authData, ExportInformation exportInformation) {
    setUpCalendarInterface(authData);
    
    return null;
  }

  private ExportResult<CalendarContainerResource> exportCalendars(Optional<PaginationData> pageData) {
    Calendar.CalendarList.List listRequest;
    CalendarList listResult;

    // Get calendar information
    try {
      listRequest = calendarInterface.calendarList().list();

      if (pageData.isPresent()) {
        listRequest.setPageToken(((StringPaginationToken) pageData.get()).getToken());
      }

      listResult = listRequest.execute();
    } catch (IOException e) {
      return new ExportResult<CalendarContainerResource>(ExportResult.ResultType.ERROR, e.getMessage());
    }

    

    List<CalendarModel> calendarModels = new ArrayList<>(listResult.getItems().size());
    List<IdOnlyResource> resources = new ArrayList<>(listResult.getItems().size());
    for (CalendarListEntry calendarData : listResult.getItems()) {
      CalendarModel model = convertToCalendarModel(calendarData);
      resources.add(new IdOnlyResource(calendarData.getId()));
      calendarModels.add(model);
    }

    // Set up continuation information
    ExportResult.ResultType resultType = ExportResult.ResultType.CONTINUE;
    PaginationData newPageData = null;
    if (listResult.getNextPageToken() != null) {
      newPageData = new StringPaginationToken(listResult.getNextPageToken());
    } else if (resources.isEmpty()){
      resultType = ExportResult.ResultType.END;
    }
    ContinuationData continuationData = new ContinuationData(newPageData);

    // Set up container resource
    CalendarContainerResource calendarContainerResource = new CalendarContainerResource
            (calendarModels, null);

  }

  void setUpCalendarInterface(AuthData authData) {
    // TODO(olsona): get credential using authData
    Credential credential = null;
    calendarInterface = new Calendar.Builder(
        GoogleStaticObjects.getHttpTransport(), GoogleStaticObjects.JSON_FACTORY, credential)
        .setApplicationName(GoogleStaticObjects.APP_NAME)
        .build();
  }

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
    return new CalendarModel(
        calendarData.getId(),
        calendarData.getSummary(),
        calendarData.getDescription());
  }

  static CalendarEventModel convertToCalendarEventModel(String id, Event eventData) {
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
