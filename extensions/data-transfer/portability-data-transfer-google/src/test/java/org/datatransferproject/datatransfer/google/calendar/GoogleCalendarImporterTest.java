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

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.test.types.FakeIdempotentImportExecutor;
import org.datatransferproject.types.common.models.calendar.CalendarContainerResource;
import org.datatransferproject.types.common.models.calendar.CalendarEventModel;
import org.datatransferproject.types.common.models.calendar.CalendarModel;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class GoogleCalendarImporterTest {
  private GoogleCalendarImporter calendarService;
  private GoogleCredentialFactory credentialFactory;

  private Calendar calendarClient;
  private Calendar.Calendars calendarCalendars;
  private Calendar.Calendars.Insert calendarInsertRequest;
  private Calendar.Events calendarEvents;
  private Calendar.Events.Insert eventInsertRequest;
  private IdempotentImportExecutor executor;

  @Before
  public void setup() {
    calendarClient = mock(Calendar.class);
    calendarCalendars = mock(Calendar.Calendars.class);
    calendarInsertRequest = mock(Calendar.Calendars.Insert.class);
    calendarEvents = mock(Calendar.Events.class);
    eventInsertRequest = mock(Calendar.Events.Insert.class);
    credentialFactory = mock(GoogleCredentialFactory.class);

    executor = new FakeIdempotentImportExecutor();

    calendarService = new GoogleCalendarImporter(credentialFactory, calendarClient);

    when(calendarClient.calendars()).thenReturn(calendarCalendars);
    when(calendarClient.events()).thenReturn(calendarEvents);

    verifyNoInteractions(credentialFactory);
  }

  @Test
  public void importCalendarAndEvent() throws Exception {
    String modelCalendarId = "modelCalendarId";
    String googleCalendarId = "googleCalendarId";
    UUID jobId = UUID.randomUUID();

    // Set up calendar, events, and mocks
    CalendarModel calendarModel = new CalendarModel(modelCalendarId, null, null);
    com.google.api.services.calendar.model.Calendar calendarToInsert =
        GoogleCalendarImporter.convertToGoogleCalendar(calendarModel);
    com.google.api.services.calendar.model.Calendar responseCalendar =
        new com.google.api.services.calendar.model.Calendar().setId(googleCalendarId);

    CalendarEventModel eventModel =
        new CalendarEventModel(modelCalendarId, null, null, null, null, null, null, null);
    Event eventToInsert = GoogleCalendarImporter.convertToGoogleCalendarEvent(eventModel);
    Event responseEvent = new Event();

    when(eventInsertRequest.execute()).thenReturn(responseEvent);
    when(calendarEvents.insert(googleCalendarId, eventToInsert)).thenReturn(eventInsertRequest);
    when(calendarInsertRequest.execute()).thenReturn(responseCalendar);
    when(calendarCalendars.insert(calendarToInsert)).thenReturn(calendarInsertRequest);

    CalendarContainerResource calendarContainerResource =
        new CalendarContainerResource(
            Collections.singleton(calendarModel), Collections.singleton(eventModel));

    // Run test
    calendarService.importItem(jobId, executor, null, calendarContainerResource);

    // Check the right methods were called
    verify(calendarCalendars).insert(calendarToInsert);
    verify(calendarInsertRequest).execute();
    verify(calendarEvents).insert(googleCalendarId, eventToInsert);
    verify(eventInsertRequest).execute();
  }
}
