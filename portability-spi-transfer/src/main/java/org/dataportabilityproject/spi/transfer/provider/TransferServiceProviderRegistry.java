package org.dataportabilityproject.spi.transfer.provider;

import java.util.Set;
import org.dataportabilityproject.types.transfer.PortableType;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.models.DataModel;

/**
 * Manages {@link TransferServiceProvider}s registered in the system.
 * Provides client access methods for Importers and Exporters.
 */
public interface TransferServiceProviderRegistry {

  /**
   * Returns the exporter that supports the serviceId and transferDataType.
   *
   * @param serviceId the service id
   * @param transferDataType the transfer data type
   */
  Exporter<?, ?> getExporter(String serviceId, String transferDataType);

  /**
   * Returns the exporter that supports the serviceId and transferDataType.
   *
   * @param serviceId the service id
   * @param transferDataType the transfer data type
   */
  Importer<?, ?> getImporter(String serviceId, String transferDataType);

  /**
   * Returns the set of service ids that can transfered for the given {@code transferDataType}.
   *
   * @param transferDataType the transfer data type
   */
  Set<String> getServices(String transferDataType);

  /**
   * Returns the set of data types that support both import and export.
   */
  Set<String> getTransferDataTypes();
}
