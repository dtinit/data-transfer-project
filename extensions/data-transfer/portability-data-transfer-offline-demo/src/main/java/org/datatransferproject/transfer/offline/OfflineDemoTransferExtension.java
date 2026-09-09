package org.datatransferproject.transfer.offline;

import static org.datatransferproject.types.common.models.DataVertical.OFFLINE_DATA;

import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;

/**
 * Simulates transferring offline data. For demo purposes only!
 *
 * <p>Both sides are credential-free, so this is the one extension pair that can run a complete
 * transfer without provider API keys.
 */
public class OfflineDemoTransferExtension implements TransferExtension {
  private static final String SERVICE_ID = "offline-demo";

  @Override
  public String getServiceId() {
    return SERVICE_ID;
  }

  @Override
  public Exporter<?, ?> getExporter(DataVertical transferDataType) {
    return OFFLINE_DATA.equals(transferDataType) ? new OfflineDemoExporter() : null;
  }

  @Override
  public Importer<?, ?> getImporter(DataVertical transferDataType) {
    return OFFLINE_DATA.equals(transferDataType) ? new OfflineDemoImporter() : null;
  }

  @Override
  public void initialize(ExtensionContext context) {}
}
