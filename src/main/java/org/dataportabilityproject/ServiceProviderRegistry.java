package org.dataportabilityproject;

import com.google.common.collect.ImmutableMap;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.serviceProviders.flickr.FlickrServiceProvider;
import org.dataportabilityproject.serviceProviders.google.GoogleServiceProvider;
import org.dataportabilityproject.serviceProviders.microsoft.MicrosoftServiceProvider;
import org.dataportabilityproject.serviceProviders.rememberTheMilk.RememberTheMilkProvider;
import org.dataportabilityproject.shared.IOInterface;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.Secrets;
import org.dataportabilityproject.shared.ServiceProvider;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A registry of all the supported {@link org.dataportabilityproject.shared.ServiceProvider}
 */
public class ServiceProviderRegistry {
    private final ImmutableMap<String, ServiceProvider> serviceProviders;

    public ServiceProviderRegistry(Secrets secrets, IOInterface ioInterface) throws Exception {
        ImmutableMap.Builder<String, ServiceProvider> providerBuilder = ImmutableMap.builder();

        addServiceProvider(new FlickrServiceProvider(secrets), providerBuilder);
        addServiceProvider(new GoogleServiceProvider(secrets), providerBuilder);
        addServiceProvider(new MicrosoftServiceProvider(secrets, ioInterface), providerBuilder);
        addServiceProvider(new RememberTheMilkProvider(secrets), providerBuilder);

        this.serviceProviders = providerBuilder.build();
    }

    public List<String> getServiceProvidersThatCanExport(PortableDataType portableDataType) {
        return serviceProviders.values().stream()
                .filter(sp -> sp.getExportTypes().stream().anyMatch(e -> e == portableDataType))
                .map(sp -> sp.getName())
                .collect(Collectors.toList());
    }

    public List<String> getServiceProvidersThatCanImport(PortableDataType portableDataType) {
        return serviceProviders.values().stream()
                .filter(sp -> sp.getImportTypes().stream().anyMatch(e -> e == portableDataType))
                .map(sp -> sp.getName())
                .collect(Collectors.toList());
    }

    public <T extends DataModel> Exporter<T> getExporter(
            String serviceProvider,
            PortableDataType portableDataType) throws IOException {
        Exporter<? extends DataModel> exporter = serviceProviders.get(serviceProvider).getExporter(portableDataType);
        return (Exporter<T>) exporter;
    }

    public <T extends DataModel> Importer<T> getImporter(
            String serviceProvider,
            PortableDataType portableDataType) throws IOException {
        Importer<? extends DataModel> importer = serviceProviders.get(serviceProvider).getImporter(portableDataType);
        return (Importer<T>) importer;
    }

    private static void addServiceProvider(ServiceProvider serviceProvider,
                                      ImmutableMap.Builder<String, ServiceProvider> builder) {
        builder.put(serviceProvider.getName(), serviceProvider);
    }
}
