package org.dataportabilityproject.transfer;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.spi.transfer.provider.TransferServiceProvider;
import org.dataportabilityproject.spi.transfer.provider.TransferServiceProviderRegistry;

/** Maintains the registry for valid TransferServiceProviders in the system */
public class PortabilityTransferServiceProviderRegistry implements TransferServiceProviderRegistry {

  private final ImmutableSet<String> supportedImportTypes;
  private final ImmutableSet<String> supportedExportTypes;
  private final ImmutableMap<String, TransferServiceProvider> serviceProviderMap;

  // The parameters to the constructor are provided via dependency injection
  public PortabilityTransferServiceProviderRegistry(
      List<String> enabledServices, Map<String, TransferServiceProvider> serviceProviderMap) {
    ImmutableMap.Builder<String, TransferServiceProvider> serviceProviderBuilder =
        ImmutableMap.builder();
    ImmutableSet.Builder<String> supportedImportTypes = ImmutableSet.builder();
    ImmutableSet.Builder<String> supportedExportTypes = ImmutableSet.builder();

    for (String service : enabledServices) {
      TransferServiceProvider provider = serviceProviderMap.get(service);
      Preconditions.checkArgument(
          provider != null, "TransferServiceProvider not found for [%s]", service);

      List<String> importTypes = provider.getImportTypes();
      List<String> exportTypes = provider.getExportTypes();

      // Check that each registered service has export if it has import.
      // We do not allow for import only types
      for (String type : importTypes) {
        Preconditions.checkArgument(
            exportTypes.contains(type),
            "TransferDataType [%s] is available for import but not export in [%s] TransferServiceProvider",
            type,
            service);
        supportedImportTypes.add(type);
      }

      supportedExportTypes.addAll(exportTypes);
      serviceProviderBuilder.put(service, provider);
    }

    this.serviceProviderMap = serviceProviderBuilder.build();
    this.supportedExportTypes = supportedExportTypes.build();
    this.supportedImportTypes = supportedImportTypes.build();
  }

  /**
   * Returns the exporter that supports the serviceId and transferDataType.
   *
   * @param serviceId the service id
   * @param transferDataType the transfer data type
   */
  @Override
  public Exporter<?, ?> getExporter(String serviceId, String transferDataType) {
    Preconditions.checkArgument(
        supportedExportTypes.contains(transferDataType),
        "TransferDataType [%s] is not valid for export",
        transferDataType);
    TransferServiceProvider serviceProvider = serviceProviderMap.get(serviceId);
    Preconditions.checkArgument(serviceProvider != null);
    return serviceProvider.getExporter(transferDataType);
  }

  /**
   * Returns the exporter that supports the serviceId and transferDataType.
   *
   * @param serviceId the service id
   * @param transferDataType the transfer data type
   */
  @Override
  public Importer<?, ?> getImporter(String serviceId, String transferDataType) {
    Preconditions.checkArgument(
        supportedImportTypes.contains(transferDataType),
        "TransferDataType [%s] is not valid for import",
        transferDataType);
    TransferServiceProvider serviceProvider = serviceProviderMap.get(serviceId);
    Preconditions.checkArgument(serviceProvider != null);
    return serviceProvider.getImporter(transferDataType);
  }

  /**
   * Returns the set of service ids that can transfered for the given {@code transferDataType}.
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
    return serviceProviderMap.keySet();
  }

  /** Returns the set of data types that support both import and export. */
  @Override
  public Set<String> getTransferDataTypes() {
    return Sets.intersection(supportedExportTypes, supportedImportTypes);
  }
}
