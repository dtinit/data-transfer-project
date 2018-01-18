/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.serviceProviders.microsoft.calendar;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Key;
import com.google.common.base.Joiner;
import java.io.IOException;
import java.net.URL;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.dataportabilityproject.dataModels.ContinuationInformation;
import org.dataportabilityproject.dataModels.ExportInformation;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.dataModels.PaginationInformation;
import org.dataportabilityproject.dataModels.Resource;
import org.dataportabilityproject.dataModels.calendar.CalendarModel;
import org.dataportabilityproject.dataModels.calendar.CalendarModelWrapper;
import org.dataportabilityproject.shared.IdOnlyResource;

/**
 * Stub for the Microsoft calendar service.
 */
public class MicrosoftCalendarService
    implements Importer<CalendarModelWrapper>, Exporter<CalendarModelWrapper> {

  private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
  private static final String HEADER_PREFIX = "Bearer ";

  private final HttpRequestFactory requestFactory;

  public MicrosoftCalendarService(String token, String account) {
    this.requestFactory =
        HTTP_TRANSPORT.createRequestFactory(
            request -> {
              String headerValue = HEADER_PREFIX + token;
              request.getHeaders().setAuthorization(headerValue);
              request.getHeaders().setAccept(
                  "text/*, application/xml, application/json;odata.metadata=none;odata.streaming=false");
              // TODO: add if needed: request.getHeaders().set("X-AnchorMailbox", account);
              request.getHeaders().setUserAgent("PlayGroundAgent/1.0");
            });
  }

  // Formats in ISO Date Time format
  private static String formatTime(ZonedDateTime dateTime) {
    return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmX")
        .withZone(ZoneOffset.UTC)
        .format(dateTime);
  }

  @Override
  public void importItem(CalendarModelWrapper object) throws IOException {
    System.out.println("importItem: " + object);
  }

  @Override
  public CalendarModelWrapper export(ExportInformation exportInformation)
      throws IOException {
    if (exportInformation.getResource().isPresent()) {
      return getCalendarEvents(
          ((IdOnlyResource) exportInformation.getResource().get()).getId(),
          exportInformation.getPaginationInformation());
    } else {
      return getCalendars(exportInformation.getPaginationInformation());
    }

  }

  private CalendarModelWrapper getCalendars(Optional<PaginationInformation> pageInfo)
      throws IOException {
    URL url = new URL("https://outlook.office.com/api/v2.0/me/calendars");

    HttpRequest getRequest = requestFactory.buildGetRequest(new GenericUrl(url));
    getRequest.setParser(new JsonObjectParser(new JacksonFactory()));
    HttpResponse response;
    try {
      response = getRequest.execute();
    } catch (HttpResponseException e) {
      System.out.println("Error fetching content");
      System.out.println("response status code: " + e.getStatusCode());
      System.out.println("response status message: " + e.getStatusMessage());
      System.out.println("response headers: " + e.getHeaders());
      System.out.println("response content: " + e.getContent());
      e.printStackTrace();
      throw e;
    }
    int statusCode = response.getStatusCode();
    if (statusCode != 200) {
      throw new IOException(
          "Bad status code: " + statusCode + " error: " + response.getStatusMessage());
    }

    // Parse response into model
    OutlookCalendarList data = response.parseAs(OutlookCalendarList.class);

    List<CalendarModel> calendars = new ArrayList<>(data.list.size());
    List<Resource> resources = new ArrayList<>(data.list.size());
    for (OutlookCalendar calendar : data.list) {
      calendars.add(new CalendarModel(calendar.id, calendar.name, null));
      resources.add(new IdOnlyResource(calendar.id));
    }

    return new CalendarModelWrapper(
        calendars,
        null,
        new ContinuationInformation(resources, null));

  }

  private CalendarModelWrapper getCalendarEvents(String calendarId,
      Optional<PaginationInformation> pageInfo) throws IOException {
    ZonedDateTime end = ZonedDateTime.now();     // The current date and time
    ZonedDateTime begin = end.minusDays(90);

    String eventsUrl = String.format(
        "https://outlook.office.com/api/v2.0/me/calendars/%s/calendarview?startDateTime=%s&endDateTime=%s",
        calendarId, formatTime(begin), formatTime(end));
    System.out.println("calendar: " + calendarId);
    System.out.println("eventsUrl: "
        + eventsUrl); // TODO: Determine why this URL works in the MS Oauth Playground but not here

    // Make requests for events
    HttpRequest getRequest = requestFactory.buildGetRequest(new GenericUrl(eventsUrl));
    HttpResponse response;
    try {
      response = getRequest.execute();
    } catch (HttpResponseException e) {
      System.out.println("Error fetching content");
      System.out.println("response status code: " + e.getStatusCode());
      System.out.println("response status message: " + e.getStatusMessage());
      System.out.println("response headers: " + e.getHeaders());
      System.out.println("response content: " + e.getContent());
      e.printStackTrace();
      throw e;
    }
    int statusCode = response.getStatusCode();
    if (statusCode != 200) {
      throw new IOException(
          "Bad status code: " + statusCode + " error: " + response.getStatusMessage());
    }

    // TODO: Parse with JSON and add to model
    // Currently this is not working as it is not returning events.
    System.out.println("response headers" + response.getHeaders());
    System.out.println("response status message" + response.getStatusMessage());
    System.out.println("\n\n" + "events resp: " + response.parseAsString() + "\n\n");

    // TODO(chuy): return actual results here.
    return null;
  }

  public static class OutlookCalendarList {

    @Key("@odata.context")
    public String context;

    @Key("value")
    public List<OutlookCalendar> list;

    @Override
    public String toString() {
      return String.format("OutlookCalendarList(context=%s list=%s)",
          context,
          (null == list || list.isEmpty()) ? "" : Joiner.on("\n").join(list));
    }
  }

  public static class OutlookCalendar {

    @Key("@odata.id")
    public String odataId;

    @Key("Id")
    public String id;

    @Key("Name")
    public String name;

    @Key("Color")
    public String color;

    @Override
    public String toString() {
      return String.format("OutlookCalendar(Id=%s Name=%s Color=%s)",
          id, name, color);
    }
  }
}
