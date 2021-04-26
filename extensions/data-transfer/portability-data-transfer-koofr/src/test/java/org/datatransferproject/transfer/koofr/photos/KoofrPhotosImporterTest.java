package org.datatransferproject.transfer.koofr.photos;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.io.ByteArrayInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Callable;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.apache.commons.io.IOUtils;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore.InputStreamWrapper;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.transfer.koofr.KoofrTransmogrificationConfig;
import org.datatransferproject.transfer.koofr.common.KoofrClient;
import org.datatransferproject.transfer.koofr.common.KoofrClientFactory;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
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
public class KoofrPhotosImporterTest {

  private KoofrClientFactory clientFactory;
  private KoofrClient client;
  private Monitor monitor;
  private JobStore jobStore;
  private KoofrPhotosImporter importer;
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
    jobStore = mock(JobStore.class);

    importer = new KoofrPhotosImporter(clientFactory, monitor, jobStore);

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
  public void testImportItemFromURL() throws Exception {
    // blank.jpg generated using
    // convert -size 1x1 xc:white blank.jpg
    // exiftool "-CreateDate=2020:08:03 11:55:24" blank.jpg
    final byte[] blankBytes =
        IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("blank.jpg"));

    server.enqueue(new MockResponse().setResponseCode(200).setBody("123"));
    server.enqueue(new MockResponse().setResponseCode(200).setBody("4567"));
    final Buffer blankBuffer = new Buffer();
    blankBuffer.write(blankBytes);
    server.enqueue(new MockResponse().setResponseCode(200).setBody(blankBuffer));
    blankBuffer.close();
    server.enqueue(new MockResponse().setResponseCode(200).setBody("89"));
    server.enqueue(new MockResponse().setResponseCode(200).setBody("0"));

    when(client.ensureRootFolder()).thenReturn("/root");
    when(executor.getCachedValue(eq("id1"))).thenReturn("/root/Album 1");
    when(executor.getCachedValue(eq("id2"))).thenReturn("/root/Album");

    when(client.fileExists("/root/Album 1/pic1.jpg")).thenReturn(false);
    when(client.fileExists("/root/Album 1/pic2.png")).thenReturn(true);
    when(client.fileExists("/root/Album 1/2020-08-03 11.55.24 pic3.jpg")).thenReturn(false);
    when(client.fileExists("/root/Album 1/2020-08-17 11.55.24 pic4.jpg")).thenReturn(false);
    when(client.fileExists("/root/Album/pic5.jpg")).thenReturn(false);

    String description1000 = new String(new char[1000]).replace("\0", "a");
    String description1001 = new String(new char[1001]).replace("\0", "a");

    UUID jobId = UUID.randomUUID();

    PortabilityJob job = mock(PortabilityJob.class);
    when(jobStore.findJob(jobId)).thenReturn(job);

    Collection<PhotoAlbum> albums =
        ImmutableList.of(
            new PhotoAlbum("id1", "Album 1", "This is a fake album"),
            new PhotoAlbum("id2", "", description1001));

    Collection<PhotoModel> photos =
        ImmutableList.of(
            new PhotoModel(
                "pic1.jpg",
                server.url("/1.jpg").toString(),
                null,
                "image/jpeg",
                "p1",
                "id1",
                false,
                null),
            new PhotoModel(
                "pic2.png",
                server.url("/2.png").toString(),
                "fine art",
                "image/png",
                "p2",
                "id1",
                false,
                null),
            new PhotoModel(
                "pic3.jpg",
                server.url("/3.jpg").toString(),
                "A pic with EXIF",
                "image/jpeg",
                "p3",
                "id1",
                false,
                null),
            new PhotoModel(
                "pic4.jpg",
                server.url("/4.jpg").toString(),
                "A pic with uploaded time",
                "image/jpeg",
                "p4",
                "id1",
                false,
                new SimpleDateFormat("yyyy:MM:dd HH:mm:ss").parse("2020:08:17 11:55:24")),
            new PhotoModel(
                "pic5.jpg",
                server.url("/5.jpg").toString(),
                description1001,
                "image/jpeg",
                "p5",
                "id2",
                false,
                null));

    PhotosContainerResource resource = spy(new PhotosContainerResource(albums, photos));

    importer.importItem(jobId, executor, authData, resource);

    InOrder clientInOrder = Mockito.inOrder(client);

    verify(resource).transmogrify(any(KoofrTransmogrificationConfig.class));
    clientInOrder.verify(client).ensureRootFolder();
    clientInOrder.verify(client).ensureFolder("/root", "Album 1");
    clientInOrder.verify(client).addDescription("/root/Album 1", "This is a fake album");
    clientInOrder.verify(client).ensureFolder("/root", "Album");
    clientInOrder.verify(client).addDescription("/root/Album", description1000);
    clientInOrder.verify(client).fileExists(eq("/root/Album 1/pic1.jpg"));
    clientInOrder
        .verify(client)
        .uploadFile(
            eq("/root/Album 1"), eq("pic1.jpg"), any(), eq("image/jpeg"), isNull(), isNull());
    clientInOrder.verify(client).fileExists(eq("/root/Album 1/pic2.png"));
    clientInOrder.verify(client).fileExists(eq("/root/Album 1/2020-08-03 11.55.24 pic3.jpg"));
    clientInOrder
        .verify(client)
        .uploadFile(
            eq("/root/Album 1"),
            eq("2020-08-03 11.55.24 pic3.jpg"),
            any(),
            eq("image/jpeg"),
            eq(new SimpleDateFormat("yyyy:MM:dd HH:mm:ss").parse("2020:08:03 11:55:24")),
            eq("A pic with EXIF"));
    clientInOrder
        .verify(client)
        .uploadFile(
            eq("/root/Album 1"),
            eq("2020-08-17 11.55.24 pic4.jpg"),
            any(),
            eq("image/jpeg"),
            eq(new SimpleDateFormat("yyyy:MM:dd HH:mm:ss").parse("2020:08:17 11:55:24")),
            eq("A pic with uploaded time"));
    clientInOrder.verify(client).fileExists(eq("/root/Album/pic5.jpg"));
    clientInOrder
        .verify(client)
        .uploadFile(
            eq("/root/Album"),
            eq("pic5.jpg"),
            any(),
            eq("image/jpeg"),
            isNull(),
            eq(description1000));
    clientInOrder.verifyNoMoreInteractions();
  }

  @Test
  public void testImportItemFromJobStore() throws Exception {
    ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[] {0, 1, 2, 3, 4});
    when(client.ensureRootFolder()).thenReturn("/root");
    when(jobStore.getStream(any(), any())).thenReturn(new InputStreamWrapper(inputStream, 5L));
    doNothing().when(jobStore).removeData(any(), anyString());
    when(executor.getCachedValue(eq("id1"))).thenReturn("/root/Album 1");

    UUID jobId = UUID.randomUUID();

    Collection<PhotoAlbum> albums =
        ImmutableList.of(new PhotoAlbum("id1", "Album 1", "This is a fake album"));

    Collection<PhotoModel> photos =
        ImmutableList.of(
            new PhotoModel(
                "pic1.jpg", "http://fake.com/1.jpg", "A pic", "image/jpeg", "p1", "id1", true),
            new PhotoModel(
                "pic2.png", "https://fake.com/2.png", "fine art", "image/png", "p2", "id1", true));

    PhotosContainerResource resource = spy(new PhotosContainerResource(albums, photos));

    importer.importItem(jobId, executor, authData, resource);

    InOrder clientInOrder = Mockito.inOrder(client);

    verify(resource).transmogrify(any(KoofrTransmogrificationConfig.class));
    clientInOrder.verify(client).ensureRootFolder();
    clientInOrder.verify(client).ensureFolder("/root", "Album 1");
    clientInOrder
        .verify(client)
        .uploadFile(
            eq("/root/Album 1"), eq("pic1.jpg"), any(), eq("image/jpeg"), isNull(), eq("A pic"));
    clientInOrder
        .verify(client)
        .uploadFile(
            eq("/root/Album 1"), eq("pic2.png"), any(), eq("image/png"), isNull(), eq("fine art"));
    verify(jobStore, Mockito.times(2)).removeData(any(), anyString());
  }

  @Test
  public void testImportItemFromJobStoreUserTimeZone() throws Exception {
    ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[] {0, 1, 2, 3, 4});
    when(jobStore.getStream(any(), any())).thenReturn(new InputStreamWrapper(inputStream, 5L));

    UUID jobId = UUID.randomUUID();

    PortabilityJob job = mock(PortabilityJob.class);
    when(job.userTimeZone()).thenReturn(TimeZone.getTimeZone("Europe/Rome"));
    when(jobStore.findJob(jobId)).thenReturn(job);

    Collection<PhotoAlbum> albums =
        ImmutableList.of(new PhotoAlbum("id1", "Album 1", "This is a fake album"));

    DateFormat format = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
    format.setTimeZone(TimeZone.getTimeZone("Europe/Kiev"));

    Collection<PhotoModel> photos =
        ImmutableList.of(
            new PhotoModel(
                "pic1.jpg",
                "http://fake.com/1.jpg",
                "A pic",
                "image/jpeg",
                "p1",
                "id1",
                true,
                format.parse("2021:02:16 11:55:00")));

    PhotosContainerResource resource = spy(new PhotosContainerResource(albums, photos));
    importer.importItem(jobId, executor, authData, resource);
    InOrder clientInOrder = Mockito.inOrder(client);

    clientInOrder
        .verify(client)
        .uploadFile(any(), eq("2021-02-16 10.55.00 pic1.jpg"), any(), any(), any(), any());
  }

  @Test
  public void testImportItemFromJobStoreUserTimeZoneCalledOnce() throws Exception {
    ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[] {0, 1, 2, 3, 4});
    when(jobStore.getStream(any(), any())).thenReturn(new InputStreamWrapper(inputStream, 5L));

    UUID jobId = UUID.randomUUID();

    PortabilityJob job = mock(PortabilityJob.class);
    when(job.userTimeZone()).thenReturn(TimeZone.getTimeZone("Europe/Rome"));
    when(jobStore.findJob(jobId)).thenReturn(job);

    Collection<PhotoAlbum> albums =
        ImmutableList.of(new PhotoAlbum("id1", "Album 1", "This is a fake album"));

    DateFormat format = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
    format.setTimeZone(TimeZone.getTimeZone("Europe/Kiev"));

    Collection<PhotoModel> photos1 =
        ImmutableList.of(
            new PhotoModel(
                "pic1.jpg",
                "http://fake.com/1.jpg",
                "A pic",
                "image/jpeg",
                "p1",
                "id1",
                true,
                format.parse("2021:02:16 11:55:00")));

    Collection<PhotoModel> photos2 =
        ImmutableList.of(
            new PhotoModel(
                "pic2.jpg",
                "http://fake.com/2.jpg",
                "A pic",
                "image/jpeg",
                "p2",
                "id1",
                true,
                format.parse("2021:02:17 11:55:00")));

    PhotosContainerResource resource1 = spy(new PhotosContainerResource(albums, photos1));
    PhotosContainerResource resource2 = spy(new PhotosContainerResource(albums, photos2));
    importer.importItem(jobId, executor, authData, resource1);
    importer.importItem(jobId, executor, authData, resource2);
    InOrder clientInOrder = Mockito.inOrder(client);

    String[] titles = {"2021-02-16 10.55.00 pic1.jpg", "2021-02-17 10.55.00 pic2.jpg"};
    for (String title : titles) {
      clientInOrder.verify(client).uploadFile(any(), eq(title), any(), any(), any(), any());
    }

    verify(jobStore, atMostOnce()).findJob(jobId);
  }
}
