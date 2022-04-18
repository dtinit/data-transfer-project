package org.datatransferproject.spi.transfer.provider.converter;

import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.util.Optional;
import java.util.UUID;

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
      UUID jobId,
      A authData,
      Optional<ExportInformation> exportInformation
  ) throws Exception {
    ExportResult<PCR> originalExportResult =
        wrappedPhotoExporter.export(jobId, authData, exportInformation);
    PCR photosContainer = originalExportResult.getExportedData();
    MediaContainerResource mediaContainerResource =
        MediaContainerResource.photoToMedia(photosContainer);
    return new ExportResult<>(originalExportResult.getType(), mediaContainerResource,
        originalExportResult.getContinuationData());
  }
}
