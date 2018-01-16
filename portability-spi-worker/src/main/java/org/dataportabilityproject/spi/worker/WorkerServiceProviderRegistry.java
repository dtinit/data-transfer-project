package org.dataportabilityproject.spi.worker;

/**
 * Manages {@link WorkerServiceProvider}s registered in the system.
 */
public interface WorkerServiceProviderRegistry {

    /**
     * Returns the provider that supports the service id.
     *
     * @param serviceId the service id
     */
    WorkerServiceProvider getServiceProvider(String serviceId);

}
