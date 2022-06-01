package org.datatransferproject.spi.transfer.provider.converter;

import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.util.Optional;
import java.util.UUID;

/**
 * Automatically produces a "PHOTO" importer from a more sophisticated "MEDIA" importer.
 *
 * Produces an `Importer` that can handle import jobs for `PhotosContainerResource` (aka "PHOTOS"
 * jobs) based on an existing importer that can handle the broader jobs for `MediaContainerResource`
 * (aka "MEDIA" jobs).
 *
 * This is intended for providers who do not support "MEDIA" as a special case.
 */
// TODO(#1065) fix primitives-obession causing us to key Providers on "PHOTOS" string rather
// than underlying file types.
public class MediaToPhotoConversionImporter<
    A extends AuthData,
    PCR extends PhotosContainerResource,
    MCR extends MediaContainerResource,
    WrappedImporter extends Importer<A, MCR>
  > implements Importer<A, PCR> {
  private final WrappedImporter wrappedMediaImporter;

  public MediaToPhotoConversionImporter(WrappedImporter wrappedMediaImporter) {
    this.wrappedMediaImporter = wrappedMediaImporter;
  }

  public ExportResult<PCR> export(
      UUID jobId, A authData, Optional<ExportInformation> exportInformation) throws Exception {
    ExportResult<MCR> originalExportResult =
        wrappedMediaImporter.export(jobId, authData, exportInformation);
    MCR mediaContainerResource = originalExportResult.getExportedData();
    PCR photosContainerResource = MediaContainerResource.mediaToPhoto(mediaContainerResource);
    return originalExportResult.copyWithExportedData(photosContainerResource);
  }
}
