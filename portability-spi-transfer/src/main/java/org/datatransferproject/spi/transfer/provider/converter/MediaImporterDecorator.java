package org.datatransferproject.spi.transfer.provider.converter;

import java.util.UUID;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.transfer.auth.AuthData;

/**
 *  Allows using existing Photo and Video adapters to create a Media adapter.
 */
public class MediaImporterDecorator<AD extends AuthData> implements
    Importer<AD, MediaContainerResource> {

  private final Importer<AD, PhotosContainerResource> photosImporter;
  private final Importer<AD, VideosContainerResource> videosImporter;

  public MediaImporterDecorator(Importer<AD, PhotosContainerResource> photosImporter,
      Importer<AD, VideosContainerResource> videosImporter) {
    this.photosImporter = photosImporter;
    this.videosImporter = videosImporter;
  }

  @Override
  public ImportResult importItem(UUID jobId, IdempotentImportExecutor idempotentExecutor,
      AD authData, MediaContainerResource data) throws Exception {
    PhotosContainerResource photosResource = MediaContainerResource.mediaToPhoto(data);
    ImportResult photosResult = photosImporter
        .importItem(jobId, idempotentExecutor, authData, photosResource);

    VideosContainerResource videosResource = MediaContainerResource.mediaToVideo(data);
    ImportResult videosResult = videosImporter
        .importItem(jobId, idempotentExecutor, authData, videosResource);

    return ImportResult.merge(photosResult, videosResult);
  }
}
