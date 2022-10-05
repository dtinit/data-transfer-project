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
package org.datatransferproject.transfer.microsoft.integration;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import okhttp3.OkHttpClient;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.test.types.FakeIdempotentImportExecutor;
import org.datatransferproject.transfer.microsoft.calendar.MicrosoftCalendarImporter;
import org.datatransferproject.transfer.microsoft.transformer.TransformerServiceImpl;
import org.datatransferproject.types.common.models.calendar.CalendarAttendeeModel;
import org.datatransferproject.types.common.models.calendar.CalendarContainerResource;
import org.datatransferproject.types.common.models.calendar.CalendarEventModel;
import org.datatransferproject.types.common.models.calendar.CalendarModel;
import org.datatransferproject.types.transfer.auth.TokenAuthData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies Calendar export using mock HTTP endpoints that replay responses from the Microsoft Graph
 * API.
 */
public class MicrosoftCalendarImportTest {
  private static final UUID JOB_ID = UUID.randomUUID();
  private static final String BATCH_CALENDAR_RESPONSE =
      "\n"
          + "{\n"
          + "    \"responses\": [\n"
          + "        {\n"
          + "            \"id\": \"1\",\n"
          + "            \"status\": 201,\n"
          + "            \"headers\": {\n"
          + "                \"Location\": \"https://outlook.office365.com/api/beta/Users('223333333')/Calendars('2233333=='),https://graph.microsoft.com\",\n"
          + "                \"OData-Version\": \"4.0\",\n"
          + "                \"Content-Type\": \"application/json;odata.metadata=minimal;odata.streaming=true;IEEE754Compatible=false;charset=utf-8\"\n"
          + "            },\n"
          + "            \"body\": {\n"
          + "                \"@odata.context\": \"https://graph.microsoft.com/beta/$metadata#users('foo.bar%40gmail.com')/calendars/$entity\",\n"
          + "                \"id\": \"NewId1\",\n"
          + "                \"name\": \"Cal1\",\n"
          + "                \"color\": \"auto\",\n"
          + "                \"hexColor\": \"\",\n"
          + "                \"isDefaultCalendar\": false,\n"
          + "                \"changeKey\": \"977==\",\n"
          + "                \"canShare\": false,\n"
          + "                \"canViewPrivateItems\": true,\n"
          + "                \"isShared\": false,\n"
          + "                \"isSharedWithMe\": false,\n"
          + "                \"canEdit\": true,\n"
          + "                \"owner\": {\n"
          + "                    \"name\": \"Foo Bar\",\n"
          + "                    \"address\": \"foo@outlook.com\"\n"
          + "                }\n"
          + "            }\n"
          + "        }\n"
          + "    ]\n"
          + "}";
  private static final String BATCH_EVENT_RESPONSE =
      "\n"
          + "{\n"
          + "    \"responses\": [\n"
          + "        {\n"
          + "            \"id\": \"1\",\n"
          + "            \"status\": 201,\n"
          + "            \"headers\": {\n"
          + "                \"Location\": \"https://outlook.office365.com/api/beta/Users('0003bffd-9f40-7cdf-0000-dddd@84df9e7f-e9f6-40af-b435-aaaaaaaaaaaa')/Events('dddddddd=='),https://graph.microsoft.com\",\n"
          + "                \"OData-Version\": \"4.0\",\n"
          + "                \"Content-Type\": \"application/json;odata.metadata=minimal;odata.streaming=true;IEEE754Compatible=false;charset=utf-8\",\n"
          + "                \"ETag\": \"W/\\\"J9QFpZ0RkN3kHHwAB6k8SEg==\\\"\"\n"
          + "            },\n"
          + "            \"body\": {\n"
          + "                \"@odata.context\": \"https://graph.microsoft.com/beta/$metadata#users('foo%40gmail.com')/calendars('AQMkADAwATNiZmYAZCA%3D')/events/$entity\",\n"
          + "                \"@odata.etag\": \"W/\\\"J9QFpZ0REkSkaxRkN3kHHwAB6k8SEg==\\\"\",\n"
          + "                \"id\": \"AQMkADAwATNiZmYAZC05ZjQwLTdjZGYtMDACLTAwCgBGAAAddddaQFpZ0REkSkaxRkN3kHHwAB6iRLTQAAAA==\",\n"
          + "                \"createdDateTime\": \"2018-02-13T16:58:20.7944246Z\",\n"
          + "                \"lastModifiedDateTime\": \"2018-02-13T16:58:21.3725491Z\",\n"
          + "                \"changeKey\": \"J9QFp8SEg==\",\n"
          + "                \"categories\": [],\n"
          + "                \"originalStartTimeZone\": \"UTC\",\n"
          + "                \"originalEndTimeZone\": \"UTC\",\n"
          + "                \"iCalUId\": \"040000008200010000000BDA99D58F3691948963ABCDFD0F9F1C5\",\n"
          + "                \"reminderMinutesBeforeStart\": 15,\n"
          + "                \"isReminderOn\": true,\n"
          + "                \"hasAttachments\": false,\n"
          + "                \"subject\": \"Event1\",\n"
          + "                \"bodyPreview\": \"Test Notes\",\n"
          + "                \"importance\": \"normal\",\n"
          + "                \"sensitivity\": \"normal\",\n"
          + "                \"isAllDay\": false,\n"
          + "                \"isCancelled\": false,\n"
          + "                \"isOrganizer\": true,\n"
          + "                \"responseRequested\": true,\n"
          + "                \"seriesMasterId\": null,\n"
          + "                \"showAs\": \"busy\",\n"
          + "                \"type\": \"singleInstance\",\n"
          + "                \"webLink\": \"https://outlook.live.com/owa/?itemid=ddddddds3334444%3D%3D&exvsurl=1&path=/calendar/item\",\n"
          + "                \"onlineMeetingUrl\": null,\n"
          + "                \"responseStatus\": {\n"
          + "                    \"response\": \"organizer\",\n"
          + "                    \"time\": \"0001-01-01T00:00:00Z\"\n"
          + "                },\n"
          + "                \"body\": {\n"
          + "                    \"contentType\": \"html\",\n"
          + "                    \"content\": \"<html>\\r\\n<head>\\r\\n<meta http-equiv=\\\"Content-Type\\\" content=\\\"text/html; charset=utf-8\\\">\\r\\n<meta content=\\\"text/html; charset=us-ascii\\\">\\r\\n</head>\\r\\n<body>\\r\\nTest Notes\\r\\n</body>\\r\\n</html>\\r\\n\"\n"
          + "                },\n"
          + "                \"start\": {\n"
          + "                    \"dateTime\": \"2018-02-13T16:56:38.8375050\",\n"
          + "                    \"timeZone\": \"UTC\"\n"
          + "                },\n"
          + "                \"end\": {\n"
          + "                    \"dateTime\": \"2018-02-13T16:56:38.8375050\",\n"
          + "                    \"timeZone\": \"UTC\"\n"
          + "                },\n"
          + "                \"location\": {\n"
          + "                    \"displayName\": \"Location1\",\n"
          + "                    \"locationType\": \"default\",\n"
          + "                    \"uniqueId\": \"Location1\",\n"
          + "                    \"uniqueIdType\": \"private\"\n"
          + "                },\n"
          + "                \"locations\": [\n"
          + "                    {\n"
          + "                        \"displayName\": \"Location1\",\n"
          + "                        \"locationType\": \"default\",\n"
          + "                        \"uniqueId\": \"Location1\",\n"
          + "                        \"uniqueIdType\": \"private\"\n"
          + "                    }\n"
          + "                ],\n"
          + "                \"recurrence\": null,\n"
          + "                \"attendees\": [\n"
          + "                    {\n"
          + "                        \"type\": \"required\",\n"
          + "                        \"status\": {\n"
          + "                            \"response\": \"none\",\n"
          + "                            \"time\": \"0001-01-01T00:00:00Z\"\n"
          + "                        },\n"
          + "                        \"emailAddress\": {\n"
          + "                            \"name\": \"Test Attendee\",\n"
          + "                            \"address\": \"test@test.com\"\n"
          + "                        }\n"
          + "                    }\n"
          + "                ],\n"
          + "                \"organizer\": {\n"
          + "                    \"emailAddress\": {\n"
          + "                        \"name\": \"Test TEST\",\n"
          + "                        \"address\": \"dddddd@outlook.com\"\n"
          + "                    }\n"
          + "                }\n"
          + "            }\n"
          + "        }\n"
          + "    ]\n"
          + "}";
  private MockWebServer server;
  private OkHttpClient client;
  private ObjectMapper mapper;
  private TransformerServiceImpl transformerService;
  private TokenAuthData token;

  @Test
  @SuppressWarnings("unchecked")
  public void testImport() throws Exception {
    server.enqueue(new MockResponse().setBody(BATCH_CALENDAR_RESPONSE));
    server.enqueue(new MockResponse().setResponseCode(201).setBody(BATCH_EVENT_RESPONSE));
    server.start();

    HttpUrl baseUrl = server.url("");
    MicrosoftCalendarImporter importer =
        new MicrosoftCalendarImporter(
            baseUrl.toString(), client, mapper, transformerService);

    CalendarModel calendarModel = new CalendarModel("OldId1", "name", "name");
    CalendarAttendeeModel attendeeModel =
        new CalendarAttendeeModel("Test Attendee", "test@test.com", false);
    CalendarEventModel.CalendarEventTime start =
        new CalendarEventModel.CalendarEventTime(
            ZonedDateTime.now(ZoneId.of("GMT")).toOffsetDateTime(), false);
    CalendarEventModel.CalendarEventTime end =
        new CalendarEventModel.CalendarEventTime(
            ZonedDateTime.now(ZoneId.of("GMT")).toOffsetDateTime(), false);
    CalendarEventModel eventModel =
        new CalendarEventModel(
            "OldId1",
            "Event1",
            "Test Notes",
            singletonList(attendeeModel),
            "Location1",
            start,
            end,
            null);
    CalendarContainerResource resource =
        new CalendarContainerResource(singleton(calendarModel), singleton(eventModel));

    FakeIdempotentImportExecutor executor = new FakeIdempotentImportExecutor();

    ImportResult result = importer.importItem(JOB_ID, executor, token, resource);

    assertEquals(ImportResult.ResultType.OK, result.getType());

    // verify the batch calendar request
    RecordedRequest calendarBatch = server.takeRequest();
    Map<String, Object> calendarBody =
        (Map<String, Object>) mapper.readValue(calendarBatch.getBody().readUtf8(), Map.class);

    List<Map<String, Object>> calendarRequests =
        (List<Map<String, Object>>) calendarBody.get("requests");

    assertNotNull(calendarRequests);
    assertEquals(1, calendarRequests.size());

    Map<String, Object> calendarRequest = calendarRequests.get(0);

    assertNotNull(calendarRequest.get("headers"));
    assertEquals("POST", calendarRequest.get("method"));
    assertEquals("/v1.0/me/calendars", calendarRequest.get("url"));

    Map<String, Object> calendarRequestBody = (Map<String, Object>) calendarRequest.get("body");
    assertNotNull(calendarRequestBody);
    assertEquals("name", calendarRequestBody.get("name"));

    // verify the calendar id mapping from old id to new id was saved
    assertEquals("NewId1", executor.getCachedValue("OldId1"));

    // verify the batch event request
    RecordedRequest eventBatch = server.takeRequest();
    Map<String, Object> eventRequests =
        (Map<String, Object>) mapper.readValue(eventBatch.getBody().readUtf8(), Map.class);
    Map<String, Object> eventRequest =
        (Map<String, Object>) ((List<Map<String, Object>>) eventRequests.get("requests")).get(0);

    assertNotNull(eventRequest.get("headers"));
    assertEquals("POST", eventRequest.get("method"));
    assertEquals(
        "/v1.0/me/calendars/NewId1/events",
        eventRequest.get("url")); // verify the URL is contructed correctly with NewId

    Map<String, Object> eventRequestBody = (Map<String, Object>) eventRequest.get("body");
    assertNotNull(eventRequestBody);
    assertEquals("Event1", eventRequestBody.get("subject"));

    Map<String, Object> location = (Map<String, Object>) eventRequestBody.get("location");
    assertEquals("Location1", location.get("displayName"));
    assertEquals("Default", location.get("locationType"));

    Map<String, Object> body = (Map<String, Object>) eventRequestBody.get("body");
    assertEquals("Test Notes", body.get("content"));
    assertEquals("HTML", body.get("contentType"));

    List<Map<String, Object>> attendees =
        (List<Map<String, Object>>) eventRequestBody.get("attendees");
    assertEquals(1, attendees.size());
    Map<String, Object> attendee = (Map<String, Object>) attendees.get(0);
    assertEquals("required", attendee.get("type"));
    Map<String, Object> emailAddress = (Map<String, Object>) attendee.get("emailAddress");
    assertEquals("test@test.com", emailAddress.get("address"));
    assertEquals("Test Attendee", emailAddress.get("name"));

    // verify dates
    Map<String, Object> startDate = (Map<String, Object>) eventRequestBody.get("start");
    assertNotNull(startDate.get("dateTime"));
    assertEquals("UTC", startDate.get("timeZone"));

    Map<String, Object> endDate = (Map<String, Object>) eventRequestBody.get("end");
    assertNotNull(endDate.get("dateTime"));
    assertEquals("UTC", endDate.get("timeZone"));
  }

  @BeforeEach
  public void setUp() {
    client = new OkHttpClient.Builder().build();
    mapper = new ObjectMapper();
    transformerService = new TransformerServiceImpl();
    token = new TokenAuthData("token456");
    server = new MockWebServer();
  }

  @AfterEach
  public void tearDown() throws Exception {
    server.shutdown();
  }
}
