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
import java.io.IOException;
import okhttp3.OkHttpClient;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.ImportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.spi.transfer.types.TempCalendarData;
import org.datatransferproject.transfer.microsoft.common.RequestHelper;
import org.datatransferproject.transfer.microsoft.transformer.TransformResult;
import org.datatransferproject.transfer.microsoft.transformer.TransformerService;
import org.datatransferproject.types.transfer.auth.TokenAuthData;
import org.datatransferproject.types.transfer.models.calendar.CalendarContainerResource;
import org.datatransferproject.types.transfer.models.calendar.CalendarEventModel;
import org.datatransferproject.types.transfer.models.calendar.CalendarModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.datatransferproject.transfer.microsoft.common.RequestHelper.createRequest;

/** Imports Outlook calendar information using the Microsoft Graph API. */
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
  private final JobStore jobStore;

  public MicrosoftCalendarImporter(
      String baseUrl,
      OkHttpClient client,
      ObjectMapper objectMapper,
      TransformerService transformerService,
      JobStore jobStore) {
    this.client = client;
    this.objectMapper = objectMapper;
    this.transformerService = transformerService;
    this.baseUrl = baseUrl;
    this.jobStore = jobStore;
  }

  @SuppressWarnings("unchecked")
  @Override
  public ImportResult importItem(
      UUID jobId, TokenAuthData authData, CalendarContainerResource data) {
    TempCalendarData calendarMappings = jobStore.findData(jobId, createCacheKey(), TempCalendarData.class);
    if (calendarMappings == null) {
      calendarMappings = new TempCalendarData(jobId);
      try {
        jobStore.create(jobId, createCacheKey(), calendarMappings);
      } catch (IOException e) {
        return new ImportResult(e);
      }
    }

    Map<String, String> requestIdToExportedId = new HashMap<>();

    List<String> problems = new ArrayList<>();

    int requestId = 1;

    List<Map<String, Object>> calendarRequests = new ArrayList<>();

    for (CalendarModel calendar : data.getCalendars()) {
      Map<String, Object> request = createRequestItem(calendar, requestId, CALENDAR_SUBPATH, problems);
      requestIdToExportedId.put(String.valueOf(requestId), calendar.getId());
      requestId++;
      calendarRequests.add(request);
    }

    if (!problems.isEmpty()) {
      // TODO log problems
    }

    RequestHelper.BatchResponse calendarResponse =
        RequestHelper.batchRequest(authData, calendarRequests, baseUrl, client, objectMapper);
    if (ImportResult.ResultType.OK != calendarResponse.getResult().getType()) {
      // TODO log problems
      return calendarResponse.getResult();
    }

    List<Map<String, Object>> batchResponses = calendarResponse.getBatchResponse();
    for (Map<String, Object> batchResponse : batchResponses) {
      String batchRequestId = (String) batchResponse.get("id");
      if (batchRequestId == null) {
        problems.add("Null request id returned by batch response");
        continue;
      }
      Integer status = (Integer) batchResponse.get("status");
      if (status == null || 201 != status) {
        problems.add("Error creating calendar: " + batchRequestId);
        continue;
      }
      Map<String, Object> body = (Map<String, Object>) batchResponse.get("body");
      if (body == null) {
        problems.add("Invalid body returned from batch calendar create: " + batchRequestId);
        continue;
      }
      calendarMappings.addIdMapping(
          requestIdToExportedId.get(batchRequestId), (String) body.get("id"));
    }
    jobStore.update(jobId, createCacheKey(), calendarMappings);

    List<Map<String, Object>> eventRequests = new ArrayList<>();
    requestId = 1;

    for (CalendarEventModel event : data.getEvents()) {
      String importedId =
          calendarMappings.getImportedId(
              event
                  .getCalendarId()); // get the imported calendar id for the event from the mappings
      Map<String, Object> request =
          createRequestItem(event, requestId, String.format(EVENT_SUBPATH, importedId), problems);
      requestId++;
      eventRequests.add(request);
    }

    if (!problems.isEmpty()) {
      // TODO log problems
    }

    RequestHelper.BatchResponse eventResponse =
        RequestHelper.batchRequest(authData, eventRequests, baseUrl, client, objectMapper);
    if (ImportResult.ResultType.OK != eventResponse.getResult().getType()) {
      // TODO log problems
      return eventResponse.getResult();
    }

    return eventResponse.getResult();
  }

  private Map<String, Object> createRequestItem(
      Object item, int id, String url, List<String> problems) {
    TransformResult<LinkedHashMap> result = transformerService.transform(LinkedHashMap.class, item);
    problems.addAll(result.getProblems());
    LinkedHashMap contact = result.getTransformed();
    return createRequest(id, url, contact);
  }

  /** Key for cache of album mappings.
   * TODO: Add a method parameter for a {@code key} for fine grained objects.
   */
  private String createCacheKey() {
    // TODO: store objects containing individual mappings instead of single object containing all mappings
    return "tempCalendarData";
  }
}
