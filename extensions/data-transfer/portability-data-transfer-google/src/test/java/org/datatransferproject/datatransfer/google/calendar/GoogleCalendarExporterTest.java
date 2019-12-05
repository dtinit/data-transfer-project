/*
 * Copyright 2018 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.google.calendar;

import static com.google.common.truth.Truth.assertThat;
import static org.datatransferproject.datatransfer.google.common.GoogleStaticObjects.CALENDAR_TOKEN_PREFIX;
import static org.datatransferproject.datatransfer.google.common.GoogleStaticObjects.EVENT_TOKEN_PREFIX;
import static org.datatransferproject.datatransfer.google.common.GoogleStaticObjects.MAX_ATTENDEES;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.models.IdOnlyContainerResource;
import org.datatransferproject.types.common.PaginationData;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.ContainerResource;
import org.datatransferproject.types.common.models.calendar.CalendarContainerResource;
import org.datatransferproject.types.common.models.calendar.CalendarEventModel;
import org.datatransferproject.types.common.models.calendar.CalendarModel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class GoogleCalendarExporterTest {
  private static final UUID JOB_ID = UUID.fromString("9b969983-a09b-4cb0-8017-7daae758126b");

  private static final String CALENDAR_ID = "calendar_id";
  private static final CalendarListEntry CALENDAR_LIST_ENTRY =
      new CalendarListEntry().setId(CALENDAR_ID);
  private static final String EVENT_DESCRIPTION = "event_description";
  private static final Event EVENT = new Event().setDescription(EVENT_DESCRIPTION);

  private static final String NEXT_TOKEN = "next_token";

  private GoogleCredentialFactory credentialFactory;
  private GoogleCalendarExporter googleCalendarExporter;

  private Calendar calendarClient;
  private Calendar.Calendars calendarCalendars;
  private Calendar.CalendarList calendarCalendarList;
  private Calendar.CalendarList.List calendarListRequest;
  private CalendarList calendarListResponse;
  private Calendar.Events calendarEvents;
  private Calendar.Events.List eventListRequest;
  private Events eventListResponse;

  @Before
  public void setup() throws IOException {
    calendarClient = mock(Calendar.class);
    calendarCalendars = mock(Calendar.Calendars.class);
    calendarCalendarList = mock(Calendar.CalendarList.class);
    calendarListRequest = mock(Calendar.CalendarList.List.class);
    calendarEvents = mock(Calendar.Events.class);
    eventListRequest = mock(Calendar.Events.List.class);
    credentialFactory = mock(GoogleCredentialFactory.class);

    googleCalendarExporter = new GoogleCalendarExporter(credentialFactory, calendarClient);

    when(calendarClient.calendars()).thenReturn(calendarCalendars);

    when(calendarClient.calendarList()).thenReturn(calendarCalendarList);
    when(calendarCalendarList.list()).thenReturn(calendarListRequest);
    when(calendarClient.events()).thenReturn(calendarEvents);

    when(calendarEvents.list(CALENDAR_ID)).thenReturn(eventListRequest);
    when(eventListRequest.setMaxAttendees(MAX_ATTENDEES)).thenReturn(eventListRequest);

    verifyNoInteractions(credentialFactory);
  }

  @Test
  public void exportCalendarFirstSet() throws IOException {
    setUpSingleCalendarResponse();

    // Looking at first page, with at least one page after it
    calendarListResponse.setNextPageToken(NEXT_TOKEN);

    // Run test
    ExportResult<CalendarContainerResource> result = googleCalendarExporter.export(JOB_ID, null,
        Optional.empty());

    // Check results
    // Verify correct methods were called
    verify(calendarClient).calendarList();
    verify(calendarCalendarList).list();
    verify(calendarListRequest).execute();

    // Check pagination token
    ContinuationData continuationData = (ContinuationData) result.getContinuationData();
    StringPaginationToken paginationToken =
        (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationToken.getToken()).isEqualTo(CALENDAR_TOKEN_PREFIX + NEXT_TOKEN);

    // Check calendars
    Collection<CalendarModel> actualCalendars = result.getExportedData().getCalendars();
    assertThat(actualCalendars.stream().map(CalendarModel::getId).collect(Collectors.toList()))
        .containsExactly(CALENDAR_ID);

    // Check events (should be empty, even though there is an event in the calendar)
    Collection<CalendarEventModel> actualEvents = result.getExportedData().getEvents();
    assertThat(actualEvents).isEmpty();
    // Should be one container in the resource list
    List<ContainerResource> actualResources = continuationData.getContainerResources();
    assertThat(
            actualResources
                .stream()
                .map(a -> ((IdOnlyContainerResource) a).getId())
                .collect(Collectors.toList()))
        .containsExactly(CALENDAR_ID);
  }

  @Test
  public void exportCalendarSubsequentSet() throws IOException {
    setUpSingleCalendarResponse();

    // Looking at subsequent page, with no page after it
    PaginationData paginationData = new StringPaginationToken(CALENDAR_TOKEN_PREFIX + NEXT_TOKEN);
    ExportInformation exportInformation = new ExportInformation(paginationData, null);
    calendarListResponse.setNextPageToken(null);

    // Run test
    ExportResult<CalendarContainerResource> result =
        googleCalendarExporter.export(UUID.randomUUID(), null, Optional.of(exportInformation));

    // Check results
    // Verify correct calls were made
    InOrder inOrder = Mockito.inOrder(calendarListRequest);
    inOrder.verify(calendarListRequest).setPageToken(NEXT_TOKEN);
    inOrder.verify(calendarListRequest).execute();

    // Check pagination token
    ContinuationData continuationData = (ContinuationData) result.getContinuationData();
    StringPaginationToken paginationToken =
        (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationToken).isNull();
  }

  @Test
  public void exportEventFirstSet() throws IOException {
    setUpSingleEventResponse();

    // Looking at first page, with at least one page after it
    ContainerResource containerResource = new IdOnlyContainerResource(CALENDAR_ID);
    ExportInformation exportInformation = new ExportInformation(null, containerResource);
    eventListResponse.setNextPageToken(NEXT_TOKEN);

    // Run test
    ExportResult<CalendarContainerResource> result =
        googleCalendarExporter.export(UUID.randomUUID(), null, Optional.of(exportInformation));

    // Check results
    // Verify correct methods were called
    verify(calendarEvents).list(CALENDAR_ID);
    verify(eventListRequest).setMaxAttendees(MAX_ATTENDEES);
    verify(eventListRequest).execute();

    // Check events
    Collection<CalendarEventModel> actualEvents = result.getExportedData().getEvents();
    assertThat(
            actualEvents
                .stream()
                .map(CalendarEventModel::getCalendarId)
                .collect(Collectors.toList()))
        .containsExactly(CALENDAR_ID);
    assertThat(actualEvents.stream().map(CalendarEventModel::getTitle).collect(Collectors.toList()))
        .containsExactly(EVENT_DESCRIPTION);

    // Check pagination token
    ContinuationData continuationData = (ContinuationData) result.getContinuationData();
    StringPaginationToken paginationToken =
        (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationToken.getToken()).isEqualTo(EVENT_TOKEN_PREFIX + NEXT_TOKEN);
  }

  @Test
  public void exportEventSubsequentSet() throws IOException {
    setUpSingleEventResponse();

    // Looking at subsequent page, with no pages after it
    ContainerResource containerResource = new IdOnlyContainerResource(CALENDAR_ID);
    PaginationData paginationData = new StringPaginationToken(EVENT_TOKEN_PREFIX + NEXT_TOKEN);
    ExportInformation exportInformation = new ExportInformation(paginationData, containerResource);
    eventListResponse.setNextPageToken(null);

    // Run test
    ExportResult<CalendarContainerResource> result =
        googleCalendarExporter.export(UUID.randomUUID(), null, Optional.of(exportInformation));

    // Check results
    // Verify correct methods were called in order
    InOrder inOrder = Mockito.inOrder(eventListRequest);
    inOrder.verify(eventListRequest).setPageToken(NEXT_TOKEN);
    inOrder.verify(eventListRequest).execute();

    // Check pagination token
    ContinuationData continuationData = (ContinuationData) result.getContinuationData();
    StringPaginationToken paginationToken =
        (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationToken).isNull();
  }

  /** Sets up a response with a single calendar, containing a single event */
  private void setUpSingleCalendarResponse() throws IOException {
    setUpSingleEventResponse();
    calendarListResponse =
        new CalendarList().setItems(Collections.singletonList(CALENDAR_LIST_ENTRY));

    when(calendarListRequest.execute()).thenReturn(calendarListResponse);
  }

  /** Sets up a response for a single event */
  private void setUpSingleEventResponse() throws IOException {
    eventListResponse = new Events().setItems(Collections.singletonList(EVENT));
    when(eventListRequest.execute()).thenReturn(eventListResponse);
  }
}
