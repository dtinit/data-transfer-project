// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package org.datatransferproject.datatransfer.backblaze.photos;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.apache.commons.io.IOUtils;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.backblaze.common.BackblazeDataTransferClient;
import org.datatransferproject.datatransfer.backblaze.common.BackblazeDataTransferClientFactory;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.transfer.ImageStreamProvider;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class BackblazePhotosImporterTest {
    Monitor monitor;
    TemporaryPerJobDataStore dataStore;
    ImageStreamProvider streamProvider;
    BackblazeDataTransferClientFactory clientFactory;
    IdempotentImportExecutor executor;
    TokenSecretAuthData authData;
    BackblazeDataTransferClient client;

    @Before
    public void setUp() {
        monitor = mock(Monitor.class);
        dataStore = mock(TemporaryPerJobDataStore.class);
        streamProvider = mock(ImageStreamProvider.class);
        clientFactory = mock(BackblazeDataTransferClientFactory.class);
        executor = mock(IdempotentImportExecutor.class);
        authData = mock(TokenSecretAuthData.class);
        client = mock(BackblazeDataTransferClient.class);
    }

    @Test
    public void testNullData() throws Exception {
        BackblazePhotosImporter sut =
                new BackblazePhotosImporter(monitor, dataStore, streamProvider, clientFactory);
        ImportResult result = sut.importItem(UUID.randomUUID(), executor, authData, null);
        assertEquals(ImportResult.OK, result);
    }

    @Test
    public void testNullPhotosAndAlbums() throws Exception {
        PhotosContainerResource data = mock(PhotosContainerResource.class);
        when(data.getAlbums()).thenReturn(null);
        when(data.getPhotos()).thenReturn(null);

        BackblazePhotosImporter sut =
                new BackblazePhotosImporter(monitor, dataStore, streamProvider, clientFactory);
        ImportResult result = sut.importItem(UUID.randomUUID(), executor, authData, data);
        assertEquals(ImportResult.OK, result);
    }

    @Test
    public void testEmptyPhotosAndAlbums() throws Exception {
        PhotosContainerResource data = mock(PhotosContainerResource.class);
        when(data.getAlbums()).thenReturn(new ArrayList<>());
        when(data.getPhotos()).thenReturn(new ArrayList<>());

        BackblazePhotosImporter sut =
                new BackblazePhotosImporter(monitor, dataStore, streamProvider, clientFactory);
        ImportResult result = sut.importItem(UUID.randomUUID(), executor, authData, data);
        assertEquals(ImportResult.OK, result);
    }

    @Test
    public void testImportPhoto() throws Exception {
        String dataId = "dataId";
        String title = "title";
        String photoUrl = "photoUrl";
        String albumName = "albumName";
        String albumId = "albumId";
        String response = "response";

        PhotoModel photoModel = new PhotoModel(title, photoUrl, "", "", dataId, albumId, false, null);
        ArrayList<PhotoModel> photos = new ArrayList<>();
        photos.add(photoModel);
        PhotosContainerResource data = mock(PhotosContainerResource.class);
        when(data.getPhotos()).thenReturn(photos);

        when(executor.getCachedValue(albumId)).thenReturn(albumName);

        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getInputStream()).thenReturn(IOUtils.toInputStream("photo content", "UTF-8"));
        when(streamProvider.getConnection(photoUrl)).thenReturn(connection);

        when(client.uploadFile(eq("Photo Transfer/albumName/dataId.jpg"), any())).thenReturn(response);
        when(clientFactory.getOrCreateB2Client(monitor, authData)).thenReturn(client);

        BackblazePhotosImporter sut =
                new BackblazePhotosImporter(monitor, dataStore, streamProvider, clientFactory);
        sut.importItem(UUID.randomUUID(), executor, authData, data);

        ArgumentCaptor<Callable<String>> importCapture = ArgumentCaptor.forClass(Callable.class);
        verify(executor, times(1))
                .executeAndSwallowIOExceptions(eq(dataId), eq(title), importCapture.capture());

        String actual = importCapture.getValue().call();
        assertEquals(response, actual);
    }

    @Test
    public void testImportAlbum() throws Exception {
        String albumId = "albumId";
        PhotoAlbum album = new PhotoAlbum(albumId, "", "");
        ArrayList<PhotoAlbum> albums = new ArrayList<>();
        albums.add(album);
        PhotosContainerResource data = mock(PhotosContainerResource.class);
        when(data.getAlbums()).thenReturn(albums);

        BackblazePhotosImporter sut =
                new BackblazePhotosImporter(monitor, dataStore, streamProvider, clientFactory);
        sut.importItem(UUID.randomUUID(), executor, authData, data);

        verify(executor, times(1))
                .executeAndSwallowIOExceptions(
                        eq(albumId), eq("Caching album name for album 'albumId'"), any());
    }
}