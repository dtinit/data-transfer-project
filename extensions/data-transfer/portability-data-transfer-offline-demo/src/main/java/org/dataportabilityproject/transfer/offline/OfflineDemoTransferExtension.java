package org.dataportabilityproject.transfer.offline;

import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.transfer.extension.TransferExtension;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.provider.Importer;

/**
 * Simulates importing offline data. For demo purposes only!
 *
 * <p>Microsoft offline data is used since that is the only form currently supported.
 */
public class OfflineDemoTransferExtension implements TransferExtension {
  private static final String SERVICE_ID = "offline-demo";

  @Override
  public String getServiceId() {
    return SERVICE_ID;
  }

  @Override
  public Exporter<?, ?> getExporter(String transferDataType) {
    return null;
  }

  @Override
  public Importer<?, ?> getImporter(String transferDataType) {
    return new OfflineDemoImporter();
  }

  @Override
  public void initialize(ExtensionContext context) {}
}
