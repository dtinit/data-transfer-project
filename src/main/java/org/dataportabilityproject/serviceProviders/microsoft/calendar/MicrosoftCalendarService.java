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
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.dataModels.calendar.CalendarModel;

/**
 * Stub for the Microsoft calendar service.
 */
public class MicrosoftCalendarService implements Importer<CalendarModel>, Exporter<CalendarModel> {
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final String HEADER_PREFIX = "Bearer ";
    private String token;
    public MicrosoftCalendarService(String token) {
        this.token = token;
    }

    @Override public void importItem(CalendarModel object) throws IOException {
      System.out.println("importItem: " + object);
    }

    @Override public Collection<CalendarModel> export() throws IOException {
      return getCalendars();
    }

    private Collection<CalendarModel> getCalendars() throws IOException {
      URL url = new URL("https://outlook.office.com/api/v2.0/me/calendars");
  
      HttpRequestFactory requestFactory =
          HTTP_TRANSPORT.createRequestFactory(
              request -> {
                String headerValue = HEADER_PREFIX + token;
                request.getHeaders().setAuthorization(headerValue);
                request.getHeaders().setAccept("text/*, application/xml, application/json;odata.metadata=none;odata.streaming=false");
                request.getHeaders().set("X-AnchorMailbox", "jchavezbaz@live.com");
                request.getHeaders().setUserAgent("PlayGroundAgent/1.0");
              });
      HttpRequest getRequest = requestFactory.buildGetRequest(new GenericUrl(url));
      getRequest.setParser(new JsonObjectParser(new JacksonFactory()));
      HttpResponse response;
      try {
        response = getRequest.execute();
      } catch (HttpResponseException e) {
        System.out.println("Error fetching content");
        System.out.println("response status code: " + e.getStatusCode());
        System.out.println("response status message: " +e.getStatusMessage());
        System.out.println("response headers: " + e.getHeaders());
        System.out.println("response content: " + e.getContent());
        e.printStackTrace();
        throw e;
      }
      int statusCode = response.getStatusCode();
      if (statusCode != 200) {
          throw new IOException("Bad status code: " + statusCode + " error: " + response.getStatusMessage());
      }

      // Parse response into model
      OutlookCalendarList data = response.parseAs(OutlookCalendarList.class);

      ImmutableList.Builder<CalendarModel> builder = ImmutableList.builder();
      ZonedDateTime end = ZonedDateTime.now();     // The current date and time
      ZonedDateTime begin = end.minusDays(90);

      for (OutlookCalendar calendar : data.list) {

        String eventsUrl = String.format("https://outlook.office.com/api/v2.0/me/calendars/%s/calendarview?startDateTime=%s&endDateTime=%s",
            calendar.id, formatTime(begin), formatTime(end));
        System.out.println("calendar: " + calendar);
        System.out.println("eventsUrl: " + eventsUrl); // TODO: Determine why this URL works in the MS Oauth Playground but not here
        
        // Make requests for events
        requestFactory.buildGetRequest(new GenericUrl(eventsUrl));
        response = null;
        try {
          response = getRequest.execute();
        } catch (HttpResponseException e) {
          System.out.println("Error fetching content");
          System.out.println("response status code: " + e.getStatusCode());
          System.out.println("response status message: " +e.getStatusMessage());
          System.out.println("response headers: " + e.getHeaders());
          System.out.println("response content: " + e.getContent());
          e.printStackTrace();
          throw e;
        }
        statusCode = response.getStatusCode();
        if (statusCode != 200) {
            throw new IOException("Bad status code: " + statusCode + " error: " + response.getStatusMessage());
        }

        // TODO: Parse with JSON and add to model
        // Currently this is not working as it is not returning events.
        System.out.println("response headers" + response.getHeaders());
        System.out.println("response status message" + response.getStatusMessage());
        System.out.println("\n\n" + "events resp: " + response.parseAsString() + "\n\n");
        
        builder.add(new CalendarModel(calendar.name, null, null));
      }
      return builder.build();
    }
 
    // Formats in ISO Date Time format
    private static String formatTime(ZonedDateTime dateTime) {
      return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmX")
          .withZone(ZoneOffset.UTC)
          .format(dateTime);
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
