package org.datatransferproject.spi.transfer.provider.converter;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.models.ContainerResource;
import org.datatransferproject.types.transfer.auth.AuthData;

/**
 * Allows flexible bridging between adapters of different types with compatible functionality.
 *
 * @param <From> The container type supported by the available exporter.
 * @param <To>   The container type that's desired.
 */
public class AnyToAnyExporter<
    AD extends AuthData,
    From extends ContainerResource,
    To extends ContainerResource> implements Exporter<AD, To> {

  private final Exporter<AD, From> exporter;
  private final Function<From, To> containerResourceConverter;
  private final Function<ContainerResource, ContainerResource> exportInformationConverter;

  /**
   * @param exporter                   existing exporter
   * @param containerResourceConverter function converting between the existing and desired
   *                                   containers.
   * @param exportInformationConverter converter that's used to adapt the export information that
   *                                   goes into the export call. It's required because exporters
   *                                   often support various container resources as part of export
   *                                   information. E.g. photo exporters support
   *                                   DateRangeContainerResource, while media adapters might only
   *                                   support MediaContainerResource.
   */
  public AnyToAnyExporter(Exporter<AD, From> exporter,
      Function<From, To> containerResourceConverter,
      Function<ContainerResource, ContainerResource> exportInformationConverter) {
    this.exporter = exporter;
    this.containerResourceConverter = containerResourceConverter;
    this.exportInformationConverter = exportInformationConverter;
  }

  /**
   * @param exporter                   existing exporter
   * @param containerResourceConverter function converting between the existing and desired
   *                                   containers.
   */
  public AnyToAnyExporter(Exporter<AD, From> exporter,
      Function<From, To> containerResourceConverter) {
    this(exporter, containerResourceConverter, Function.identity());
  }

  @Override
  public ExportResult<To> export(UUID jobId, AD authData, Optional<ExportInformation> exportInfo)
      throws Exception {
    Optional<ExportInformation> infoWithConvertedResource =
        exportInfo.map(
            (ei) ->
                ei.copyWithResource(exportInformationConverter.apply(ei.getContainerResource())));
    ExportResult<From> originalResult = exporter.export(jobId, authData, infoWithConvertedResource);
    return originalResult.copyWithExportedData(
        containerResourceConverter.apply(originalResult.getExportedData()));
  }
}
