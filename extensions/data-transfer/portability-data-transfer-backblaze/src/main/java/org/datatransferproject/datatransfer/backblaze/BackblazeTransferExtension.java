package org.datatransferproject.datatransfer.backblaze;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.backblaze.photos.BackblazePhotosImporter;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;

public class BackblazeTransferExtension implements TransferExtension {
  public static final String SERVICE_ID = "Backblaze";
  private static final List<String> SUPPORTED_TYPES = ImmutableList.of("PHOTOS");

  private BackblazePhotosImporter importer;
  private boolean initialized = false;

  @Override
  public String getServiceId() {
    return SERVICE_ID;
  }

  @Override
  public Exporter<?, ?> getExporter(String transferDataType) {
    throw new IllegalArgumentException();
  }

  @Override
  public Importer<?, ?> getImporter(String transferDataType) {
    Preconditions.checkArgument(
        initialized, "Trying to call getImporter before initalizing BackblazeTransferExtension");
    Preconditions.checkArgument(
        SUPPORTED_TYPES.contains(transferDataType),
        "Import of " + transferDataType + " not supported by Backblaze");
    return importer;
  }

  @Override
  public void initialize(ExtensionContext context) {
    Monitor monitor = context.getMonitor();
    monitor.debug(() -> "Starting Backblaze initialization");
    if (initialized) {
      monitor.severe(() -> "BackblazeTransferExtension already initialized.");
      return;
    }

    TemporaryPerJobDataStore jobStore = context.getService(TemporaryPerJobDataStore.class);

    importer = new BackblazePhotosImporter(monitor, jobStore);
    initialized = true;
  }
}
