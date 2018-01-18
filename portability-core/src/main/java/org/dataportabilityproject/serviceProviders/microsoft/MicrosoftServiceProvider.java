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
package org.dataportabilityproject.serviceProviders.microsoft;

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
import org.dataportabilityproject.serviceProviders.microsoft.calendar.MicrosoftCalendarService;
import org.dataportabilityproject.serviceProviders.microsoft.mail.MicrosoftMailService;
import org.dataportabilityproject.shared.AppCredentialFactory;
import org.dataportabilityproject.shared.AppCredentials;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.ServiceMode;
import org.dataportabilityproject.shared.ServiceProvider;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.PasswordAuthData;
import org.dataportabilityproject.shared.auth.PasswordAuthDataGenerator;

/**
 * The {@link ServiceProvider} for Microsoft (http://www.microsoft.com/).
 */
final class MicrosoftServiceProvider implements ServiceProvider {

  // The list of supported dataTypes for import and export.
  // TODO: support IMPORT for MAIL.
  private final static Map<ServiceMode, ImmutableList<PortableDataType>> SUPPORTED_DATA_TYPES = ImmutableMap.<ServiceMode, ImmutableList<PortableDataType>>builder()
      .put(ServiceMode.IMPORT, ImmutableList.of(PortableDataType.CALENDAR))
      .put(ServiceMode.EXPORT, ImmutableList.of(PortableDataType.CALENDAR, PortableDataType.MAIL))
      .build();
  private final static Map<PortableDataType, ListMultimap<ServiceMode, String>> DATA_TYPE_SCOPES =
      ImmutableMap.<PortableDataType, ListMultimap<ServiceMode, String>>builder()
          .put(PortableDataType.CALENDAR, ImmutableListMultimap.<ServiceMode, String>builder()
              .putAll(ServiceMode.EXPORT, Arrays.asList("wl.calendars", "wl.contacts_calendars"))
              .putAll(ServiceMode.IMPORT, Arrays.asList("wl.calendars_update"))
              .build())
          // Support for more service scopes go here
          .build();
  private final static Map<PortableDataType, Map<ServiceMode, MicrosoftAuth>> DATA_TYPE_AUTHS = new HashMap<>();
  // passwordAuth is used for mail export
  private final PasswordAuthDataGenerator passwordAuth;
  // credentials to pass into AuthDataGenerators
  private final AppCredentials appCredentials;

  @Inject
  MicrosoftServiceProvider(AppCredentialFactory appCredentialFactory) throws IOException {
    this.appCredentials =
        appCredentialFactory.lookupAndCreate("MICROSOFT_KEY", "MICROSOFT_SECRET");
    this.passwordAuth = new PasswordAuthDataGenerator();
  }

  @Override
  public String getName() {
    return "Microsoft";
  }

  @Override
  public ImmutableList<PortableDataType> getExportTypes() {
    return SUPPORTED_DATA_TYPES.get(ServiceMode.EXPORT);
  }

  @Override
  public ImmutableList<PortableDataType> getImportTypes() {
    return SUPPORTED_DATA_TYPES.get(ServiceMode.IMPORT);
  }

  @Override
  public OfflineAuthDataGenerator getOfflineAuthDataGenerator(PortableDataType dataType,
      ServiceMode serviceMode) {
    Preconditions.checkArgument(SUPPORTED_DATA_TYPES.get(serviceMode).contains(dataType),
        "[%s] of type [%s] is not supported by Microsoft.", serviceMode, dataType);
    switch (dataType) {
      case CALENDAR:
        return getAndCreateMicrosoftAuth(dataType, serviceMode);
      case MAIL:
        return passwordAuth;
      default:
        throw new IllegalArgumentException("Type [" + dataType + "] is not supported.");
    }
  }

  @Override
  public OnlineAuthDataGenerator getOnlineAuthDataGenerator(PortableDataType dataType,
      ServiceMode serviceMode) {
    Preconditions.checkArgument(SUPPORTED_DATA_TYPES.get(serviceMode).contains(dataType),
        "[%s] of type [%s] is not supported by Microsoft", serviceMode, dataType);
    switch (dataType) {
      case CALENDAR:
        return getAndCreateMicrosoftAuth(dataType, serviceMode);
      case MAIL:
        return passwordAuth;
      default:
        throw new IllegalArgumentException("Type [" + dataType + "] is not supported.");
    }
  }

  @Override
  public Exporter<? extends DataModel> getExporter(
      PortableDataType type,
      AuthData authData,
      JobDataCache jobDataCache) throws IOException {
    Preconditions.checkArgument(SUPPORTED_DATA_TYPES.get(ServiceMode.EXPORT).contains(type),
        "Export of type [%s] is not supported by Microsoft.", type);
    switch (type) {
      case CALENDAR:
        return getCalendarService(authData);
      case MAIL:
        return getMailService(authData);
      default:
        throw new IllegalArgumentException("Type [" + type + "] is not supported.");
    }
  }

  @Override
  public Importer<? extends DataModel> getImporter(
      PortableDataType type,
      AuthData authData,
      JobDataCache jobDataCache) throws IOException {
    Preconditions.checkArgument(SUPPORTED_DATA_TYPES.get(ServiceMode.IMPORT).contains(type),
        "Import of type [%s] is not supported.", type);
    switch (type) {
      case CALENDAR:
        return getCalendarService(authData);
      default:
        throw new IllegalArgumentException("Type [" + type + "] is not supported.");
    }
  }

  private MicrosoftCalendarService getCalendarService(AuthData authData) {
    MicrosoftOauthData msAuthData = (MicrosoftOauthData) authData;
    return new MicrosoftCalendarService(msAuthData.accessToken(), msAuthData.accountAddress());
  }

  private MicrosoftMailService getMailService(AuthData authData) {
    PasswordAuthData passwordAuthData = (PasswordAuthData) authData;
    return new MicrosoftMailService(passwordAuthData.username(), passwordAuthData.password());
  }

  private MicrosoftAuth getAndCreateMicrosoftAuth(PortableDataType dataType,
      ServiceMode serviceMode) {
    Preconditions.checkArgument(SUPPORTED_DATA_TYPES.get(serviceMode).contains(dataType),
        "[%s] of type [%s] is not supported by Microsoft", serviceMode, dataType);

    if (!DATA_TYPE_AUTHS.containsKey(dataType)) {
      DATA_TYPE_AUTHS.put(dataType, new HashMap<>());
    }

    Map<ServiceMode, MicrosoftAuth> serviceModeAuth = DATA_TYPE_AUTHS.get(dataType);
    if (!serviceModeAuth.containsKey(serviceMode)) {
      serviceModeAuth.put(serviceMode,
          new MicrosoftAuth(appCredentials, DATA_TYPE_SCOPES.get(dataType).get(serviceMode)));
    }

    return serviceModeAuth.get(serviceMode);
  }
}
