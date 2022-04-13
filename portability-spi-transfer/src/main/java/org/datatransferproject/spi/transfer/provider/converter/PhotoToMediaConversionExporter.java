package org.datatransferproject.spi.transfer.provider.converter;

import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.common.models.media.MediaContainerResource;

public class PhotoToMediaConversionExporter<E extends Exporter<A, PhotosContainerResource>>
    implements Exporter<A extends AuthData, MediaContainerResource> {

  private final E wrappedExporter;
  public PhotoToMediaConversionExporter(E wrappedExporter) {
    this.wrappedExporter = wrappedExporter;
  }

  ExportResult<MediaContainerResource> export(
      UUID jobId,
      A authData,
      Optional<ExportInformation> exportInformation,
      ) throws Exception {
    ExportResult<PhotosContainerResource> originalExportResult = mediaExporter.export(jobId, authData, exportInformation);
    PhotosContainerResource originalContainer = originalExportResult.getExportedData();
    MediaContainerResource photosContainerResource = PhotoToMediaConversionExporter.photoToMedia(originalContainer);
    return new ExportResult<>(
        originalExportResult.getType(),
        photosContainerResource,
        originalExportResult.getContinuationData());
  }

  // TODO(jzacsh) move this to the MediaAlbum class as a static method; this is just for proof of concept.
  // make sure not to merge PhotoAlbum#fromMediaAlbum(MediaAlbum) from this hackery:
  // https://github.com/jzacsh/data-transfer-project/compare/msoft-add-media-importer...jzacsh:techdebt-dedupe-media-vertical
  private static MediaContainerResource photoToMedia(PhotosContainerResource photosContainer) {
    return new MediaContainerResource(
      photosContainer.getAlbums().stream().map(a -> PhotoToMediaConversionExporter.photoToMediaAlbum(a)).collect(Collectors.toList()),
      photosContainer.getPhotos(),
      null /*videos*/
    );
  }

  // TODO(jzacsh) move this to the MediaAlbum class as a static method; this is just for proof of concept.
  // make sure not to merge PhotoAlbum#fromMediaAlbum(MediaAlbum) from this hackery:
  // https://github.com/jzacsh/data-transfer-project/compare/msoft-add-media-importer...jzacsh:techdebt-dedupe-media-vertical
  private static PhotosContainerResource photoToMedia(MediaContainerResource mediaContainer) {
    return new PhotosContainerResource(
      mediaContainer.getAlbums().stream().map(a -> PhotoToMediaConversionExporter.mediaToPhotoAlbum(a)).collect(Collectors.toList()),
      mediaContainer.getPhotos()
    );
  }

  /** Generates a partially empty MediaAlbum from a PhotoAlbum. */
  // TODO(jzacsh) move this to the MediaAlbum class as a static method; this is just for proof of concept.
  // make sure not to merge PhotoAlbum#fromMediaAlbum(MediaAlbum) from this hackery:
  // https://github.com/jzacsh/data-transfer-project/compare/msoft-add-media-importer...jzacsh:techdebt-dedupe-media-vertical
  private static MediaAlbum photoToMediaAlbum(PhotoAlbum photoAlbum) {
    return new MediaAlbum(photoAlbum.getId(), photoAlbum.getName(), photoAlbum.getDescription(), null /*videos*/);
  }

  /**
   * Extracts photos-specific data from a MediaAlbum and drops anything unsupported by PhotoAlbum.
   */
  // TODO(jzacsh) move this to the MediaAlbum class as a static method; this is just for proof of concept.
  // make sure not to merge PhotoAlbum#fromMediaAlbum(MediaAlbum) from this hackery:
  // https://github.com/jzacsh/data-transfer-project/compare/msoft-add-media-importer...jzacsh:techdebt-dedupe-media-vertical
  public static PhotoAlbum mediaToPhotoAlbum(MediaAlbum mediaAlbum) {
    return new PhotoAlbum(mediaAlbum.getId(), mediaAlbum.getName(), mediaAlbum.getDescription());
  }
}

