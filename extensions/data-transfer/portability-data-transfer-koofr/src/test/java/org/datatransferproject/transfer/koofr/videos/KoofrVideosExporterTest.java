package org.datatransferproject.transfer.koofr.videos;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.transfer.koofr.common.Fixtures;
import org.datatransferproject.transfer.koofr.common.KoofrClient;
import org.datatransferproject.transfer.koofr.common.KoofrClientFactory;
import org.datatransferproject.types.common.models.videos.VideoAlbum;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class KoofrVideosExporterTest {

  @Mock
  private KoofrClientFactory clientFactory;
  @Mock
  private KoofrClient client;
  @Mock
  private Monitor monitor;
  private KoofrVideosExporter exporter;
  private TokensAndUrlAuthData authData;

  @BeforeEach
  public void setUp() throws Exception {
    when(clientFactory.create(any())).thenReturn(client);
    exporter = new KoofrVideosExporter(clientFactory, monitor);
    authData = new TokensAndUrlAuthData("acc", "refresh", "");
  }

  @Test
  public void testExport() throws Exception {
    when(client.getRootPath()).thenReturn("/Data transfer");
    when(client.listRecursive("/Data transfer")).thenReturn(Fixtures.listRecursiveItems);
    when(client.fileLink("/Data transfer/Album 2 :heart:/Video 1.mp4"))
        .thenReturn("https://app-1.koofr.net/content/files/get/Video+1.mp4?base=TESTBASE");
    when(client.fileLink("/Data transfer/Videos/Video 2.mp4"))
        .thenReturn("https://app-1.koofr.net/content/files/get/Video+2.mp4?base=TESTBASE");

    UUID jobId = UUID.randomUUID();

    ExportResult<VideosContainerResource> result =
        exporter.export(jobId, authData, Optional.empty());

    assertEquals(ExportResult.ResultType.END, result.getType());
    assertNull(result.getContinuationData());
    VideosContainerResource exportedData = result.getExportedData();

    List<VideoAlbum> expectedAlbums =
        ImmutableList.of(
            new VideoAlbum("/Album 2 :heart:", "Album 2 ❤️", "Album 2 description ❤️"),
            new VideoAlbum("/Videos", "Videos", null));
    assertEquals(expectedAlbums, exportedData.getAlbums());

    List<VideoModel> expectedVideos =
        ImmutableList.of(
            new VideoModel(
                "Video 1.mp4",
                "https://app-1.koofr.net/content/files/get/Video+1.mp4?base=TESTBASE",
                null,
                "video/mp4",
                "/Album 2 :heart:/Video 1.mp4",
                "/Album 2 :heart:",
                false,
                Date.from(Instant.parse("2020-09-04T12:40:57.741Z"))),
            new VideoModel(
                "Video 2.mp4",
                "https://app-1.koofr.net/content/files/get/Video+2.mp4?base=TESTBASE",
                "Video 3 description",
                "video/mp4",
                "/Videos/Video 2.mp4",
                "/Videos",
                false,
                Date.from(Instant.parse("2020-09-04T12:41:06.949Z"))));
    assertEquals(expectedVideos, exportedData.getVideos());
  }
}
