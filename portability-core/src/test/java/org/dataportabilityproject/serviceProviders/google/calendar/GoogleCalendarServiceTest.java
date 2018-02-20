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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.Calendar.CalendarList;
import java.io.IOException;
import java.util.Optional;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.dataModels.ExportInformation;
import org.junit.Before;
import org.junit.Test;

public class GoogleCalendarServiceTest {
  private static final String NEXT_TOKEN = "next_token";

  private Calendar calendarClient;
  private JobDataCache jobDataCache;
  private CalendarList calendarList;
  private CalendarList.List listRequest;

  @Before
  public void setup() throws IOException {
    listRequest = mock(CalendarList.List.class);
    calendarList = mock(CalendarList.class);

    calendarClient = mock(Calendar.class);
    jobDataCache = mock(JobDataCache.class);

    when(calendarClient.calendarList()).thenReturn(calendarList);
    when(calendarList.list()).thenReturn(listRequest);
  }

  @Test
  public void testExportFirstSet() {
    // Looking at first page, with at least one page after it
    ExportInformation emptyExportInformation = new ExportInformation(Optional.empty(),
        Optional.empty());
    listRequest.setPageToken(NEXT_TOKEN);
  }
}
