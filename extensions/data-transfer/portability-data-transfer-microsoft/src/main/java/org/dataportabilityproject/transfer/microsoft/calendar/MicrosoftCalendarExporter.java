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
package org.dataportabilityproject.transfer.microsoft.calendar;

import static org.dataportabilityproject.transfer.microsoft.transformer.TransformConstants.CALENDAR_ID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.dataportabilityproject.spi.transfer.provider.ExportResult;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.types.ExportInformation;
import org.dataportabilityproject.transfer.microsoft.provider.MicrosoftTransferExtension;
import org.dataportabilityproject.transfer.microsoft.transformer.TransformResult;
import org.dataportabilityproject.transfer.microsoft.transformer.TransformerService;
import org.dataportabilityproject.types.transfer.auth.TokenAuthData;
import org.dataportabilityproject.types.transfer.models.calendar.CalendarContainerResource;
import org.dataportabilityproject.types.transfer.models.calendar.CalendarEventModel;
import org.dataportabilityproject.types.transfer.models.calendar.CalendarModel;

/** Exports Outlook calendar information using the Microsoft Graph API. */
public class MicrosoftCalendarExporter
    implements Exporter<TokenAuthData, CalendarContainerResource> {
  private static final String CALENDARS_URL = "/v1.0/me/calendars";
  private static final String EVENTS_URL = "/v1.0/me/calendars/%s/events";
  private static final String ODATA_NEXT = "@odata.nextLink";

  private final String baseUrl;

  private final OkHttpClient client;
  private final ObjectMapper objectMapper;
  private final TransformerService transformerService;

  public MicrosoftCalendarExporter(
      OkHttpClient client, ObjectMapper objectMapper, TransformerService transformerService) {
    this("https://graph.microsoft.com/", client, objectMapper, transformerService);
  }

  @VisibleForTesting
  public MicrosoftCalendarExporter(
      String baseUrl,
      OkHttpClient client,
      ObjectMapper objectMapper,
      TransformerService transformerService) {
    this.client = client;
    this.objectMapper = objectMapper;
    this.transformerService = transformerService;
    this.baseUrl = baseUrl;
  }

  @Override
  public String getServiceId() {
    return MicrosoftTransferExtension.SERVICE_ID;
  }

  @Override
  public ExportResult<CalendarContainerResource> export(TokenAuthData authData) {
    Request.Builder calendarsBuilder = getBuilder(baseUrl + CALENDARS_URL, authData);

    List<CalendarModel> calendarModels = new ArrayList<>();
    try (Response graphResponse = client.newCall(calendarsBuilder.build()).execute()) {
      ResponseBody body = graphResponse.body();
      if (body == null) {
        return new ExportResult<>(
            ExportResult.ResultType.ERROR, "Error retrieving contacts: response body was null");
      }
      String graphBody = new String(body.bytes());
      Map graphMap = objectMapper.reader().forType(Map.class).readValue(graphBody);

      // TODO String nextLink = (String) graphMap.get(ODATA_NEXT);
      // TODO ContinuationData continuationData = nextLink == null
      // ? null : new ContinuationData(new GraphPagination(nextLink));

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> rawCalendars = (List<Map<String, Object>>) graphMap.get("value");
      if (rawCalendars == null) {
        return new ExportResult<>(ExportResult.ResultType.END);
      }
      for (Map<String, Object> rawCalendar : rawCalendars) {

        TransformResult<CalendarModel> result =
            transformerService.transform(CalendarModel.class, rawCalendar);
        if (result.hasProblems()) {
          // discard
          // FIXME log problem
          continue;
        }

        calendarModels.add(result.getTransformed());
      }
    } catch (IOException e) {
      e.printStackTrace(); // FIXME log error
      return new ExportResult<>(
          ExportResult.ResultType.ERROR, "Error retrieving calendar: " + e.getMessage());
    }

    List<CalendarEventModel> calendarEventModels = new ArrayList<>();

    for (CalendarModel calendarModel : calendarModels) {
      String id = calendarModel.getId();
      Request.Builder eventsBuilder = getBuilder(calculateEventsUrl(id), authData);

      try (Response graphResponse = client.newCall(eventsBuilder.build()).execute()) {
        ResponseBody body = graphResponse.body();
        if (body == null) {
          return new ExportResult<>(
              ExportResult.ResultType.ERROR, "Error retrieving calendar: response body was null");
        }
        String graphBody = new String(body.bytes());
        Map graphMap = objectMapper.reader().forType(Map.class).readValue(graphBody);

        // TODO String nextLink = (String) graphMap.get(ODATA_NEXT);
        // TODO ContinuationData continuationData = nextLink == null
        // ? null : new ContinuationData(new GraphPagination(nextLink));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawEvents = (List<Map<String, Object>>) graphMap.get("value");
        if (rawEvents == null) {
          return new ExportResult<>(ExportResult.ResultType.END);
        }

        for (Map<String, Object> rawEvent : rawEvents) {
          Map<String, String> properties = new HashMap<>();
          properties.put(CALENDAR_ID, id);
          TransformResult<CalendarEventModel> result =
              transformerService.transform(CalendarEventModel.class, rawEvent, properties);
          if (result.hasProblems()) {
            // discard
            // FIXME log problem
            continue;
          }
          calendarEventModels.add(result.getTransformed());
        }

      } catch (IOException e) {
        e.printStackTrace(); // FIXME log error
        return new ExportResult<>(
            ExportResult.ResultType.ERROR, "Error retrieving contacts: " + e.getMessage());
      }
    }

    CalendarContainerResource resource =
        new CalendarContainerResource(calendarModels, calendarEventModels);
    return new ExportResult<>(ExportResult.ResultType.END, resource, null);
  }

  private String calculateEventsUrl(String eventId) {
    return baseUrl + String.format(EVENTS_URL, eventId);
  }

  @Override
  public ExportResult<CalendarContainerResource> export(
      TokenAuthData authData, ExportInformation exportInformation) {
    // TODO support pagination
    throw new UnsupportedOperationException();
  }

  private Request.Builder getBuilder(String url, TokenAuthData authData) {
    Request.Builder calendarsRequestBuilder = new Request.Builder().url(url);
    calendarsRequestBuilder.header("Authorization", "Bearer " + authData.getToken());
    return calendarsRequestBuilder;
  }
}
