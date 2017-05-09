package org.dataportabilityproject.serviceProviders.google;

import static org.dataportabilityproject.shared.PortableDataType.CALENDAR;
import static org.dataportabilityproject.shared.PortableDataType.MAIL;
import static org.dataportabilityproject.shared.PortableDataType.PHOTOS;
import static org.dataportabilityproject.shared.PortableDataType.TASKS;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialRefreshListener;
import com.google.api.client.auth.oauth2.TokenErrorResponse;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.tasks.TasksScopes;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.jobDataCache.JobDataCacheImpl;
import org.dataportabilityproject.serviceProviders.google.calendar.GoogleCalendarService;
import org.dataportabilityproject.serviceProviders.google.mail.GoogleMailService;
import org.dataportabilityproject.serviceProviders.google.piccasa.GooglePhotosService;
import org.dataportabilityproject.serviceProviders.google.tasks.GoogleTaskService;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.Secrets;
import org.dataportabilityproject.shared.ServiceProvider;

/**
 * The {@link ServiceProvider} for Google (http://www.google.com/).
 */
public final class GoogleServiceProvider implements ServiceProvider {
    private final static  List<String> SCOPES = Arrays.asList(TasksScopes.TASKS,
            "https://picasaweb.google.com/data/",
            CalendarScopes.CALENDAR,
            GmailScopes.GMAIL_READONLY,
            GmailScopes.GMAIL_MODIFY,
            GmailScopes.GMAIL_LABELS);

    private final Supplier<Credential> creds;

    private final Supplier<GoogleCalendarService> calendarService;
    private final Supplier<GooglePhotosService> photoService;
    private final Supplier<GoogleTaskService> taskService;
    private final Supplier<GoogleMailService> mailService;

    public GoogleServiceProvider(Secrets secrets) throws Exception {
        final CredentialGenerator credentialGenerator = new CredentialGenerator(
                secrets.get("GOOGLE_CLIENT_ID"),
                secrets.get("GOOGLE_SECRET"));
        this.creds = Suppliers.memoize(() -> {
            try {
                Credential cred = credentialGenerator.authorize(SCOPES);
                if (!cred.refreshToken()) {
                    throw new IOException("Couldn't refresh token");
                }
                return cred;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });

        this.calendarService = Suppliers.memoize(() -> new GoogleCalendarService(
            creds.get(), new JobDataCacheImpl()));
        this.photoService = Suppliers.memoize(() -> new GooglePhotosService(
            creds.get(), new JobDataCacheImpl()));
        this.taskService = Suppliers.memoize(() -> new GoogleTaskService(
            creds.get(), new JobDataCacheImpl()));
        this.mailService = Suppliers.memoize(() -> new GoogleMailService(creds.get()));
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
    public Exporter<? extends DataModel> getExporter(PortableDataType type) throws IOException {
        switch (type) {
            case CALENDAR:
                return calendarService.get();
            case MAIL:
                return mailService.get();
            case PHOTOS:
                return photoService.get();
            case TASKS:
                return taskService.get();
            default:
                throw new IllegalArgumentException("Type " + type + " is not supported");
        }
    }

    @Override
    public Importer<? extends DataModel> getImporter(PortableDataType type) throws IOException {
        switch (type) {
            case CALENDAR:
                return calendarService.get();
            case MAIL:
                return mailService.get();
            case PHOTOS:
                return photoService.get();
            case TASKS:
                return taskService.get();
            default:
                throw new IllegalArgumentException("Type " + type + " is not supported");
        }
    }
}
