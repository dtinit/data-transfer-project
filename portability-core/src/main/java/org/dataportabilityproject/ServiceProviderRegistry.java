package org.dataportabilityproject;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.serviceProviders.flickr.FlickrServiceProvider;
import org.dataportabilityproject.serviceProviders.google.GoogleServiceProvider;
import org.dataportabilityproject.serviceProviders.instagram.InstagramServiceProvider;
import org.dataportabilityproject.serviceProviders.microsoft.MicrosoftServiceProvider;
import org.dataportabilityproject.serviceProviders.rememberTheMilk.RememberTheMilkProvider;
import org.dataportabilityproject.serviceProviders.smugmug.SmugMugServiceProvider;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.Secrets;
import org.dataportabilityproject.shared.ServiceProvider;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;

/**
 * A registry of all the supported {@link org.dataportabilityproject.shared.ServiceProvider}
 */
public class ServiceProviderRegistry {
    private final ImmutableMap<String, ServiceProvider> serviceProviders;


    public ServiceProviderRegistry(Secrets secrets, CloudFactory cloudFactory) throws Exception {
        JobDataCache jobDataCache = cloudFactory.getJobDataCache();
        ImmutableMap.Builder<String, ServiceProvider> providerBuilder = ImmutableMap.builder();
        addServiceProvider(new FlickrServiceProvider(secrets, jobDataCache), providerBuilder);
        addServiceProvider(new GoogleServiceProvider(secrets, jobDataCache), providerBuilder);
        addServiceProvider(new MicrosoftServiceProvider(secrets), providerBuilder);
        addServiceProvider(new RememberTheMilkProvider(secrets, jobDataCache), providerBuilder);
        addServiceProvider(new InstagramServiceProvider(secrets), providerBuilder);
        addServiceProvider(new SmugMugServiceProvider(secrets, jobDataCache), providerBuilder);

        this.serviceProviders = providerBuilder.build();
    }

    public List<String> getServiceProvidersThatCanExport(PortableDataType portableDataType) {
        return serviceProviders.values().stream()
                .filter(sp -> sp.getExportTypes().stream().anyMatch(e -> e == portableDataType))
                .map(ServiceProvider::getName)
                .collect(Collectors.toList());
    }

    public List<String> getServiceProvidersThatCanImport(PortableDataType portableDataType) {
        return serviceProviders.values().stream()
                .filter(sp -> sp.getImportTypes().stream().anyMatch(e -> e == portableDataType))
                .map(ServiceProvider::getName)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public <T extends DataModel> Exporter<T> getExporter(
            String serviceProvider,
            PortableDataType portableDataType,
            AuthData authData) throws IOException {
        Exporter<? extends DataModel> exporter = serviceProviders.get(serviceProvider)
            .getExporter(portableDataType, authData);
        return (Exporter<T>) exporter;
    }

    @SuppressWarnings("unchecked")
    public <T extends DataModel> Importer<T> getImporter(
            String serviceProvider,
            PortableDataType portableDataType,
            AuthData authData) throws IOException {
        Importer<? extends DataModel> importer = serviceProviders.get(serviceProvider)
            .getImporter(portableDataType, authData);
        return (Importer<T>) importer;
    }

    public OnlineAuthDataGenerator getOnlineAuth(String serviceProvider,
        PortableDataType dataType) {
        return serviceProviders.get(serviceProvider).getOnlineAuthDataGenerator(dataType);
    }

    public OfflineAuthDataGenerator getOfflineAuth(String serviceProvider,
            PortableDataType dataType) {
        return serviceProviders.get(serviceProvider).getOfflineAuthDataGenerator(dataType);
    }

    private static void addServiceProvider(ServiceProvider serviceProvider,
                                      ImmutableMap.Builder<String, ServiceProvider> builder) {
        builder.put(serviceProvider.getName(), serviceProvider);
    }
}
