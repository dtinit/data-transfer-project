package org.datatransferproject.transfer.koofr.videos;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Callable;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.transfer.koofr.common.KoofrClient;
import org.datatransferproject.transfer.koofr.common.KoofrClientFactory;
import org.datatransferproject.types.common.models.videos.VideoAlbum;
import org.datatransferproject.types.common.models.videos.VideoObject;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class KoofrVideosImporterTest {

  private KoofrClientFactory clientFactory;
  private KoofrClient client;
  private Monitor monitor;
  private KoofrVideosImporter importer;
  private IdempotentImportExecutor executor;
  private TokensAndUrlAuthData authData;
  private MockWebServer server;

  @Before
  public void setUp() throws Exception {
    server = new MockWebServer();
    server.start();

    client = mock(KoofrClient.class);

    clientFactory = mock(KoofrClientFactory.class);
    when(clientFactory.create(any())).thenReturn(client);

    monitor = mock(Monitor.class);

    importer = new KoofrVideosImporter(clientFactory, monitor);

    executor = mock(IdempotentImportExecutor.class);
    when(executor.executeAndSwallowIOExceptions(any(), any(), any()))
        .then(
            (InvocationOnMock invocation) -> {
              Callable<String> callable = invocation.getArgument(2);
              return callable.call();
            });
    authData = new TokensAndUrlAuthData("acc", "refresh", "");
  }

  @After
  public void tearDown() throws Exception {
    server.shutdown();
  }

  @Test
  public void testImportItemFromURLWithAlbum() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(200).setBody("123"));
    server.enqueue(new MockResponse().setResponseCode(200).setBody("4567"));
    server.enqueue(new MockResponse().setResponseCode(200).setBody("89"));

    when(client.ensureRootFolder()).thenReturn("/root");
    when(executor.getCachedValue(eq("id1"))).thenReturn("/root/Album 1");
    when(executor.getCachedValue(eq("id2"))).thenReturn("/root/Album");

    when(client.fileExists("/root/Album 1/video1.mp4")).thenReturn(false);
    when(client.fileExists("/root/Album 1/video2.mp4")).thenReturn(true);
    when(client.fileExists("/root/Album/video3.mp4")).thenReturn(false);

    String description1000 = new String(new char[1000]).replace("\0", "a");
    String description1001 = new String(new char[1001]).replace("\0", "a");

    UUID jobId = UUID.randomUUID();
    Collection<VideoAlbum> albums =
        ImmutableList.of(
            new VideoAlbum("id1", "Album 1", "This is a fake album"),
            new VideoAlbum("id2", "", description1001));

    Collection<VideoObject> videos =
        ImmutableList.of(
            new VideoObject(
                "video1.mp4",
                server.url("/1.mp4").toString(),
                "A video 1",
                "video/mp4",
                "video1",
                "id1",
                false),
            new VideoObject(
                "video2.mp4",
                server.url("/2.mp4").toString(),
                "A video 2",
                "video/mp4",
                "video2",
                "id1",
                false),
            new VideoObject(
                "video3.mp4",
                server.url("/3.mp4").toString(),
                description1001,
                "video/mp4",
                "video3",
                "id2",
                false));

    VideosContainerResource resource = spy(new VideosContainerResource(albums, videos));

    importer.importItem(jobId, executor, authData, resource);

    InOrder clientInOrder = Mockito.inOrder(client);

    clientInOrder.verify(client).ensureRootFolder();
    clientInOrder.verify(client).ensureFolder("/root", "Album 1");
    clientInOrder.verify(client).addDescription("/root/Album 1", "This is a fake album");
    clientInOrder.verify(client).ensureFolder("/root", "Album");
    clientInOrder.verify(client).addDescription("/root/Album", description1000);
    clientInOrder.verify(client).fileExists(eq("/root/Album 1/video1.mp4"));
    clientInOrder
        .verify(client)
        .uploadFile(
            eq("/root/Album 1"),
            eq("video1.mp4"),
            any(),
            eq("video/mp4"),
            isNull(),
            eq("A video 1"));
    clientInOrder.verify(client).fileExists(eq("/root/Album 1/video2.mp4"));
    clientInOrder.verify(client).fileExists(eq("/root/Album/video3.mp4"));
    clientInOrder
        .verify(client)
        .uploadFile(
            eq("/root/Album"),
            eq("video3.mp4"),
            any(),
            eq("video/mp4"),
            isNull(),
            eq(description1000));
    clientInOrder.verifyNoMoreInteractions();
  }

  @Test
  public void testImportItemFromURLWithoutAlbum() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(200).setBody("123"));
    server.enqueue(new MockResponse().setResponseCode(200).setBody("4567"));

    when(client.ensureVideosFolder()).thenReturn("/root/Videos");

    UUID jobId = UUID.randomUUID();
    Collection<VideoAlbum> albums = ImmutableList.of();

    Collection<VideoObject> videos =
        ImmutableList.of(
            new VideoObject(
                "video1.mp4",
                server.url("/1.mp4").toString(),
                "A video 1",
                "video/mp4",
                "video1",
                null,
                false),
            new VideoObject(
                "video2.mp4",
                server.url("/2.mp4").toString(),
                "A video 2",
                "video/mp4",
                "video2",
                null,
                false));

    VideosContainerResource resource = spy(new VideosContainerResource(albums, videos));

    importer.importItem(jobId, executor, authData, resource);

    InOrder clientInOrder = Mockito.inOrder(client);

    clientInOrder.verify(client).ensureVideosFolder();
    clientInOrder.verify(client).fileExists(eq("/root/Videos/video1.mp4"));
    clientInOrder
        .verify(client)
        .uploadFile(
            eq("/root/Videos"),
            eq("video1.mp4"),
            any(),
            eq("video/mp4"),
            isNull(),
            eq("A video 1"));
    clientInOrder.verify(client).fileExists(eq("/root/Videos/video2.mp4"));
    clientInOrder
        .verify(client)
        .uploadFile(
            eq("/root/Videos"),
            eq("video2.mp4"),
            any(),
            eq("video/mp4"),
            isNull(),
            eq("A video 2"));
    clientInOrder.verifyNoMoreInteractions();
  }
}
