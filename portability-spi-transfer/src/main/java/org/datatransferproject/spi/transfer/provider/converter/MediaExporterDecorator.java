package org.datatransferproject.spi.transfer.provider.converter;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.models.ContainerResource;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.common.models.videos.VideoAlbum;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.transfer.auth.AuthData;

/**
 *  Allows using existing Photo and Video adapters to create a Media adapter.
 */
public class MediaExporterDecorator<AD extends AuthData> implements
    Exporter<AD, MediaContainerResource> {

  private final Exporter<AD, PhotosContainerResource> photosExporter;
  private final Exporter<AD, VideosContainerResource> videosExporter;

  public MediaExporterDecorator(Exporter<AD, PhotosContainerResource> photosExporter,
      Exporter<AD, VideosContainerResource> videosExporter) {
    this.photosExporter = photosExporter;
    this.videosExporter = videosExporter;
  }

  @Override
  public ExportResult<MediaContainerResource> export(UUID jobId, AD authData,
      Optional<ExportInformation> exportInfo) throws Exception {
    ExportResult<PhotosContainerResource> per = exportPhotos(jobId, authData, exportInfo);
    if (per.getThrowable().isPresent()) {
      return new ExportResult<>(per.getThrowable().get());
    }

    ExportResult<VideosContainerResource> ver = exportVideos(jobId, authData, exportInfo);
    if (ver.getThrowable().isPresent()) {
      return new ExportResult<>(ver.getThrowable().get());
    }
    return mergeResults(per, ver);
  }

  private ExportResult<MediaContainerResource> mergeResults(
      ExportResult<PhotosContainerResource> photoResult,
      ExportResult<VideosContainerResource> videoResult) {
    ResultType resultType = ResultType.merge(photoResult.getType(), videoResult.getType());

    Collection<MediaAlbum> albums = mergeAlbums(
        photoResult.getExportedData().getAlbums(),
        videoResult.getExportedData().getAlbums());

    MediaContainerResource exportData = new MediaContainerResource(albums,
        photoResult.getExportedData().getPhotos(),
        videoResult.getExportedData().getVideos());

    ContinuationData cd =
        mergeContinuationData(photoResult.getContinuationData(), videoResult.getContinuationData());

    return new ExportResult<>(resultType, exportData, cd);
  }

  private ExportResult<VideosContainerResource> exportVideos(UUID jobId, AD authData,
      Optional<ExportInformation> exportInfo) throws Exception {
    return videosExporter.export(jobId, authData, exportInfo.map((ei) -> {
      ContainerResource cr = ei.getContainerResource();
      if (cr instanceof MediaContainerResource) {
        cr = MediaContainerResource.mediaToVideo((MediaContainerResource) cr);
      }
      return ei.copyWithResource(cr);
    }));
  }

  private ExportResult<PhotosContainerResource> exportPhotos(UUID jobId, AD authData,
      Optional<ExportInformation> exportInfo)
      throws Exception {
    return photosExporter.export(jobId, authData, exportInfo.map((ei) -> {
      ContainerResource cr = ei.getContainerResource();
      if (cr instanceof MediaContainerResource) {
        cr = MediaContainerResource.mediaToPhoto((MediaContainerResource) cr);
      }
      return ei.copyWithResource(cr);
    }));
  }

  private ContinuationData mergeContinuationData(ContinuationData cd1, ContinuationData cd2) {
    if (cd1 == null) {
      return cd2;
    }
    if (cd2 == null) {
      return cd1;
    }
    ContinuationData res = new ContinuationData(cd1.getPaginationData());
    res.addContainerResources(cd1.getContainerResources());
    res.addContainerResources(cd2.getContainerResources());
    return res;
  }

  private Collection<MediaAlbum> mergeAlbums(Collection<PhotoAlbum> a1,
      Collection<VideoAlbum> a2) {
    return Stream.concat(
        a1.stream().map(MediaAlbum::photoToMediaAlbum),
        a2.stream().map(MediaAlbum::videoToMediaAlbum)
    ).distinct().collect(Collectors.toList());
  }
}
