package org.dataportabilityproject.spi.datatransfer;

import org.dataportabilityproject.client.types.providers.RegisteredServiceProvider;

import java.util.List;

/**
 * Tracks service provider importer and exporter extensions registered in the system.
 */
public interface TransferRegistry {

    /**
     * Returns the registered service providers.
     */
    List<RegisteredServiceProvider> getRegisteredServiceProviders();

    /**
     * Registers a service provider.
     *
     * @param provider the provider to register
     */
    void registerServiceProvider(RegisteredServiceProvider provider);

}
