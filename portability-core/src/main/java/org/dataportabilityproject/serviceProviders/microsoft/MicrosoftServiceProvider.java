package org.dataportabilityproject.serviceProviders.microsoft;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.serviceProviders.microsoft.calendar.MicrosoftCalendarService;
import org.dataportabilityproject.serviceProviders.microsoft.mail.MicrosoftMailService;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.Secrets;
import org.dataportabilityproject.shared.ServiceProvider;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.OfflinePasswordAuthDataGenerator;
import org.dataportabilityproject.shared.auth.PasswordAuthData;

/**
 * The {@link ServiceProvider} for Microsoft (http://www.microsoft.com/).
 */
public final class MicrosoftServiceProvider implements ServiceProvider {
    private final MicrosoftAuth oauthProvider;
    private final OfflinePasswordAuthDataGenerator passwordAuth =
        new OfflinePasswordAuthDataGenerator();

    public MicrosoftServiceProvider(Secrets secrets) {
        oauthProvider = new MicrosoftAuth(secrets);
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
    public OfflineAuthDataGenerator getOfflineAuthDataGenerator(PortableDataType dataType) {
        switch (dataType) {
            case CALENDAR:
                return oauthProvider;
            case MAIL:
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
        return new MicrosoftCalendarService(msAuthData.token(), msAuthData.accountAddress());
    }

    private MicrosoftMailService getMailService(AuthData authData) {
        PasswordAuthData passwordAuthData = (PasswordAuthData) authData;
        return new MicrosoftMailService(passwordAuthData.username(), passwordAuthData.password());
    }
}
