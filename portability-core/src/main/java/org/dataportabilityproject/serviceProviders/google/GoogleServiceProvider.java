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
package org.dataportabilityproject.serviceProviders.google;

import static org.dataportabilityproject.shared.PortableDataType.CALENDAR;
import static org.dataportabilityproject.shared.PortableDataType.MAIL;
import static org.dataportabilityproject.shared.PortableDataType.PHOTOS;
import static org.dataportabilityproject.shared.PortableDataType.TASKS;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.tasks.TasksScopes;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.serviceProviders.google.calendar.GoogleCalendarService;
import org.dataportabilityproject.serviceProviders.google.mail.GoogleMailService;
import org.dataportabilityproject.serviceProviders.google.piccasa.GooglePhotosService;
import org.dataportabilityproject.serviceProviders.google.tasks.GoogleTaskService;
import org.dataportabilityproject.shared.AppCredentialFactory;
import org.dataportabilityproject.shared.AppCredentials;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.ServiceMode;
import org.dataportabilityproject.shared.ServiceProvider;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;

/**
 * The {@link ServiceProvider} for Google (http://www.google.com/).
 */
final class GoogleServiceProvider implements ServiceProvider {

  private final static ImmutableList<PortableDataType> SUPPORTED_DATA_TYPES = ImmutableList
      .of(CALENDAR, MAIL, PHOTOS, TASKS);

  // The scopes necessary to read or write each PortableDataType and ServiceMode pair
  // Scopes for EXPORT should contain READONLY permissions
  final private static Map<PortableDataType, ListMultimap<ServiceMode, String>> DATA_TYPE_SCOPES =
      ImmutableMap.<PortableDataType, ListMultimap<ServiceMode, String>>builder()
          .put(CALENDAR, ImmutableListMultimap.<ServiceMode, String>builder()
              .putAll(ServiceMode.IMPORT, Arrays.asList(CalendarScopes.CALENDAR))
              .putAll(ServiceMode.EXPORT, Arrays.asList(CalendarScopes.CALENDAR_READONLY))
              .build())
          .put(MAIL, ImmutableListMultimap.<ServiceMode, String>builder()
              .putAll(ServiceMode.IMPORT, Arrays.asList(GmailScopes.GMAIL_MODIFY))
              .putAll(ServiceMode.EXPORT, Arrays.asList(GmailScopes.GMAIL_READONLY))
              .build())
          .put(PHOTOS, ImmutableListMultimap.<ServiceMode, String>builder()
              //picasaweb does not have a READONLY scope
              .putAll(ServiceMode.IMPORT, Arrays.asList("https://picasaweb.google.com/data/"))
              .putAll(ServiceMode.EXPORT, Arrays.asList("https://picasaweb.google.com/data/"))
              .build())
          .put(TASKS, ImmutableListMultimap.<ServiceMode, String>builder()
              .putAll(ServiceMode.IMPORT, Arrays.asList(TasksScopes.TASKS))
              .putAll(ServiceMode.EXPORT, Arrays.asList(TasksScopes.TASKS_READONLY))
              .build())
          .build();

  private final static Map<PortableDataType, Map<ServiceMode, GoogleAuth>> DATA_TYPE_AUTHS = new HashMap<>();

  private final AppCredentials appCredentials;

  @Inject
  GoogleServiceProvider(AppCredentialFactory appCredentialFactory) throws Exception {
    this.appCredentials =
        appCredentialFactory.lookupAndCreate("GOOGLE_KEY", "GOOGLE_SECRET");
  }


  @Override
  public String getName() {
    return "Google";
  }

  @Override
  public ImmutableList<PortableDataType> getExportTypes() {
    return SUPPORTED_DATA_TYPES;
  }

  @Override
  public ImmutableList<PortableDataType> getImportTypes() {
    return SUPPORTED_DATA_TYPES;
  }

  @Override
  public OfflineAuthDataGenerator getOfflineAuthDataGenerator(PortableDataType dataType,
      ServiceMode serviceMode) {
    return lookupAndCreateGoogleAuth(dataType, serviceMode);
  }

  @Override
  public OnlineAuthDataGenerator getOnlineAuthDataGenerator(PortableDataType dataType,
      ServiceMode serviceMode) {
    return lookupAndCreateGoogleAuth(dataType, serviceMode);
  }

  @Override
  public Exporter<? extends DataModel> getExporter(
      PortableDataType type,
      AuthData authData,
      JobDataCache jobDataCache)
      throws IOException {

    Credential cred = lookupAndCreateGoogleAuth(type, ServiceMode.EXPORT).getCredential(authData);

    if (!cred.refreshToken()) {
      throw new IOException("Couldn't refresh token");
    }
    switch (type) {
      case CALENDAR:
        return new GoogleCalendarService(cred, jobDataCache);
      case MAIL:
        return new GoogleMailService(cred);
      case PHOTOS:
        return new GooglePhotosService(cred, jobDataCache);
      case TASKS:
        return new GoogleTaskService(cred, jobDataCache);
      default:
        throw new IllegalArgumentException("Type " + type + " is not supported");
    }
  }

  @Override
  public Importer<? extends DataModel> getImporter(
      PortableDataType type,
      AuthData authData,
      JobDataCache jobDataCache) throws IOException {
    Credential cred = lookupAndCreateGoogleAuth(type, ServiceMode.IMPORT).getCredential(authData);
    if (!cred.refreshToken()) {
      throw new IOException("Couldn't refresh token");
    }
    switch (type) {
      case CALENDAR:
        return new GoogleCalendarService(cred, jobDataCache);
      case MAIL:
        return new GoogleMailService(cred);
      case PHOTOS:
        return new GooglePhotosService(cred, jobDataCache);
      case TASKS:
        return new GoogleTaskService(cred, jobDataCache);
      default:
        throw new IllegalArgumentException("Type " + type + " is not supported");
    }
  }

  private GoogleAuth lookupAndCreateGoogleAuth(PortableDataType dataType, ServiceMode serviceMode) {
    Preconditions.checkArgument(SUPPORTED_DATA_TYPES.contains(dataType),
        "[%s] mode for type [%s] is not supported by Google.", serviceMode, dataType);

    if (!DATA_TYPE_AUTHS.containsKey(dataType)) {
      DATA_TYPE_AUTHS.put(dataType, new HashMap<>());
    }

    Map<ServiceMode, GoogleAuth> googleAuth = DATA_TYPE_AUTHS.get(dataType);
    if (!googleAuth.containsKey(serviceMode)) {
      googleAuth.put(serviceMode, new GoogleAuth(appCredentials,
          DATA_TYPE_SCOPES.get(dataType).get(serviceMode)));
    }

    return googleAuth.get(serviceMode);
  }
}
