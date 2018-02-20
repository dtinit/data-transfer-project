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
package org.dataportabilityproject.serviceProviders.google.calendar;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.dataModels.ContinuationInformation;
import org.dataportabilityproject.dataModels.ExportInformation;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.dataModels.PaginationInformation;
import org.dataportabilityproject.dataModels.Resource;
import org.dataportabilityproject.dataModels.calendar.CalendarEventModel;
import org.dataportabilityproject.dataModels.calendar.CalendarModel;
import org.dataportabilityproject.dataModels.calendar.CalendarModelWrapper;
import org.dataportabilityproject.serviceProviders.google.GoogleStaticObjects;
import org.dataportabilityproject.shared.IdOnlyResource;
import org.dataportabilityproject.shared.StringPaginationToken;

//TODO(repeated events are ignored)
public class GoogleCalendarService
    implements Exporter<CalendarModelWrapper>, Importer<CalendarModelWrapper> {

  private final Calendar calendarClient;
  private final JobDataCache jobDataCache;

  public GoogleCalendarService(Credential credential, JobDataCache jobDataCache) {
    this(new Calendar.Builder(
        GoogleStaticObjects.getHttpTransport(), GoogleStaticObjects.JSON_FACTORY, credential)
        .setApplicationName(GoogleStaticObjects.APP_NAME)
        .build(), jobDataCache);
  }

  GoogleCalendarService(Calendar calendarClient, JobDataCache jobDataCache) {
    this.calendarClient = calendarClient;
    this.jobDataCache = jobDataCache;
  }

  @Override
  public CalendarModelWrapper export(ExportInformation exportInformation) throws IOException {
    if (exportInformation.getResource().isPresent()) {
      String calendarId = ((IdOnlyResource) exportInformation.getResource().get()).getId();
      return getCalendarEvents(calendarId, exportInformation.getPaginationInformation());
    } else {
      return exportCalendars(exportInformation.getPaginationInformation());
    }
  }

  private CalendarModelWrapper exportCalendars(Optional<PaginationInformation> pageInfo)
      throws IOException {
    Calendar.CalendarList.List listRequest = calendarClient.calendarList().list();

    if (pageInfo.isPresent()) {
      listRequest.setPageToken(((StringPaginationToken) pageInfo.get()).getId());
    }

    CalendarList listResult = listRequest.execute();
    List<CalendarModel> calendarModels = new ArrayList<>(listResult.getItems().size());
    List<Resource> resources = new ArrayList<>(listResult.getItems().size());
    for (CalendarListEntry calendarData : listResult.getItems()) {
      CalendarModel model = GoogleCalendarToModelConverter.convertToCalendarModel(calendarData);
      resources.add(new IdOnlyResource(calendarData.getId()));
      calendarModels.add(model);
    }

    PaginationInformation newPageInfo = null;
    if (listResult.getNextPageToken() != null) {
      newPageInfo = new StringPaginationToken(listResult.getNextPageToken());
    }

    return new CalendarModelWrapper(
        calendarModels,
        null,
        new ContinuationInformation(resources, newPageInfo));
  }

  private CalendarModelWrapper getCalendarEvents(
      String id, Optional<PaginationInformation> pageInfo) throws IOException {

    Calendar.Events.List listRequest = calendarClient.events().list(id).setMaxAttendees(100);
    if (pageInfo.isPresent()) {
      listRequest.setPageToken(((StringPaginationToken) pageInfo.get()).getId());
    }
    Events listResult = listRequest.execute();
    List<CalendarEventModel> results = new ArrayList<>(listResult.getItems().size());
    for (Event eventData : listResult.getItems()) {
      CalendarEventModel model = GoogleCalendarToModelConverter
          .convertToCalendarEventModel(id, eventData);
      results.add(model);
    }

    PaginationInformation newPageInfo = null;
    if (listResult.getNextPageToken() != null) {
      newPageInfo = new StringPaginationToken(listResult.getNextPageToken());
    }

    return new CalendarModelWrapper(
        null,
        results,
        new ContinuationInformation(null, newPageInfo));
  }

  @Override
  public void importItem(CalendarModelWrapper wrapper) throws IOException {
    for (CalendarModel calendar : wrapper.getCalendars()) {
      com.google.api.services.calendar.model.Calendar toInsert =
          ModelToGoogleCalendarConverter.convertToGoogleCalendar(calendar);
      com.google.api.services.calendar.model.Calendar calendarResult =
          calendarClient.calendars().insert(toInsert).execute();
      jobDataCache.store(calendar.getId(), calendarResult.getId());
    }

    for (CalendarEventModel eventModel : wrapper.getEvents()) {
      Event event = ModelToGoogleCalendarConverter.convertToGoogleCalendarEvent(eventModel);
      String newCalendarId = jobDataCache.getData(eventModel.getCalendarId(), String.class);
      calendarClient.events().insert(newCalendarId, event).execute();
    }
  }
}
