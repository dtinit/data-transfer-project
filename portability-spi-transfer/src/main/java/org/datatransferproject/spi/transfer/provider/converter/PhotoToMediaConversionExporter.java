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
 * Generates a partial Media exporter using just the results of a Photo export.
 *
 * This is intended for providers who do not support video, but should be allowed to continue using
 * the common model of "Photo" without rewriting their DTP integration. In such cases this class
 * allows the provider to export Media objects with only the relevant photo fields populated and the
 * video fields ignored.
 */
public class PhotoToMediaConversionExporter<
      A extends AuthData,
      PCR extends PhotosContainerResource,
      PE extends Exporter<A, PCR>
    > implements Exporter<A, MediaContainerResource> {
  private final PE wrappedPhotoExporter;

  public PhotoToMediaConversionExporter(PE wrappedPhotoExporter) {
    this.wrappedPhotoExporter = wrappedPhotoExporter;
  }

  public ExportResult<MediaContainerResource> export(
      UUID jobId, A authData, Optional<ExportInformation> exportInformation) throws Exception {
    ExportResult<PCR> originalExportResult =
        wrappedPhotoExporter.export(jobId, authData, exportInformation);
    PCR photosContainer = originalExportResult.getExportedData();
    MediaContainerResource mediaContainerResource =
        MediaContainerResource.photoToMedia(photosContainer);
    return originalExportResult.copyWithContainerResource(mediaContainerResource);
  }
}
