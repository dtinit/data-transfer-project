package org.dataportabilityproject.transfer;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import org.dataportabilityproject.spi.transfer.provider.TransferServiceProviderRegistry;

/** Maintains the registry for valid TransferServiceProviders in the system */
public class PortabilityTransferServiceProviderRegistry implements TransferServiceProviderRegistry {

  private final ImmutableSet<String> supportedImportTypes;
  private final ImmutableSet<String> supportedExportTypes;

  // The parameters to the constructor are provided via dependency injection
  public PortabilityTransferServiceProviderRegistry(List<String> enabledServices) {
    // TODO(seehamrun): Populate these lists without loading up transfer code
    this.supportedExportTypes = ImmutableSet.<String>builder().build();
    this.supportedImportTypes = ImmutableSet.<String>builder().build();
  }

  /**
   * Returns the set of service ids that can transferred for the given {@code transferDataType}.
   *
   * @param transferDataType the transfer data type
   */
  @Override
  public Set<String> getServices(String transferDataType) {
    Preconditions.checkArgument(
        supportedExportTypes.contains(transferDataType),
        "TransferDataType [%s] is not valid for export",
        transferDataType);
    Preconditions.checkArgument(
        supportedImportTypes.contains(transferDataType),
        "TransferDataType [%s] is not valid for import",
        transferDataType);
    // TODO(seehamrun): Populate this list without the service provider map
    return null;
  }

  /** Returns the set of data types that support both import and export. */
  @Override
  public Set<String> getTransferDataTypes() {
    return Sets.intersection(supportedExportTypes, supportedImportTypes);
  }
}
