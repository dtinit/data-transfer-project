package org.datatransferproject.spi.transfer.provider.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MediaExporterDecoratorTest {

  private MediaExporterDecorator<AuthData> mediaExporter;
  private List<MediaAlbum> albums;
  private List<PhotoModel> photos;
  private List<VideoModel> videos;
  private Exporter<AuthData, PhotosContainerResource> photosExporter;
  private Exporter<AuthData, VideosContainerResource> videosExporter;


  @BeforeEach
  public void setUp() {
    photosExporter = (jobId, authData, exportInformation) -> new ExportResult<>(ResultType.END,
        exportInformation.map(ei -> (PhotosContainerResource) ei.getContainerResource())
            .orElse(new PhotosContainerResource(null, null)));
    videosExporter = (jobId, authData, exportInformation) -> new ExportResult<>(ResultType.END,
        exportInformation.map(ei -> (VideosContainerResource) ei.getContainerResource())
            .orElse(new VideosContainerResource(null, null)));
    mediaExporter = new MediaExporterDecorator<>(photosExporter, videosExporter);

    albums = List.of(new MediaAlbum("album1", "name1", "desc1"),
        new MediaAlbum("album2", "name2", "desc2"),
        new MediaAlbum("album3", "name3", "desc3"));

    photos = List.of(
        new PhotoModel("p1", "", null, null, "d1", null,  false, null, null),
        new PhotoModel("p2", "", null, null, "d2", "a1", false, null, null),
        new PhotoModel("p3", "", null, null, "d3", "a2", false, null, null));

    videos = List.of(
        new VideoModel("v1", "", null, null, "d4", null, false, null),
        new VideoModel("v2", "", null, null, "d5", "a3", false, null),
        new VideoModel("v3", "", null, null, "d6", null, false, null));
  }

  @Test
  public void shouldHandleEmptyExportInfomationAndEmptyResults() throws Exception {
    ExportResult<MediaContainerResource> res = mediaExporter.export(null, null, Optional.empty());
    ExportResult<MediaContainerResource> exp = new ExportResult<>(
        ResultType.END, new MediaContainerResource(null, null, null));
    assertEquals(exp, res);
  }

  @Test
  public void shouldMergePhotoAndVideoResults() throws Exception {
    MediaContainerResource mcr = new MediaContainerResource(albums, photos, videos);
    ExportResult<MediaContainerResource> exp = new ExportResult<>(ResultType.END, mcr);

    Optional<ExportInformation> ei = Optional.of(new ExportInformation(null, mcr));
    ExportResult<MediaContainerResource> res = mediaExporter.export(null, null, ei);
    assertEquals(exp, res);
  }

  @Test
  public void shouldHandleOnlyPhotos() throws Exception {
    MediaContainerResource mcr = new MediaContainerResource(albums, photos, null);
    ExportResult<MediaContainerResource> exp = new ExportResult<>(ResultType.END, mcr);

    Optional<ExportInformation> ei = Optional.of(new ExportInformation(null, mcr));
    ExportResult<MediaContainerResource> res = mediaExporter.export(null, null, ei);
    assertEquals(exp, res);
  }

  @Test
  public void shouldHandleOnlyVideos() throws Exception {
    MediaContainerResource mcr = new MediaContainerResource(albums, null, videos);
    ExportResult<MediaContainerResource> exp = new ExportResult<>(ResultType.END, mcr);

    Optional<ExportInformation> ei = Optional.of(new ExportInformation(null, mcr));
    ExportResult<MediaContainerResource> res = mediaExporter.export(null, null, ei);
    assertEquals(exp, res);
  }

  @Test
  public void shouldHandleErrorInPhotos() throws Exception {
    Exception throwable = new Exception();
    mediaExporter =
        new MediaExporterDecorator<>((id, ad, ei) -> new ExportResult<>(throwable), videosExporter);

    MediaContainerResource mcr = new MediaContainerResource(albums, photos, videos);
    Optional<ExportInformation> ei = Optional.of(new ExportInformation(null, mcr));
    ExportResult<MediaContainerResource> res = mediaExporter.export(null, null, ei);
    assertEquals(new ExportResult<>(throwable), res);
  }


  @Test
  public void shouldHandleErrorInVideos() throws Exception {
    Exception throwable = new Exception();
    mediaExporter =
        new MediaExporterDecorator<>(photosExporter, (id, ad, ei) -> new ExportResult<>(throwable));

    MediaContainerResource mcr = new MediaContainerResource(albums, photos, videos);
    Optional<ExportInformation> ei = Optional.of(new ExportInformation(null, mcr));
    ExportResult<MediaContainerResource> res = mediaExporter.export(null, null, ei);
    assertEquals(new ExportResult<>(throwable), res);
  }
}
