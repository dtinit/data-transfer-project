package org.datatransferproject.spi.transfer.provider.converter;

import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class PhotoToMediaConversionExporter<E extends Exporter<A, PhotosContainerResource>>
    implements Exporter<A extends AuthData, MediaContainerResource> {
  private final E wrappedExporter;
  public PhotoToMediaConversionExporter(E wrappedExporter) {
    this.wrappedExporter = wrappedExporter;
  }

  ExportResult<MediaContainerResource> export(
      UUID jobId, A authData, Optional<ExportInformation> exportInformation, ) throws Exception {
    ExportResult<PhotosContainerResource> originalExportResult =
        mediaExporter.export(jobId, authData, exportInformation);
    PhotosContainerResource photosContainer = originalExportResult.getExportedData();
    MediaContainerResource mediaContainerResource =
        MediaContainerResource.photoToMedia(photosContainer);
    return new ExportResult<>(originalExportResult.getType(), mediaContainerResource,
        originalExportResult.getContinuationData());
  }
}
