package org.dataportabilityproject.transfer.derived;

import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.transfer.extension.TransferExtension;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.provider.Importer;

/** Simulates importing derived data. For demo purposes only! */
public class DerivedDemoTransferExtension implements TransferExtension {
  private static final String SERVICE_ID = "derived-demo";

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
    return new DerivedDemoImporter();
  }

  @Override
  public void initialize(ExtensionContext context) {}
}
