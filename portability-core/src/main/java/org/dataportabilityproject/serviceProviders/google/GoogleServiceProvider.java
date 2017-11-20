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
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.serviceProviders.google.calendar.GoogleCalendarService;
import org.dataportabilityproject.serviceProviders.google.mail.GoogleMailService;
import org.dataportabilityproject.serviceProviders.google.piccasa.GooglePhotosService;
import org.dataportabilityproject.serviceProviders.google.tasks.GoogleTaskService;
import org.dataportabilityproject.shared.AppCredentials;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.ServiceProvider;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;

/**
 * The {@link ServiceProvider} for Google (http://www.google.com/).
 */
public final class GoogleServiceProvider implements ServiceProvider {
    private final static  List<String> SCOPES = Arrays.asList(
        TasksScopes.TASKS,
        "https://picasaweb.google.com/data/",
        CalendarScopes.CALENDAR,
        GmailScopes.GMAIL_READONLY,
        GmailScopes.GMAIL_MODIFY,
        GmailScopes.GMAIL_LABELS);

    private final GoogleAuth googleAuth;

    public GoogleServiceProvider() throws Exception {
        AppCredentials appCredentials =
            AppCredentials.createFromSecrets("GOOGLE_KEY", "GOOGLE_SECRET");
        this.googleAuth = new GoogleAuth(
                appCredentials,
                // TODO: only use scopes from the products we are accessing.
                SCOPES);
    }


    @Override public String getName() {
        return "Google";
    }

    @Override
    public ImmutableList<PortableDataType> getExportTypes() {
        return ImmutableList.of(CALENDAR, MAIL, PHOTOS, TASKS);
    }

    @Override
    public ImmutableList<PortableDataType> getImportTypes() {
        return ImmutableList.of(CALENDAR, MAIL, PHOTOS, TASKS);
    }

    @Override
    public OfflineAuthDataGenerator getOfflineAuthDataGenerator(PortableDataType dataType) {
        return googleAuth;
    }

    @Override
    public OnlineAuthDataGenerator getOnlineAuthDataGenerator(PortableDataType dataType) {
        return googleAuth;
    }

    @Override
    public Exporter<? extends DataModel> getExporter(
        PortableDataType type,
        AuthData authData,
        JobDataCache jobDataCache)
            throws IOException {
        Credential cred = googleAuth.getCredential(authData);
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
        Credential cred = googleAuth.getCredential(authData);
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
}
