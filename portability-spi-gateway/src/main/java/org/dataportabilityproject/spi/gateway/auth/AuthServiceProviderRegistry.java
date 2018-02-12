package org.dataportabilityproject.spi.gateway.auth;

/**
 * Manages {@link AuthServiceProvider}s registered in the system.
 */
public interface AuthServiceProviderRegistry {

    /**
     * Returns the provider that supports the service id.
     *
     * @param serviceId the service id
     */
    AuthServiceProvider getServiceProvider(String serviceId);
  }
