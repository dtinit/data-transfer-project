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
package org.datatransferproject.transfer.microsoft.transformer.calendar;

import static org.datatransferproject.transfer.microsoft.transformer.TransformConstants.CALENDAR_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import org.datatransferproject.transfer.microsoft.helper.TestTransformerContext;
import org.datatransferproject.types.common.models.calendar.CalendarAttendeeModel;
import org.datatransferproject.types.common.models.calendar.CalendarEventModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ToCalendarEventModelTransformerTest {
  private static final String SAMPLE_CALENDAR_EVENT =
      "        {\n"
          + "            \"@odata.etag\": \"W/\\\"J9QFpZ0REkSkaxRkN3kHHwAB54nFgA==\\\"\",\n"
          + "            \"id\": \"567=\",\n"
          + "            \"createdDateTime\": \"2018-02-07T09:11:50.9516391Z\",\n"
          + "            \"lastModifiedDateTime\": \"2018-02-07T09:11:50.982889Z\",\n"
          + "            \"changeKey\": \"key==\",\n"
          + "            \"categories\": [],\n"
          + "            \"originalStartTimeZone\": \"Pacific Standard Time\",\n"
          + "            \"originalEndTimeZone\": \"Pacific Standard Time\",\n"
          + "            \"iCalUId\": \"id123\",\n"
          + "            \"reminderMinutesBeforeStart\": 15,\n"
          + "            \"isReminderOn\": true,\n"
          + "            \"hasAttachments\": false,\n"
          + "            \"subject\": \"Test Appointment 1\",\n"
          + "            \"bodyPreview\": \"\",\n"
          + "            \"importance\": \"normal\",\n"
          + "            \"sensitivity\": \"normal\",\n"
          + "            \"isAllDay\": false,\n"
          + "            \"isCancelled\": false,\n"
          + "            \"isOrganizer\": true,\n"
          + "            \"responseRequested\": true,\n"
          + "            \"seriesMasterId\": null,\n"
          + "            \"showAs\": \"busy\",\n"
          + "            \"type\": \"singleInstance\",\n"
          + "            \"webLink\": \"https://outlook.live.com/owa/?itemid=link%3D&exvsurl=1&path=/calendar/item\",\n"
          + "            \"onlineMeetingUrl\": null,\n"
          + "            \"responseStatus\": {\n"
          + "                \"response\": \"organizer\",\n"
          + "                \"time\": \"0001-01-01T00:00:00Z\"\n"
          + "            },\n"
          + "            \"body\": {\n"
          + "                \"contentType\": \"html\",\n"
          + "                \"content\": \"<html>\\r\\n<head>\\r\\n<meta http-equiv=\\\"Content-Type\\\" content=\\\"text/html; charset=utf-8\\\">\\r\\n<meta content=\\\"text/html; charset=us-ascii\\\">\\r\\n<style type=\\\"text/css\\\" style=\\\"display:none\\\">\\r\\n<!--\\r\\np\\r\\n\\t{margin-top:0;\\r\\n\\tmargin-bottom:0}\\r\\n-->\\r\\n</style>\\r\\n</head>\\r\\n<body dir=\\\"ltr\\\">\\r\\n<div id=\\\"divtagdefaultwrapper\\\" dir=\\\"ltr\\\" style=\\\"font-size:12pt; color:#000000; font-family:Calibri,Helvetica,sans-serif\\\">\\r\\n<p style=\\\"margin-top:0; margin-bottom:0\\\"><br>\\r\\n</p>\\r\\n</div>\\r\\n</body>\\r\\n</html>\\r\\n\"\n"
          + "            },\n"
          + "            \"start\": {\n"
          + "                \"dateTime\": \"2018-02-14T18:00:00.0000000\",\n"
          + "                \"timeZone\": \"UTC\"\n"
          + "            },\n"
          + "            \"end\": {\n"
          + "                \"dateTime\": \"2018-02-14T18:30:00.0000000\",\n"
          + "                \"timeZone\": \"UTC\"\n"
          + "            },\n"
          + "            \"location\": {\n"
          + "                \"displayName\": \"Some Place\",\n"
          + "                \"address\": {}\n"
          + "            },\n"
          + "            \"recurrence\": null,\n"
          + "            \"attendees\": [\n"
          + "                {\n"
          + "                    \"type\": \"required\",\n"
          + "                    \"status\": {\n"
          + "                        \"response\": \"none\",\n"
          + "                        \"time\": \"0001-01-01T00:00:00Z\"\n"
          + "                    },\n"
          + "                    \"emailAddress\": {\n"
          + "                        \"name\": \"Test Test1\",\n"
          + "                        \"address\": \"foo@foo.com\"\n"
          + "                    }\n"
          + "                }\n"
          + "            ],\n"
          + "            \"organizer\": {\n"
          + "                \"emailAddress\": {\n"
          + "                    \"name\": \"Foo\",\n"
          + "                    \"address\": \"outlook_123@outlook.com\"\n"
          + "                }\n"
          + "            }\n"
          + "        }\n";
  private ToCalendarEventModelTransformer transformer;
  private ObjectMapper mapper;
  private TestTransformerContext context;

  @SuppressWarnings("unchecked")
  @Test
  public void testTransform() throws IOException {
    context.setProperty(CALENDAR_ID, "123");
    Map<String, Object> rawEvent = mapper.readValue(SAMPLE_CALENDAR_EVENT, Map.class);

    CalendarEventModel event = transformer.apply(rawEvent, context);

    assertEquals("123", event.getCalendarId());

    assertEquals("Some Place", event.getLocation());
    assertEquals("Test Appointment 1", event.getTitle());
    assertTrue(event.getNotes().length() > 5);
    assertEquals(1, event.getAttendees().size());

    CalendarAttendeeModel attendee = event.getAttendees().get(0);
    assertEquals("Test Test1", attendee.getDisplayName());
    assertEquals("foo@foo.com", attendee.getEmail());
    assertFalse(attendee.getOptional());

    assertEquals(18, event.getStartTime().getDateTime().getHour());
    assertEquals(0, event.getStartTime().getDateTime().getMinute());

    assertEquals(18, event.getEndTime().getDateTime().getHour());
    assertEquals(30, event.getEndTime().getDateTime().getMinute());
  }

  @BeforeEach
  public void setUp() {
    transformer = new ToCalendarEventModelTransformer();
    mapper = new ObjectMapper();
    context = new TestTransformerContext();
  }
}
