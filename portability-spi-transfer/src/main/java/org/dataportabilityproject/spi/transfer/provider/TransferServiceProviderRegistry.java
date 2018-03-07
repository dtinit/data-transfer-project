package org.dataportabilityproject.spi.transfer.provider;

import java.util.Set;

/**
 * Manages {@link TransferServiceProvider}s registered in the system. Provides client access methods
 * for Importers and Exporters.
 */
public interface TransferServiceProviderRegistry {

  /**
   * Returns the set of service ids that can transfered for the given {@code transferDataType}.
   *
   * @param transferDataType the transfer data type
   */
  Set<String> getServices(String transferDataType);

  /** Returns the set of data types that support both import and export. */
  Set<String> getTransferDataTypes();
}
