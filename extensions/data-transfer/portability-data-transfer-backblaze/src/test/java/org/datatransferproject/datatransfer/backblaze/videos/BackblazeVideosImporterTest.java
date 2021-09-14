// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package org.datatransferproject.datatransfer.backblaze.videos;

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
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class BackblazeVideosImporterTest {
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
        BackblazeVideosImporter sut =
                new BackblazeVideosImporter(monitor, dataStore, streamProvider, clientFactory);
        ImportResult result = sut.importItem(UUID.randomUUID(), executor, authData, null);
        assertEquals(ImportResult.OK, result);
    }

    @Test
    public void testNullVideos() throws Exception {
        VideosContainerResource data = mock(VideosContainerResource.class);
        when(data.getVideos()).thenReturn(null);

        BackblazeVideosImporter sut =
                new BackblazeVideosImporter(monitor, dataStore, streamProvider, clientFactory);
        ImportResult result = sut.importItem(UUID.randomUUID(), executor, authData, data);
        assertEquals(ImportResult.OK, result);
    }

    @Test
    public void testEmptyVideos() throws Exception {
        VideosContainerResource data = mock(VideosContainerResource.class);
        when(data.getVideos()).thenReturn(new ArrayList<>());

        BackblazeVideosImporter sut =
                new BackblazeVideosImporter(monitor, dataStore, streamProvider, clientFactory);
        ImportResult result = sut.importItem(UUID.randomUUID(), executor, authData, data);
        assertEquals(ImportResult.OK, result);
    }

    @Test
    public void testImportVideo() throws Exception {
        String dataId = "dataId";
        String title = "title";
        String videoUrl = "videoUrl";
        String description = "description";
        String encodingFormat = "UTF-8";
        String albumName = "albumName";
        String albumId = "albumId";
        String response = "response";

        VideoModel videoObject =
                new VideoModel(title, videoUrl, description, encodingFormat, dataId, albumId, false);
        ArrayList<VideoModel> videos = new ArrayList<>();
        videos.add(videoObject);
        VideosContainerResource data = mock(VideosContainerResource.class);
        when(data.getVideos()).thenReturn(videos);

        when(executor.getCachedValue(albumId)).thenReturn(albumName);

        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getInputStream()).thenReturn(IOUtils.toInputStream("video content", "UTF-8"));
        when(streamProvider.getConnection(videoUrl)).thenReturn(connection);

        when(client.uploadFile(eq("Video Transfer/dataId.mp4"), any())).thenReturn(response);
        when(clientFactory.getOrCreateB2Client(monitor, authData)).thenReturn(client);

        BackblazeVideosImporter sut =
                new BackblazeVideosImporter(monitor, dataStore, streamProvider, clientFactory);
        sut.importItem(UUID.randomUUID(), executor, authData, data);

        ArgumentCaptor<Callable<String>> importCapture = ArgumentCaptor.forClass(Callable.class);
        verify(executor, times(1))
                .executeAndSwallowIOExceptions(eq(dataId), eq(title), importCapture.capture());

        String actual = importCapture.getValue().call();
        assertEquals(response, actual);
    }
}