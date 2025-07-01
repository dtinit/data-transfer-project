/*
 * Copyright 2025 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.datatransferproject.datatransfer.synology.uploader;

import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.synology.constant.SynologyConstant;
import org.datatransferproject.datatransfer.synology.exceptions.SynologyHttpException;
import org.datatransferproject.datatransfer.synology.exceptions.SynologyImportException;
import org.datatransferproject.datatransfer.synology.service.SynologyDTPService;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.types.common.DownloadableItem;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SynologyUploaderTest {
  @Mock private SynologyDTPService synologyDTPService;
  @Mock private IdempotentImportExecutor executor;
  @Mock private Monitor monitor;

  UUID mockJobId = UUID.randomUUID();

  @BeforeEach
  public void setUp() throws Exception {
    doAnswer(
            invocation -> {
              Callable<String> lambda = invocation.getArgument(2);
              return lambda.call();
            })
        .when(executor)
        .executeAndSwallowIOExceptions(anyString(), anyString(), any());
  }

  private boolean containsMessage(Throwable throwable, String expectedMessage) {
    while (throwable != null) {
      if (throwable.getMessage() != null && throwable.getMessage().contains(expectedMessage)) {
        return true;
      }
      throwable = throwable.getCause();
    }
    return false;
  }

  @Nested
  public class ImportAlbums {
    @Test
    public void shouldImportAlbums() throws Exception {
      SynologyUploader uploader = new SynologyUploader(executor, monitor, synologyDTPService);

      List<MediaAlbum> albums =
          Arrays.asList(
              new MediaAlbum("1", "album1", "desc"), new MediaAlbum("2", "album2", "desc"));

      for (MediaAlbum album : albums) {
        when(synologyDTPService.createAlbum(album, mockJobId))
            .thenReturn(Map.of("album_id", album.getId()));
      }

      uploader.importAlbums(albums, mockJobId);

      verify(synologyDTPService, times(albums.size())).createAlbum(any(), any());

      // should create with cache
      for (MediaAlbum album : albums) {
        verify(synologyDTPService).createAlbum(album, mockJobId);
        verify(executor)
            .executeAndSwallowIOExceptions(eq(album.getId()), eq(album.getName()), any());
      }
    }

    @Test
    public void shouldThrowExceptionIfCreateAlbumFails() {
      SynologyUploader spyUploader =
          Mockito.spy(new SynologyUploader(executor, monitor, synologyDTPService));
      List<MediaAlbum> albums = List.of(new MediaAlbum("1", "album1", "desc"));
      String expectedMessage = "Failed to import albums";
      when(synologyDTPService.createAlbum(any(), any()))
          .thenThrow(
              new SynologyHttpException(
                  expectedMessage, SC_INTERNAL_SERVER_ERROR, "Internal Server Error"));
      Exception e =
          assertThrows(
              SynologyImportException.class, () -> spyUploader.importAlbums(albums, mockJobId));
      assertTrue(containsMessage(e, expectedMessage));
    }
  }

  @Nested
  @TestInstance(Lifecycle.PER_CLASS)
  public class ImportMedia {
    private final List<String> dataIds = List.of("dataId1", "dataId2");
    private final List<String> newItemIds = List.of("1", "2");
    private final String albumId = "1";
    private final String newAlbumId = "10";
    private final Map<String, String> albumIdToNewAlbumIdMap = Map.of(albumId, newAlbumId);
    private final Map<String, String> dataIdToItemIdMap =
        IntStream.range(0, dataIds.size())
            .boxed()
            .collect(Collectors.toMap(dataIds::get, newItemIds::get));
    private final List<PhotoModel> photos =
        List.of(
            new PhotoModel("title1", "url1", "desc", "mediaType", dataIds.get(0), albumId, true),
            new PhotoModel("title2", "url2", "desc", "mediaType", dataIds.get(1), albumId, true));
    private final List<VideoModel> videos =
        List.of(
            new VideoModel("name1", "url1", "desc", "format", dataIds.get(0), albumId, true, null),
            new VideoModel("name2", "url2", "desc", "format", dataIds.get(1), albumId, true, null));

    private Stream<Object> provideMediaItems() {
      return Stream.of(photos, videos);
    }

    @BeforeEach
    public void setUp() {
      lenient()
          .when(synologyDTPService.addItemToAlbum(any(), any(), any()))
          .thenReturn(Map.of("success", true));
      lenient()
          .when(synologyDTPService.createAlbum(any(), any()))
          .thenAnswer(
              invocation -> {
                MediaAlbum album = invocation.getArgument(0);
                return Map.of("album_id", albumIdToNewAlbumIdMap.get(album.getId()));
              });
      for (PhotoModel photo : photos) {
        lenient()
            .when(synologyDTPService.createPhoto(photo, mockJobId))
            .thenAnswer(
                invocation -> {
                  PhotoModel photoModel = invocation.getArgument(0);
                  return Map.of("item_id", dataIdToItemIdMap.get(photoModel.getDataId()));
                });
      }
      for (VideoModel video : videos) {
        lenient()
            .when(synologyDTPService.createVideo(video, mockJobId))
            .thenAnswer(
                invocation -> {
                  VideoModel videoModel = invocation.getArgument(0);
                  return Map.of("item_id", dataIdToItemIdMap.get(videoModel.getDataId()));
                });
      }
    }

    @ParameterizedTest(name = "shouldImportMediaItemsWhenAlbumBeforeItem [{index}] {0}")
    @MethodSource("provideMediaItems")
    public void shouldImportMediaItemsWhenAlbumBeforeItem(
        List<? extends DownloadableItem> mediaItems) {
      SynologyUploader uploader = new SynologyUploader(executor, monitor, synologyDTPService);
      List<MediaAlbum> albums = List.of(new MediaAlbum(albumId, "album1", "desc"));

      uploader.importAlbums(albums, mockJobId);
      if (mediaItems.get(0) instanceof PhotoModel) {
        uploader.importPhotos((List<PhotoModel>) mediaItems, mockJobId);
      } else if (mediaItems.get(0) instanceof VideoModel) {
        uploader.importVideos((List<VideoModel>) mediaItems, mockJobId);
      }

      for (int i = 0; i < mediaItems.size(); i++) {
        Object media = mediaItems.get(i);
        if (media instanceof PhotoModel) {
          verify(synologyDTPService).createPhoto((PhotoModel) media, mockJobId);
        } else if (media instanceof VideoModel) {
          verify(synologyDTPService).createVideo((VideoModel) media, mockJobId);
        }
        verify(synologyDTPService).addItemToAlbum(newAlbumId, newItemIds.get(i), mockJobId);
      }
    }

    @ParameterizedTest(name = "shouldImportMediaItemsWhenItemBeforeAlbum [{index}] {0}")
    @MethodSource("provideMediaItems")
    public void shouldImportMediaItemsWhenItemBeforeAlbum(
        List<? extends DownloadableItem> mediaItems) throws Exception {
      SynologyUploader uploader = new SynologyUploader(executor, monitor, synologyDTPService);
      List<MediaAlbum> albums = List.of(new MediaAlbum(albumId, "album1", "desc"));

      if (mediaItems.get(0) instanceof PhotoModel) {
        uploader.importPhotos((List<PhotoModel>) mediaItems, mockJobId);
      } else if (mediaItems.get(0) instanceof VideoModel) {
        uploader.importVideos((List<VideoModel>) mediaItems, mockJobId);
      }
      uploader.importAlbums(albums, mockJobId);

      for (int i = 0; i < mediaItems.size(); i++) {
        Object media = mediaItems.get(i);
        if (media instanceof PhotoModel) {
          verify(synologyDTPService).createPhoto((PhotoModel) media, mockJobId);
        } else if (media instanceof VideoModel) {
          verify(synologyDTPService).createVideo((VideoModel) media, mockJobId);
        }
        verify(synologyDTPService).addItemToAlbum(newAlbumId, newItemIds.get(i), mockJobId);
      }
    }

    @ParameterizedTest(name = "shouldImportMediaItemsWithCache [{index}] {0}")
    @MethodSource("provideMediaItems")
    public void shouldImportMediaItemsWithCache(List<? extends DownloadableItem> mediaItems)
        throws Exception {
      SynologyUploader uploader = new SynologyUploader(executor, monitor, synologyDTPService);
      List<MediaAlbum> albums = List.of(new MediaAlbum(albumId, "album1", "desc"));

      uploader.importAlbums(albums, mockJobId);
      if (mediaItems.get(0) instanceof PhotoModel) {
        uploader.importPhotos((List<PhotoModel>) mediaItems, mockJobId);
      } else if (mediaItems.get(0) instanceof VideoModel) {
        uploader.importVideos((List<VideoModel>) mediaItems, mockJobId);
      }

      for (Object media : mediaItems) {
        if (media instanceof PhotoModel) {
          verify(executor)
              .executeAndSwallowIOExceptions(
                  eq(((PhotoModel) media).getDataId()), eq(((PhotoModel) media).getTitle()), any());
        } else if (media instanceof VideoModel) {
          verify(executor)
              .executeAndSwallowIOExceptions(
                  eq(((VideoModel) media).getDataId()), eq(((VideoModel) media).getName()), any());
        }
        String newItemId = newItemIds.get(mediaItems.indexOf(media));
        String albumIdToItemId =
            String.format(SynologyConstant.ALBUM_ITEM_ID_FORMAT, newAlbumId, newItemId);
        verify(executor).executeAndSwallowIOExceptions(eq(albumIdToItemId), eq(newItemId), any());
      }
    }

    @ParameterizedTest(
        name = "shouldThrowHttpExceptionWithStatusCodeIfCreateMediaItemFails [{index}] {0}")
    @MethodSource("provideMediaItems")
    public void shouldThrowHttpExceptionWithStatusCodeIfCreateMediaItemFails(
        List<? extends DownloadableItem> mediaItems) {
      SynologyUploader spyUploader =
          Mockito.spy(new SynologyUploader(executor, monitor, synologyDTPService));
      List<MediaAlbum> albums = List.of(new MediaAlbum("1", "album1", "desc"));
      String expectedMessage = String.format("statusCode=%d", SC_INTERNAL_SERVER_ERROR);

      if (mediaItems.get(0) instanceof PhotoModel) {
        when(synologyDTPService.createPhoto(any(), any()))
            .thenThrow(
                new SynologyHttpException(
                    "Failed to create photo", SC_INTERNAL_SERVER_ERROR, "Internal Server Error"));
      } else if (mediaItems.get(0) instanceof VideoModel) {
        when(synologyDTPService.createVideo(any(), any()))
            .thenThrow(
                new SynologyHttpException(
                    "Failed to create video", SC_INTERNAL_SERVER_ERROR, "Internal Server Error"));
      }

      spyUploader.importAlbums(albums, mockJobId);

      if (mediaItems.get(0) instanceof PhotoModel) {
        Exception e =
            assertThrows(
                SynologyImportException.class,
                () -> spyUploader.importPhotos((List<PhotoModel>) mediaItems, mockJobId));
        assertTrue(containsMessage(e, expectedMessage));
      } else if (mediaItems.get(0) instanceof VideoModel) {
        Exception e =
            assertThrows(
                SynologyImportException.class,
                () -> spyUploader.importVideos((List<VideoModel>) mediaItems, mockJobId));
        assertTrue(containsMessage(e, expectedMessage));
      }
    }

    @ParameterizedTest(name = "shouldThrowExceptionIfCreateMediaItemNotSuccess [{index}] {0}")
    @MethodSource("provideMediaItems")
    public void shouldThrowExceptionIfCreateMediaItemNotSuccess(
        List<? extends DownloadableItem> mediaItems) {
      SynologyUploader spyUploader =
          Mockito.spy(new SynologyUploader(executor, monitor, synologyDTPService));
      List<MediaAlbum> albums = List.of(new MediaAlbum("1", "album1", "desc"));

      if (mediaItems.get(0) instanceof PhotoModel) {
        when(synologyDTPService.createPhoto(any(), any())).thenReturn(Map.of("success", false));
      } else if (mediaItems.get(0) instanceof VideoModel) {
        when(synologyDTPService.createVideo(any(), any())).thenReturn(Map.of("success", false));
      }

      spyUploader.importAlbums(albums, mockJobId);

      if (mediaItems.get(0) instanceof PhotoModel) {
        Exception e =
            assertThrows(
                SynologyImportException.class,
                () -> spyUploader.importPhotos((List<PhotoModel>) mediaItems, mockJobId));
        assertTrue(containsMessage(e, "Failed to import photos"));
      } else if (mediaItems.get(0) instanceof VideoModel) {
        Exception e =
            assertThrows(
                SynologyImportException.class,
                () -> spyUploader.importVideos((List<VideoModel>) mediaItems, mockJobId));
        assertTrue(containsMessage(e, "Failed to import videos"));
      }
    }

    @ParameterizedTest(name = "shouldThrowExceptionIfAddItemToAlbumFails [{index}] {0}")
    @MethodSource("provideMediaItems")
    public void shouldThrowExceptionIfAddItemToAlbumFails(
        List<? extends DownloadableItem> mediaItems) {
      SynologyUploader spyUploader =
          Mockito.spy(new SynologyUploader(executor, monitor, synologyDTPService));
      List<MediaAlbum> albums = List.of(new MediaAlbum("1", "album1", "desc"));
      String expectedMessage = "Failed to add item to album";

      when(synologyDTPService.addItemToAlbum(any(), any(), any()))
          .thenThrow(
              new SynologyHttpException(
                  expectedMessage, SC_INTERNAL_SERVER_ERROR, "Internal Server Error"));

      spyUploader.importAlbums(albums, mockJobId);
      if (mediaItems.get(0) instanceof PhotoModel) {
        Exception e =
            assertThrows(
                SynologyImportException.class,
                () -> spyUploader.importPhotos((List<PhotoModel>) mediaItems, mockJobId));
        assertTrue(containsMessage(e, expectedMessage));
      } else if (mediaItems.get(0) instanceof VideoModel) {
        Exception e =
            assertThrows(
                SynologyImportException.class,
                () -> spyUploader.importVideos((List<VideoModel>) mediaItems, mockJobId),
                expectedMessage);
        assertTrue(containsMessage(e, expectedMessage));
      }
    }

    @ParameterizedTest(name = "shouldThrowExceptionIfAddItemToAlbumNotSuccess [{index}] {0}")
    @MethodSource("provideMediaItems")
    public void shouldThrowExceptionIfAddItemToAlbumNotSuccess(
        List<? extends DownloadableItem> mediaItems) {
      SynologyUploader spyUploader =
          Mockito.spy(new SynologyUploader(executor, monitor, synologyDTPService));
      List<MediaAlbum> albums = List.of(new MediaAlbum("1", "album1", "desc"));
      String expectedMessage = "Unsuccessful";

      when(synologyDTPService.addItemToAlbum(any(), any(), any()))
          .thenReturn(Map.of("success", false));

      spyUploader.importAlbums(albums, mockJobId);
      if (mediaItems.get(0) instanceof PhotoModel) {
        Exception e =
            assertThrows(
                SynologyImportException.class,
                () -> spyUploader.importPhotos((List<PhotoModel>) mediaItems, mockJobId));
        assertTrue(containsMessage(e, expectedMessage));
      } else if (mediaItems.get(0) instanceof VideoModel) {
        Exception e =
            assertThrows(
                SynologyImportException.class,
                () -> spyUploader.importVideos((List<VideoModel>) mediaItems, mockJobId));
        assertTrue(containsMessage(e, expectedMessage));
      }
    }
  }
}
