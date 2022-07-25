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
 * @param <From> The container type supported by the available exporter.
 * @param <To>   The container type that's desired.
 */
public class AnyToAnyImporter<
    AD extends AuthData,
    From extends ContainerResource,
    To extends ContainerResource> implements Importer<AD, To> {

  private final Importer<AD, From> importer;
  private final Function<To, From> converter;

  /**
   * @param importer  existing importer
   * @param converter function converting between the existing and desired containers.
   */
  public AnyToAnyImporter(Importer<AD, From> importer, Function<To, From> converter) {
    this.importer = importer;
    this.converter = converter;
  }

  @Override
  public ImportResult importItem(UUID jobId, IdempotentImportExecutor idempotentExecutor,
      AD authData, To data) throws Exception {
    return importer.importItem(jobId, idempotentExecutor, authData, converter.apply(data));
  }
}
