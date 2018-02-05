package org.dataportabilityproject.spi.transfer.provider;

import org.dataportabilityproject.types.transfer.PortableType;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.models.DataModel;

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
    Exporter<AuthData, DataModel> getExporter(String serviceId, PortableType transferDataType);
    Importer<AuthData, DataModel> getImporter(String serviceId, PortableType transferDataType);


}
