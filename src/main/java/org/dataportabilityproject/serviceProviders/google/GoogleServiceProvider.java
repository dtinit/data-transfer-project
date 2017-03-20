package org.dataportabilityproject.serviceProviders.google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.tasks.TasksScopes;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.serviceProviders.google.calendar.GoogleCalendarService;
import org.dataportabilityproject.serviceProviders.google.piccasa.GooglePhotosService;
import org.dataportabilityproject.serviceProviders.google.tasks.GoogleTaskService;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.Secrets;
import org.dataportabilityproject.shared.ServiceProvider;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.dataportabilityproject.shared.PortableDataType.CALENDAR;
import static org.dataportabilityproject.shared.PortableDataType.PHOTOS;
import static org.dataportabilityproject.shared.PortableDataType.TASKS;

/**
 * The {@link ServiceProvider} for Google (http://www.google.com/).
 */
public final class GoogleServiceProvider implements ServiceProvider {
    private final static  List<String> SCOPES = Arrays.asList(TasksScopes.TASKS,
            //"https://picasaweb.serviceProviders.google.com/data/",
            CalendarScopes.CALENDAR);

    private final Supplier<Credential> creds;

    private final Supplier<GoogleCalendarService> calendarService;
    private final Supplier<GooglePhotosService> photoService;
    private final Supplier<GoogleTaskService> taskService;

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

        this.calendarService = Suppliers.memoize(() -> new GoogleCalendarService(creds.get()));
        this.photoService = Suppliers.memoize(() -> new GooglePhotosService(creds.get()));
        this.taskService = Suppliers.memoize(() -> new GoogleTaskService(creds.get()));
    }


    @Override public String getName() {
        return "Google";
    }

    @Override
    public ImmutableList<PortableDataType> getExportTypes() {
        return ImmutableList.of(TASKS, PHOTOS, CALENDAR);
    }

    @Override
    public ImmutableList<PortableDataType> getImportTypes() {
        return ImmutableList.of(TASKS, PHOTOS, CALENDAR);
    }

    @Override
    public Exporter<? extends DataModel> getExporter(PortableDataType type) throws IOException {
        switch (type) {
            case PHOTOS:
                return photoService.get();
            case CALENDAR:
                return calendarService.get();
            case TASKS:
                return taskService.get();
            default:
                throw new IllegalArgumentException("Type " + type + " is not supported");
        }
    }

    @Override
    public Importer<? extends DataModel> getImporter(PortableDataType type) throws IOException {
        switch (type) {
            case PHOTOS:
                return photoService.get();
            case CALENDAR:
                return calendarService.get();
            case TASKS:
                return taskService.get();
            default:
                throw new IllegalArgumentException("Type " + type + " is not supported");
        }
    }
}
