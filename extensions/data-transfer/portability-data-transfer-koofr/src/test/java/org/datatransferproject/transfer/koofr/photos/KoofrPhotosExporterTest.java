package org.datatransferproject.transfer.koofr.photos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.transfer.koofr.common.Fixtures;
import org.datatransferproject.transfer.koofr.common.KoofrClient;
import org.datatransferproject.transfer.koofr.common.KoofrClientFactory;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class KoofrPhotosExporterTest {

  private KoofrClientFactory clientFactory;
  private KoofrClient client;
  private Monitor monitor;
  private KoofrPhotosExporter exporter;
  private TokensAndUrlAuthData authData;

  @Before
  public void setUp() throws Exception {
    client = mock(KoofrClient.class);

    clientFactory = mock(KoofrClientFactory.class);
    when(clientFactory.create(any())).thenReturn(client);

    monitor = mock(Monitor.class);

    exporter = new KoofrPhotosExporter(clientFactory, monitor);

    authData = new TokensAndUrlAuthData("acc", "refresh", "");
  }

  @Test
  public void testExport() throws Exception {
    when(client.getRootPath()).thenReturn("/Data transfer");
    when(client.listRecursive("/Data transfer")).thenReturn(Fixtures.listRecursiveItems);
    when(client.fileLink("/Data transfer/Album 1/Photo 1.jpg"))
        .thenReturn("https://app-1.koofr.net/content/files/get/Photo+1.jpg?base=TESTBASE");
    when(client.fileLink("/Data transfer/Album 1/Photo 2.jpg"))
        .thenReturn("https://app-1.koofr.net/content/files/get/Photo+2.jpg?base=TESTBASE");
    when(client.fileLink("/Data transfer/Album 2 :heart:/Photo 3.jpg"))
        .thenReturn("https://app-1.koofr.net/content/files/get/Photo+3.jpg?base=TESTBASE");

    UUID jobId = UUID.randomUUID();

    ExportResult<PhotosContainerResource> result =
        exporter.export(jobId, authData, Optional.empty());

    assertEquals(ExportResult.ResultType.END, result.getType());
    assertNull(result.getContinuationData());
    PhotosContainerResource exportedData = result.getExportedData();

    List<PhotoAlbum> expectedAlbums =
        ImmutableList.of(
            new PhotoAlbum("/Album 1", "Album 1", null),
            new PhotoAlbum("/Album 2 :heart:", "Album 2 ❤️", "Album 2 description ❤️"));
    assertEquals(expectedAlbums, exportedData.getAlbums());

    List<PhotoModel> expectedPhotos =
        ImmutableList.of(
            new PhotoModel(
                "Photo 1.jpg",
                "https://app-1.koofr.net/content/files/get/Photo+1.jpg?base=TESTBASE",
                "Photo 1 description",
                "image/jpeg",
                "/Album 1/Photo 1.jpg",
                "/Album 1",
                false,
                new Date(1324824491000L)),
            new PhotoModel(
                "Photo 2.jpg",
                "https://app-1.koofr.net/content/files/get/Photo+2.jpg?base=TESTBASE",
                null,
                "image/jpeg",
                "/Album 1/Photo 2.jpg",
                "/Album 1",
                false,
                new Date(1368774569000L)),
            new PhotoModel(
                "Photo 3.jpg",
                "https://app-1.koofr.net/content/files/get/Photo+3.jpg?base=TESTBASE",
                "Photo 3 description",
                "image/jpeg",
                "/Album 2 :heart:/Photo 3.jpg",
                "/Album 2 :heart:",
                false,
                new Date(1489345497000L)));
    assertEquals(expectedPhotos, exportedData.getPhotos());
  }
}
