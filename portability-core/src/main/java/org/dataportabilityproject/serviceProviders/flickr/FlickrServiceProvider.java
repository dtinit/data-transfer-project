package org.dataportabilityproject.serviceProviders.flickr;

import com.flickr4java.flickr.FlickrException;
import com.google.common.collect.ImmutableList;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.jobDataCache.JobDataCacheImpl;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.Secrets;
import org.dataportabilityproject.shared.ServiceProvider;

import java.io.IOException;

/**
 * The {@link ServiceProvider} for the Flickr service (http://www.flickr.com/).
 */
public final class FlickrServiceProvider implements ServiceProvider {
    private FlickrPhotoService photoService;
    private final Secrets secrets;

    public FlickrServiceProvider(Secrets secrets) throws IOException, FlickrException {
        this.secrets = secrets;
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
            photoService = new FlickrPhotoService(
                    secrets.get("FLICKR_API_KEY"),
                    secrets.get("FLICKR_SECRET"),
                    secrets.get("FLICKR_NSID"),
                    new JobDataCacheImpl()
            );
        }
        return photoService;
    }
}
