package org.dataportabilityproject.serviceProviders.flickr;

import com.flickr4java.flickr.auth.Auth;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
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
 * The {@link ServiceProvider} for the Flickr service (http://www.flickr.com/).
 */
public final class FlickrServiceProvider implements ServiceProvider {
    private FlickrPhotoService photoService;
    private final Secrets secrets;
    private final IOInterface ioInterface;

    public FlickrServiceProvider(Secrets secrets, IOInterface ioInterface) {
        this.secrets = secrets;
        this.ioInterface = ioInterface;
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

    @Override public Exporter<? extends DataModel> getExporter(PortableDataType type) throws IOException {
        if (type != PortableDataType.PHOTOS) {
            throw new IllegalArgumentException("Type " + type + " is not supported");
        }

        return getInstanceOfService();
    }

    @Override public Importer<? extends DataModel> getImporter(PortableDataType type) throws IOException {
        if (type != PortableDataType.PHOTOS) {
            throw new IllegalArgumentException("Type " + type + " is not supported");
        }

        return getInstanceOfService();
    }

    private synchronized FlickrPhotoService getInstanceOfService() throws IOException {
        if (null == photoService) {
            FlickrAuth authProvider = new FlickrAuth(
                secrets.get("FLICKR_API_KEY"),
                secrets.get("FLICKR_SECRET"));
            AuthData authData = authProvider.generateAuthData(ioInterface);
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
