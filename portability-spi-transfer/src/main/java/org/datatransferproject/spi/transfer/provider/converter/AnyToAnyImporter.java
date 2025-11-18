package org.datatransferproject.spi.transfer.provider.converter;

import java.util.UUID;
import java.util.function.Function;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.models.ContainerResource;
import org.datatransferproject.types.transfer.auth.AuthData;

/**
 * Allows flexible bridging between adapters of different types with compatible functionality.
 *
 * ## Usage
 *
 * DTP transfers data by passing models from an Exporter (producing bytes)
 * to an {@link Importer} (consuming bytes). Models have a 1:1 correspondance to
 * {@link org.datatransferproject.types.common.models.DataVertical} (so a PHOTO
 * vertical is universally assumed in the DTP codebase to be used for
 * PhotosContainerResource transfers).
 *
 * ### Example Usage
 *
 * Given:
 *
 * - a DataVertical.PHOTO transfer
 * - a matching Exporter: an adapter producing {@link PhotosContainerResource} data
 * - a stand-in Importer: a good-enough adapter; something intended for a
 *   semi-compatible but not identical data type (eg: {@link MediaContainerResource}).
 * - a conversion function to go from said PHOTOS objects to DataVertical.MEDIA
 *   objects (eg: {@link MediaContainerResource} data).
 *
 * Then: one can create a synthetic Importer with this class by constructing with said conversion function.
 *
 * @param <StandinImporterType> Container type in which some extant exporter is
 *     providing data. eg: MediaContainerResource.
 * @param <ExportingType> The original container type which is being exported
 *     (that we will convert before arriving at our stand-in importer); eg:
 *     PhotosContainerResource.
 */
public class AnyToAnyImporter<
    AD extends AuthData,
    StandinImporterType extends ContainerResource,
    ExportingType extends ContainerResource> implements Importer<AD, ExportingType> {

  private final Importer<AD, StandinImporterType> standinImporter;
  private final Function<ExportingType, StandinImporterType> modelConverter;

  /**
   * @param standinImporter  existing importer
   * @param modelConverter function converting between some existing
   *    exporters' type to our stand-in importers' container types.
   */
  public AnyToAnyImporter(
      Importer<AD, StandinImporterType> standinImporter,
      Function<ExportingType, StandinImporterType> modelConverter
    ) {
    this.standinImporter = standinImporter;
    this.modelConverter = modelConverter;
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      AD authData,
      ExportingType data
  ) throws Exception {
    return standinImporter.importItem(jobId, idempotentExecutor, authData, modelConverter.apply(data));
  }
}
