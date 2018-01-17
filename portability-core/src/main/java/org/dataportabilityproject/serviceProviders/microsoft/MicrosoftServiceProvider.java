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
import com.google.inject.Inject;
import java.io.IOException;
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
import org.dataportabilityproject.shared.auth.PasswordAuthDataGenerator;
import org.dataportabilityproject.shared.auth.PasswordAuthData;

/**
 * The {@link ServiceProvider} for Microsoft (http://www.microsoft.com/).
 */
final class MicrosoftServiceProvider implements ServiceProvider {
    private static final ImmutableList<String> SCOPES = ImmutableList.of(
        "wl.imap", // outlook export via IMAP
        "wl.offline_access", // provides for refresh tokens
        "wl.calendars", "wl.contacts_calendars"); // calendar export

    private final MicrosoftAuth microsoftAuth;
    private final PasswordAuthDataGenerator  passwordAuth;

    @Inject
    MicrosoftServiceProvider(AppCredentialFactory appCredentialFactory) throws IOException {
        AppCredentials appCredentials =
            appCredentialFactory.lookupAndCreate("MICROSOFT_KEY", "MICROSOFT_SECRET");
        microsoftAuth = new MicrosoftAuth(
            appCredentials,
            // TODO: only use scopes from the products we are accessing.
            SCOPES);
        passwordAuth  = new PasswordAuthDataGenerator();
    }

    @Override public String getName() {
        return "Microsoft";
    }

    @Override
    public ImmutableList<PortableDataType> getExportTypes() {
        return ImmutableList.of(PortableDataType.CALENDAR, PortableDataType.MAIL);
    }

    @Override
    public ImmutableList<PortableDataType> getImportTypes() {
        return ImmutableList.of(PortableDataType.CALENDAR);
    }

    @Override
    public OfflineAuthDataGenerator getOfflineAuthDataGenerator(PortableDataType dataType, ServiceMode serviceMode) {
        switch (dataType) {
            case CALENDAR:
                return microsoftAuth;
            case MAIL:
                return passwordAuth;
            default:
                throw new IllegalArgumentException("Type " + dataType + " is not supported");
        }
    }

    @Override
    public OnlineAuthDataGenerator getOnlineAuthDataGenerator(PortableDataType dataType, ServiceMode serviceMode) {
        switch (dataType) {
            case CALENDAR:
                // IMPORT and EXPORT are supported for CALENDAR
                return microsoftAuth;
            case MAIL:
                // Only EXPORT is supported for MAIL
                Preconditions.checkArgument(serviceMode == ServiceMode.EXPORT, "IMPORT for MAIL is not supported by Microsoft.");
                return passwordAuth;
            default:
                throw new IllegalArgumentException("Type " + dataType + " is not supported");
        }
    }

    @Override
    public Exporter<? extends DataModel> getExporter(
        PortableDataType type,
        AuthData authData,
        JobDataCache jobDataCache) throws IOException {
        switch (type) {
            case CALENDAR:
                return getCalendarService(authData);
            case MAIL:
                return getMailService(authData);
            default:
                throw new IllegalArgumentException("Type " + type + " is not supported");
        }
    }

    @Override
    public Importer<? extends DataModel> getImporter(
        PortableDataType type,
        AuthData authData,
        JobDataCache jobDataCache) throws IOException {
        switch (type) {
            case CALENDAR:
                return getCalendarService(authData);
            default:
                throw new IllegalArgumentException("Type " + type + " is not supported");
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
}
