package org.dataportabilityproject.spi.transfer.provider;

import java.util.List;

/**
 * Factory responsible for providing {@link Exporter} and {@link Importer} implementations that
 * transfer data from and to a particular service.
 */
public interface TransferServiceProvider {

  /** Returns the id of the service this factory supports. */
  String getServiceId();

  /**
   * Provides an exporter for the given transfer data type
   *
   * @param transferDataType the transfer data tyoe
   */
  Exporter<?, ?> getExporter(String transferDataType);

  /**
   * Provides an importer for the given transfer data type
   *
   * @param transferDataType the transfer data tyoe
   */
  Importer<?, ?> getImporter(String transferDataType);

  /**
   * Provides the list of valid import types for the TransferServiceProvider
   *
   * @return List of String representing the import types
   */
  List<String> getImportTypes();

  /**
   * Provides the list of valid export types for the TransferServiceProvider
   *
   * @return List of String representing the export types
   */
  List<String> getExportTypes();
}
