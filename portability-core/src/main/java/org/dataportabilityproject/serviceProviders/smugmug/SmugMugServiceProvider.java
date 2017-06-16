package org.dataportabilityproject.serviceProviders.smugmug;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import oauth.signpost.OAuthConsumer;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.jobDataCache.JobDataCacheImpl;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.Secrets;
import org.dataportabilityproject.shared.ServiceProvider;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;

/**
 * The {@link ServiceProvider} for the SmugMub service (https://www.smugmug.com/).
 */
public final class SmugMugServiceProvider implements ServiceProvider {
    private final SmugMugAuth auth;

    public SmugMugServiceProvider(Secrets secrets)
            throws IOException {
        this.auth = new SmugMugAuth(secrets.get("SMUGMUG_API_KEY"),
            secrets.get("SMUGMUG_SECRET"));
    }

    @Override public String getName() {
        return "SmugMug";
    }

    @Override public ImmutableList<PortableDataType> getExportTypes() {
        return ImmutableList.of(PortableDataType.PHOTOS);
    }

    @Override public ImmutableList<PortableDataType> getImportTypes() {
        return ImmutableList.of(PortableDataType.PHOTOS);
    }

    @Override
    public OfflineAuthDataGenerator getOfflineAuthDataGenerator(PortableDataType dataType) {
        return auth;
    }

    @Override public Exporter<? extends DataModel> getExporter(PortableDataType type,
            AuthData authData) throws IOException {
        if (type != PortableDataType.PHOTOS) {
            throw new IllegalArgumentException("Type " + type + " is not supported");
        }

        return getInstanceOfService(authData);
    }

    @Override public Importer<? extends DataModel> getImporter(PortableDataType type,
            AuthData authData) throws IOException {
        if (type != PortableDataType.PHOTOS) {
            throw new IllegalArgumentException("Type " + type + " is not supported");
        }

        return getInstanceOfService(authData);
    }
    private static SmugMugPhotoService hack;

    private synchronized SmugMugPhotoService getInstanceOfService(AuthData authData)
            throws IOException {
        if (hack == null) {
            OAuthConsumer consumer = auth.generateConsumer(authData);
            hack = new SmugMugPhotoService(
                consumer,
                new JobDataCacheImpl());
        }

        return hack;
    }
}
