package org.dataportabilityproject.serviceProviders.smugmug;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import oauth.signpost.OAuthConsumer;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.jobDataCache.JobDataCacheImpl;
import org.dataportabilityproject.shared.IOInterface;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.Secrets;
import org.dataportabilityproject.shared.ServiceProvider;
import org.dataportabilityproject.shared.auth.AuthData;

/**
 * The {@link ServiceProvider} for the SmugMub service (https://www.smugmug.com/).
 */
public final class SmugMugServiceProvider implements ServiceProvider {
    private final Secrets secrets;
    private final IOInterface consoleIO;

    public SmugMugServiceProvider(Secrets secrets, IOInterface consoleIO)
        throws IOException {
        this.secrets = secrets;
        this.consoleIO = consoleIO;
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

    @Override public Exporter<? extends DataModel> getExporter(PortableDataType type)
        throws IOException {
        if (type != PortableDataType.PHOTOS) {
            throw new IllegalArgumentException("Type " + type + " is not supported");
        }

        return getInstanceOfService();
    }

    @Override public Importer<? extends DataModel> getImporter(PortableDataType type)
        throws IOException {
        if (type != PortableDataType.PHOTOS) {
            throw new IllegalArgumentException("Type " + type + " is not supported");
        }

        return getInstanceOfService();
    }
    private static SmugMugPhotoService hack;

    private synchronized SmugMugPhotoService getInstanceOfService() throws IOException {
        if (hack == null) {
            SmugMugAuth auth = new SmugMugAuth(secrets.get("SMUGMUG_API_KEY"),
                secrets.get("SMUGMUG_SECRET"));
            AuthData authData = auth.generateAuthData(consoleIO);
            OAuthConsumer consumer = auth.generateConsumer(authData);
            hack = new SmugMugPhotoService(
                consumer,
                new JobDataCacheImpl());
        }

        return hack;
    }
}
