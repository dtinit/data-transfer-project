package org.datatransferproject.datatransfer.google.calendar;

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
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.common.GoogleStaticObjects;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.PaginationData;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.IdOnlyContainerResource;
import org.datatransferproject.types.common.models.calendar.CalendarAttendeeModel;
import org.datatransferproject.types.common.models.calendar.CalendarContainerResource;
import org.datatransferproject.types.common.models.calendar.CalendarEventModel;
import org.datatransferproject.types.common.models.calendar.CalendarModel;
import org.datatransferproject.types.common.models.calendar.RecurrenceRule;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.datatransferproject.datatransfer.google.common.GoogleStaticObjects.CALENDAR_TOKEN_PREFIX;
import static org.datatransferproject.datatransfer.google.common.GoogleStaticObjects.EVENT_TOKEN_PREFIX;


public class GoogleCalendarExporter implements
    Exporter<TokensAndUrlAuthData, CalendarContainerResource> {

  private final GoogleCredentialFactory credentialFactory;
  private volatile Calendar calendarInterface;

  public GoogleCalendarExporter(GoogleCredentialFactory credentialFactory) {
    this(credentialFactory, null); // Lazily initialized later on
  }

  @VisibleForTesting
  GoogleCalendarExporter(GoogleCredentialFactory credentialFactory, Calendar calendarInterface) {
    this.credentialFactory = credentialFactory;
    this.calendarInterface = calendarInterface;
  }

  private static CalendarAttendeeModel transformToModelAttendee(EventAttendee attendee) {
    return new CalendarAttendeeModel(
        attendee.getDisplayName(),
        attendee.getEmail(),
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
      offsetDateTime =
          OffsetDateTime.from(
              LocalDate.parse(dateTime.getDate().toString()).atStartOfDay(ZoneId.of("UTC")));
    }

    return new CalendarEventModel.CalendarEventTime(offsetDateTime, dateTime.getDate() != null);
  }

  private static RecurrenceRule getRecurrenceRule(List<String> ruleStrings) {
    RecurrenceRule.Builder ruleBuilder = new RecurrenceRule.Builder();
    for (String st : ruleStrings) {
      Preconditions.checkArgument(st.contains(":"),
          "Recurrence entry " + st + " cannot be parsed");
      String[] split = st.split("[:;]", 2);
      String type = split[0];
      String value = split[1];
      switch (type) {
        case RecurrenceRule.RRULE:
          ruleBuilder.setRRule(RecurrenceRule.parseRRuleString(value));
          break;
        case RecurrenceRule.RDATE:
          ruleBuilder.setRDate(RecurrenceRule.parseRDateString(value));
          break;
        case RecurrenceRule.EXDATE:
          ruleBuilder.setExDate(RecurrenceRule.parseExDateString(value));
          break;
        default:
          throw new IllegalArgumentException(
              "Recurrence entry " + st + " is not recognizable as an RRULE, RDATE, or EXDATE");
      }
    }
    return ruleBuilder.build();
  }

  private static CalendarModel convertToCalendarModel(CalendarListEntry calendarData) {
    return new CalendarModel(
        calendarData.getId(), calendarData.getSummary(), calendarData.getDescription());
  }

  private static CalendarEventModel convertToCalendarEventModel(String id, Event eventData) {
    List<EventAttendee> attendees = eventData.getAttendees();
    List<String> recurrenceRulesStrings = eventData.getRecurrence();
    return new CalendarEventModel(
        id,
        eventData.getDescription(),
        eventData.getSummary(),
        attendees == null
            ? null
            : attendees
                .stream()
                .map(GoogleCalendarExporter::transformToModelAttendee)
                .collect(Collectors.toList()),
        eventData.getLocation(),
        getEventTime(eventData.getStart()),
        getEventTime(eventData.getEnd()),
        recurrenceRulesStrings == null ? null : getRecurrenceRule(recurrenceRulesStrings));
  }

  @Override
  public ExportResult<CalendarContainerResource> export(
      UUID jobId, TokensAndUrlAuthData authData, Optional<ExportInformation> exportInformation) {
    if (!exportInformation.isPresent()) {
      return exportCalendars(authData, Optional.empty());
    } else {
      StringPaginationToken paginationToken =
          (StringPaginationToken) exportInformation.get().getPaginationData();
      if (paginationToken != null && paginationToken.getToken().startsWith(CALENDAR_TOKEN_PREFIX)) {
        // Next thing to export is more calendars
        return exportCalendars(authData, Optional.of(paginationToken));
      } else {
        // Next thing to export is events
        IdOnlyContainerResource idOnlyContainerResource =
            (IdOnlyContainerResource) exportInformation.get().getContainerResource();
        Optional<PaginationData> pageData = Optional.ofNullable(paginationToken);
        return getCalendarEvents(authData,
            idOnlyContainerResource.getId(),
            pageData);
      }
    }
  }

  private ExportResult<CalendarContainerResource> exportCalendars(
      TokensAndUrlAuthData authData, Optional<PaginationData> pageData) {
    Calendar.CalendarList.List listRequest;
    CalendarList listResult;

    // Get calendar information
    try {
      listRequest = getOrCreateCalendarInterface(authData).calendarList().list();

      if (pageData.isPresent()) {
        StringPaginationToken paginationToken = (StringPaginationToken) pageData.get();
        Preconditions.checkState(
            paginationToken.getToken().startsWith(CALENDAR_TOKEN_PREFIX),
            "Token is not applicable");
        listRequest.setPageToken(
            ((StringPaginationToken) pageData.get())
                .getToken()
                .substring(CALENDAR_TOKEN_PREFIX.length()));
      }

      listResult = listRequest.execute();
    } catch (IOException e) {
      return new ExportResult<>(e);
    }

    // Set up continuation data
    PaginationData nextPageData = null;
    if (listResult.getNextPageToken() != null) {
      nextPageData =
          new StringPaginationToken(CALENDAR_TOKEN_PREFIX + listResult.getNextPageToken());
    }
    ContinuationData continuationData = new ContinuationData(nextPageData);

    // Process calendar list
    List<CalendarModel> calendarModels = new ArrayList<>(listResult.getItems().size());
    for (CalendarListEntry calendarData : listResult.getItems()) {
      CalendarModel model = convertToCalendarModel(calendarData);
      continuationData.addContainerResource(new IdOnlyContainerResource(calendarData.getId()));
      calendarModels.add(model);
    }
    CalendarContainerResource calendarContainerResource =
        new CalendarContainerResource(calendarModels, null);

    // Get result type
    ExportResult.ResultType resultType = ResultType.CONTINUE;
    if (calendarModels.isEmpty()) {
      resultType = ResultType.END;
    }

    return new ExportResult<>(resultType, calendarContainerResource, continuationData);
  }

  private ExportResult<CalendarContainerResource> getCalendarEvents(
      TokensAndUrlAuthData authData, String id, Optional<PaginationData> pageData) {
    Calendar.Events.List listRequest;
    Events listResult;

    // Get event information
    try {
      listRequest =
          getOrCreateCalendarInterface(authData)
              .events()
              .list(id)
              .setMaxAttendees(GoogleStaticObjects.MAX_ATTENDEES);
      if (pageData.isPresent()) {
        StringPaginationToken paginationToken = (StringPaginationToken) pageData.get();
        Preconditions.checkState(
            paginationToken.getToken().startsWith(EVENT_TOKEN_PREFIX), "Token is not applicable");
        listRequest.setPageToken(
            ((StringPaginationToken) pageData.get())
                .getToken()
                .substring(EVENT_TOKEN_PREFIX.length()));
      }
      listResult = listRequest.execute();
    } catch (IOException e) {
      return new ExportResult<>(e);
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
    CalendarContainerResource calendarContainerResource =
        new CalendarContainerResource(null, eventModels);

    // Get result type
    ExportResult.ResultType resultType = ResultType.CONTINUE;
    if (nextPageData == null) {
      resultType = ResultType.END;
    }

    return new ExportResult<>(resultType, calendarContainerResource, continuationData);
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
