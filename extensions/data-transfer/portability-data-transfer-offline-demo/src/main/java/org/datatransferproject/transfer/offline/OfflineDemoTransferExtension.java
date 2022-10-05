package org.datatransferproject.transfer.offline;

import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;

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
  public Exporter<?, ?> getExporter(DataVertical transferDataType) {
    return null;
  }

  @Override
  public Importer<?, ?> getImporter(DataVertical transferDataType) {
    return new OfflineDemoImporter();
  }

  @Override
  public void initialize(ExtensionContext context) {}
}
