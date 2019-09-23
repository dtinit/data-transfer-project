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
package org.datatransferproject.transfer.microsoft.calendar;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.microsoft.common.RequestHelper;
import org.datatransferproject.transfer.microsoft.transformer.TransformResult;
import org.datatransferproject.transfer.microsoft.transformer.TransformerService;
import org.datatransferproject.types.common.models.calendar.CalendarContainerResource;
import org.datatransferproject.types.common.models.calendar.CalendarEventModel;
import org.datatransferproject.types.common.models.calendar.CalendarModel;
import org.datatransferproject.types.transfer.auth.TokenAuthData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.datatransferproject.transfer.microsoft.common.RequestHelper.createRequest;

/**
 * Imports Outlook calendar information using the Microsoft Graph API.
 */
public class MicrosoftCalendarImporter
    implements Importer<TokenAuthData, CalendarContainerResource> {

  private static final String CALENDAR_SUBPATH =
      "/v1.0/me/calendars"; // must be relative for batch operations
  private static final String EVENT_SUBPATH =
      "/v1.0/me/calendars/%s/events"; // must be relative for batch operations

  private final OkHttpClient client;
  private final ObjectMapper objectMapper;
  private final TransformerService transformerService;

  private final String baseUrl;

  public MicrosoftCalendarImporter(
      String baseUrl,
      OkHttpClient client,
      ObjectMapper objectMapper,
      TransformerService transformerService) {
    this.client = client;
    this.objectMapper = objectMapper;
    this.transformerService = transformerService;
    this.baseUrl = baseUrl;
  }

  @SuppressWarnings("unchecked")
  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentImportExecutor,
      TokenAuthData authData,
      CalendarContainerResource data) throws Exception {

    for (CalendarModel calendar : data.getCalendars()) {
      idempotentImportExecutor.executeAndSwallowIOExceptions(calendar.getId(),
          calendar.getName(),
          () -> importCalendar(authData, calendar));
    }

    List<Map<String, Object>> eventRequests = new ArrayList<>();
    int requestId = 1;

    for (CalendarEventModel event : data.getEvents()) {
      // get the imported calendar id for the event from the mappings
      String importedId = idempotentImportExecutor.getCachedValue(event.getCalendarId());
      Map<String, Object> request =
          createRequestItem(event, requestId, String.format(EVENT_SUBPATH, importedId));
      requestId++;
      eventRequests.add(request);
    }


    RequestHelper.BatchResponse eventResponse =
        RequestHelper.batchRequest(authData, eventRequests, baseUrl, client, objectMapper);
    if (ImportResult.ResultType.OK != eventResponse.getResult().getType()) {
      // TODO log problems
      return eventResponse.getResult();
    }

    return eventResponse.getResult();
  }

  private String importCalendar(TokenAuthData authData,
      CalendarModel calendar) throws Exception {
    List<Map<String, Object>> calendarRequests = new ArrayList<>();

    Map<String, Object> request = createRequestItem(calendar, 1, CALENDAR_SUBPATH);
    calendarRequests.add(request);

    RequestHelper.BatchResponse calendarResponse =
        RequestHelper.batchRequest(authData, calendarRequests, baseUrl, client, objectMapper);
    if (ImportResult.ResultType.OK != calendarResponse.getResult().getType()) {
      // TODO log problems
      throw new IOException("Problem importing calendar: " + calendarResponse.getResult());
    }

    Map<String, Object> body = (Map<String, Object>) calendarResponse.getBatchResponse()
        .get(0).get("body");
    return (String) body.get("id");
  }

  private Map<String, Object> createRequestItem(
      Object item, int id, String url) throws Exception {
    TransformResult<LinkedHashMap> result = transformerService.transform(LinkedHashMap.class, item);
    if (result.getProblems() != null && !result.getProblems().isEmpty()) {
      throw new IOException("Problem transforming request: " + result.getProblems().get(0));
    }
    LinkedHashMap contact = result.getTransformed();
    return createRequest(id, url, contact);
  }
}
