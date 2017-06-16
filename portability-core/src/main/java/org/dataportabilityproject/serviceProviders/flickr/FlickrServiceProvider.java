package org.dataportabilityproject.serviceProviders.flickr;

import com.flickr4java.flickr.auth.Auth;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
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
 * The {@link ServiceProvider} for the Flickr service (http://www.flickr.com/).
 */
public final class FlickrServiceProvider implements ServiceProvider {
    private FlickrPhotoService photoService;
    private final Secrets secrets;
    private final FlickrAuth authProvider;

    public FlickrServiceProvider(Secrets secrets) {
        this.secrets = secrets;
        this.authProvider = new FlickrAuth(
            secrets.get("FLICKR_API_KEY"),
            secrets.get("FLICKR_SECRET"));
    }

    @Override public String getName() {
        return "Flickr";
    }

    @Override public ImmutableList<PortableDataType> getExportTypes() {
        return ImmutableList.of(PortableDataType.PHOTOS);
    }

    @Override public ImmutableList<PortableDataType> getImportTypes() {
        return ImmutableList.of(PortableDataType.PHOTOS);
    }

    @Override
    public OfflineAuthDataGenerator getOfflineAuthDataGenerator(PortableDataType dataType) {
        return authProvider;
    }

    @Override public Exporter<? extends DataModel> getExporter(
        PortableDataType type,
        AuthData authData) throws IOException {
        if (type != PortableDataType.PHOTOS) {
            throw new IllegalArgumentException("Type " + type + " is not supported");
        }

        return getInstanceOfService(authData);
    }

    @Override public Importer<? extends DataModel> getImporter(
        PortableDataType type,
        AuthData authData) throws IOException {
        if (type != PortableDataType.PHOTOS) {
            throw new IllegalArgumentException("Type " + type + " is not supported");
        }

        return getInstanceOfService(authData);
    }

    private synchronized FlickrPhotoService getInstanceOfService(AuthData authData)
        throws IOException {
        if (null == photoService) {
            Auth auth = authProvider.getAuth(authData);
            photoService = new FlickrPhotoService(
                    secrets.get("FLICKR_API_KEY"),
                    secrets.get("FLICKR_SECRET"),
                    auth,
                    new JobDataCacheImpl()
            );
        }
        return photoService;
    }
}
