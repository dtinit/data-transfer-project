package org.datatransferproject.spi.transfer.provider.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MediaImporterDecoratorTest {

  private MediaImporterDecorator<AuthData> mediaImporter;
  private List<MediaAlbum> albums;
  private List<PhotoModel> photos;
  private List<VideoModel> videos;
  private Importer<AuthData, PhotosContainerResource> photosImporter;
  private Importer<AuthData, VideosContainerResource> videosImporter;

  @BeforeEach
  public void setUp() {
    photosImporter = (id, ex, ad, data) -> ImportResult.OK;
    videosImporter = (id, ex, ad, data) -> ImportResult.OK;
    mediaImporter = new MediaImporterDecorator<>(photosImporter, videosImporter);

    albums = List.of(
        new MediaAlbum("album1", "name1", "desc1"),
        new MediaAlbum("album2", "name2", "desc2"),
        new MediaAlbum("album3", "name3", "desc3"));

    photos = List.of(
        new PhotoModel("p1", "", null, null, "d1", null, false, null, null),
        new PhotoModel("p2", "", null, null, "d2", "a1", false, null, null),
        new PhotoModel("p3", "", null, null, "d3", "a2", false, null, null));

    videos = List.of(
        new VideoModel("v1", "", null, null, "d4", null, false, null),
        new VideoModel("v2", "", null, null, "d5", "a3", false, null),
        new VideoModel("v3", "", null, null, "d6", null, false, null));
  }

  @Test
  public void shouldHandleVariousInputs() throws Exception {
    assertEquals(ImportResult.OK,
        mediaImporter.importItem(null, null, null, new MediaContainerResource(null, null, null)));
    assertEquals(ImportResult.OK,
        mediaImporter
            .importItem(null, null, null, new MediaContainerResource(albums, null, videos)));
    assertEquals(ImportResult.OK,
        mediaImporter
            .importItem(null, null, null, new MediaContainerResource(albums, photos, null)));
    assertEquals(ImportResult.OK,
        mediaImporter
            .importItem(null, null, null, new MediaContainerResource(albums, photos, videos)));
  }

  @Test
  public void shouldHandleErrorInPhotos() throws Exception {
    Exception throwable = new Exception();
    mediaImporter = new MediaImporterDecorator<>((id, ex, ad, data) -> new ImportResult(throwable),
        videosImporter);

    MediaContainerResource mcr = new MediaContainerResource(albums, photos, videos);
    ImportResult res = mediaImporter.importItem(null, null, null, mcr);
    assertEquals(new ImportResult(throwable), res);
  }


  @Test
  public void shouldHandleErrorInVideos() throws Exception {
    Exception throwable = new Exception();
    mediaImporter = new MediaImporterDecorator<>(photosImporter,
        (id, ex, ad, data) -> new ImportResult(throwable));

    MediaContainerResource mcr = new MediaContainerResource(albums, photos, videos);
    ImportResult res = mediaImporter.importItem(null, null, null, mcr);
    assertEquals(new ImportResult(throwable), res);
  }

}
