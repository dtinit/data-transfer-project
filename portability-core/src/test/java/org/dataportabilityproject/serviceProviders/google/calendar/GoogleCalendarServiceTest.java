/*
 * Copyright 2018 The Data-Portability Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.serviceProviders.google.calendar;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.dataModels.ExportInformation;
import org.dataportabilityproject.dataModels.PaginationInformation;
import org.dataportabilityproject.dataModels.Resource;
import org.dataportabilityproject.dataModels.calendar.CalendarEventModel;
import org.dataportabilityproject.dataModels.calendar.CalendarModel;
import org.dataportabilityproject.dataModels.calendar.CalendarModelWrapper;
import org.dataportabilityproject.serviceProviders.google.GoogleStaticObjects;
import org.dataportabilityproject.shared.IdOnlyResource;
import org.dataportabilityproject.shared.StringPaginationToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class GoogleCalendarServiceTest {

  private static final String CALENDAR_ID = "calendar_id";
  private static final CalendarListEntry CALENDAR_LIST_ENTRY = new CalendarListEntry().setId
      (CALENDAR_ID);
  private static final String EVENT_DESCRIPTION = "event_description";
  private static final Event EVENT = new Event().setDescription(EVENT_DESCRIPTION);

  private static final String NEXT_TOKEN = "next_token";

  private GoogleCalendarService calendarService;

  private Calendar calendarClient;
  private JobDataCache jobDataCache;
  private Calendar.CalendarList calendarCalendarList;
  private Calendar.CalendarList.List calendarListRequest;
  private CalendarList calendarListResponse;
  private Calendar.Events calendarEvents;
  private Calendar.Events.List eventListRequest;
  private Events eventListResponse;

  @Before
  public void setup() throws IOException {
    calendarCalendarList = mock(Calendar.CalendarList.class);
    calendarListRequest = mock(Calendar.CalendarList.List.class);
    calendarEvents = mock(Calendar.Events.class);
    eventListRequest = mock(Calendar.Events.List.class);

    calendarClient = mock(Calendar.class);
    jobDataCache = mock(JobDataCache.class);

    calendarService = new GoogleCalendarService(calendarClient, jobDataCache);

    when(calendarClient.calendarList()).thenReturn(calendarCalendarList);
    when(calendarCalendarList.list()).thenReturn(calendarListRequest);
    when(calendarClient.events()).thenReturn(calendarEvents);
    when(calendarEvents.list(CALENDAR_ID)).thenReturn(eventListRequest);
    when(eventListRequest.setMaxAttendees(GoogleStaticObjects.MAX_ATTENDEES))
        .thenReturn(eventListRequest);
  }

  /**
   * Sets up a response with a single calendar, containing a single event
   */
  private void setUpSingleCalendarResponse() throws IOException {
    setUpSingleEventResponse();
    calendarListResponse = new CalendarList()
        .setItems(Collections.singletonList(CALENDAR_LIST_ENTRY));

    when(calendarListRequest.execute()).thenReturn(calendarListResponse);
  }

  private void setUpSingleEventResponse() throws IOException {
    eventListResponse = new Events().setItems(Collections.singletonList(EVENT));
    when(eventListRequest.execute()).thenReturn(eventListResponse);
  }

  @Test
  public void testExportCalendarFirstSet() throws IOException {
    setUpSingleCalendarResponse();

    // Looking at first page, with at least one page after it
    ExportInformation emptyExportInformation = new ExportInformation(Optional.empty(),
        Optional.empty());
    calendarListResponse.setNextPageToken(NEXT_TOKEN);

    // Run test
    CalendarModelWrapper wrapper = calendarService.export(emptyExportInformation);

    // Check results
    // Verify correct methods were called
    verify(calendarClient).calendarList();
    verify(calendarCalendarList).list();
    verify(calendarListRequest).execute();

    // Check pagination token
    StringPaginationToken paginationToken = (StringPaginationToken) wrapper
        .getContinuationInformation().getPaginationInformation();
    assertThat(paginationToken.getId()).isEqualTo(NEXT_TOKEN);

    // Check calendars
    Collection<CalendarModel> calendars = wrapper.getCalendars();
    assertThat(calendars.stream().map(CalendarModel::getId).collect(Collectors.toList()))
        .containsExactly(CALENDAR_ID);

    // Check events (should be empty, even though there is an event in the calendar)
    Collection<CalendarEventModel> events = wrapper.getEvents();
    assertThat(events).isEmpty();
    // Should be one event in the resource list
    Collection<? extends Resource> subResources = wrapper.getContinuationInformation()
        .getSubResources();
    assertThat(subResources.stream().map(a -> ((IdOnlyResource) a).getId()).collect(Collectors
        .toList())).containsExactly(CALENDAR_ID);
  }

  @Test
  public void testExportCalendarSubsequentSet() throws IOException {
    setUpSingleCalendarResponse();

    // Looking at subsequent page, with no page after it
    ExportInformation nextPageExportInformation = new ExportInformation(Optional.empty(),
        Optional.of(new StringPaginationToken(NEXT_TOKEN)));
    calendarListResponse.setNextPageToken(null);

    // Run test
    CalendarModelWrapper wrapper = calendarService.export(nextPageExportInformation);

    // Check results
    // Verify correct calls were made
    InOrder inOrder = Mockito.inOrder(calendarListRequest);
    inOrder.verify(calendarListRequest).setPageToken(NEXT_TOKEN);
    inOrder.verify(calendarListRequest).execute();

    // Check pagination token
    StringPaginationToken paginationToken = (StringPaginationToken) wrapper
        .getContinuationInformation().getPaginationInformation();
    assertThat(paginationToken).isNull();
  }

  @Test
  public void testExportEventFirstSet() throws IOException {
    setUpSingleEventResponse();

    // Looking at first page, with at least one page after it
    Resource resource = new IdOnlyResource(CALENDAR_ID);
    ExportInformation resourceExportInformation = new ExportInformation(Optional.of(resource),
        Optional.empty());
    eventListResponse.setNextPageToken(NEXT_TOKEN);

    // Run test
    CalendarModelWrapper wrapper = calendarService.export(resourceExportInformation);

    // Check results
    // Verify correct methods were called
    verify(calendarEvents).list(CALENDAR_ID);
    verify(eventListRequest).setMaxAttendees(GoogleStaticObjects.MAX_ATTENDEES);
    verify(eventListRequest).execute();

    // Check events
    Collection<CalendarEventModel> events = wrapper.getEvents();
    assertThat(events.stream().map(CalendarEventModel::getCalendarId).collect(Collectors.toList()))
        .containsExactly(CALENDAR_ID);
    assertThat(events.stream().map(CalendarEventModel::getTitle).collect(Collectors.toList()))
        .containsExactly(EVENT_DESCRIPTION);

    // Check pagination token
    StringPaginationToken paginationToken = (StringPaginationToken) wrapper
        .getContinuationInformation().getPaginationInformation();
    assertThat(paginationToken.getId()).isEqualTo(NEXT_TOKEN);
  }

  @Test
  public void testExportEventSubsequentSet() throws IOException {
    setUpSingleEventResponse();;

    // Looking at subsequent page, with no pages after it
    Resource resource = new IdOnlyResource(CALENDAR_ID);
    PaginationInformation paginationInformation = new StringPaginationToken(NEXT_TOKEN);
    ExportInformation resourceExportInformation = new ExportInformation(Optional.of(resource),
        Optional.of(paginationInformation));
    eventListResponse.setNextPageToken(null);

    // Run test
    CalendarModelWrapper wrapper = calendarService.export(resourceExportInformation);

    // Check results
    // Verify correct methods were called in order
    InOrder inOrder = Mockito.inOrder(eventListRequest);
    inOrder.verify(eventListRequest).setPageToken(NEXT_TOKEN);
    inOrder.verify(eventListRequest).execute();

    // Check pagination token
    StringPaginationToken paginationToken = (StringPaginationToken) wrapper
        .getContinuationInformation().getPaginationInformation();
    assertThat(paginationToken).isNull();
  }
}
