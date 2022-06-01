package org.datatransferproject.spi.transfer.provider.converter;

import java.util.Optional;
import java.util.UUID;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

/**
 * Automatically produces a "MEDIA" importer from a "PHOTO" importer.
 *
 * This is intended for providers who do not support video, but should be allowed to continue using
 * the common model of "Photo" without rewriting their DTP integration. In such cases this class
 * allows the provider to export Media objects with only the relevant photo fields populated and
 * anything else (like the video fields) ignored.
 *
 * This is intended for providers who do not support "PHOTOS" as a special case.
 */
// TODO(#1065) fix primitives-obession causing us to key Providers on "PHOTOS" string rather
// than underlying file types.
public class PhotoToMediaConversionExporter<
    A extends AuthData,
    PCR extends PhotosContainerResource,
    MCR extends MediaContainerResource,
    WrappedExporter extends Exporter<A, PCR>
  > implements Exporter<A, MCR> {
  private final WrappedExporter wrappedPhotoExporter;

  public PhotoToMediaConversionExporter(WrappedExporter wrappedPhotoExporter) {
    this.wrappedPhotoExporter = wrappedPhotoExporter;
  }

  public ExportResult<MCR> export(
      UUID jobId,
      A authData,
      Optional<ExportInformation> exportInformation) throws Exception {
    ExportResult<PCR> originalExportResult =
        wrappedPhotoExporter.export(jobId, authData, exportInformation);
    PCR photosContainerResource = originalExportResult.getExportedData();
    MCR mediaContainerResource =
        MediaContainerResource.photoToMedia(photosContainerResource);
    return originalExportResult.copyWithExportedData(mediaContainerResource);
  }
}
