package org.dataportabilityproject.spi.transfer;

/**
 * Manages {@link TransferServiceProvider}s registered in the system.
 */
public interface TransferServiceProviderRegistry {

    /**
     * Returns the provider that supports the service id.
     *
     * @param serviceId the service id
     */
    TransferServiceProvider getServiceProvider(String serviceId);

}
