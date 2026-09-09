/*
 * Copyright 2026 The Data Transfer Project Authors.
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
 */

package org.datatransferproject.transfer.amazon.photos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.transfer.JobMetadata;
import org.datatransferproject.transfer.amazon.photos.model.AmazonPhotosNode;
import org.datatransferproject.types.common.models.FavoriteInfo;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class AmazonMediaImporterTest {

  @Mock private Monitor monitor;
  @Mock private TemporaryPerJobDataStore dataStore;
  @Mock private IdempotentImportExecutor executor;
  @Mock private IdempotentImportExecutor retryingExecutor;
  @Mock private AmazonPhotosInterface client;

  private TokensAndUrlAuthData authData;
  private AmazonMediaImporter importer;
  private UUID jobId;

  @BeforeEach
  void setUp() {
    authData = new TokensAndUrlAuthData("access", "refresh", "http://token-url");
    importer = new AmazonMediaImporter(monitor, dataStore, client);
    jobId = UUID.randomUUID();
  }

  private static MediaContainerResource media(
      List<MediaAlbum> albums, List<PhotoModel> photos, List<VideoModel> videos) {
    return new MediaContainerResource(albums, photos, videos);
  }

  // ---------------------------------------------------------------------------
  // Retrying-executor selection (platform retry opt-in).
  // ---------------------------------------------------------------------------

  @Test
  void importItem_usesRetryingExecutorWhenEnabled() throws Exception {
    AmazonMediaImporter retryingImporter =
        new AmazonMediaImporter(monitor, dataStore, client, retryingExecutor, true);
    MediaAlbum album = new MediaAlbum("a1", "Album", null);
    MediaContainerResource resource =
        media(Collections.singletonList(album), Collections.emptyList(), Collections.emptyList());

    retryingImporter.importItem(jobId, executor, authData, resource);

    verify(retryingExecutor).executeAndSwallowIOExceptions(eq("a1"), eq("Album"), any());
    verifyNoInteractions(executor);
  }

  @Test
  void importItem_usesDefaultExecutorWhenRetryingDisabled() throws Exception {
    AmazonMediaImporter retryingImporter =
        new AmazonMediaImporter(monitor, dataStore, client, retryingExecutor, false);
    MediaAlbum album = new MediaAlbum("a1", "Album", null);
    MediaContainerResource resource =
        media(Collections.singletonList(album), Collections.emptyList(), Collections.emptyList());

    retryingImporter.importItem(jobId, executor, authData, resource);

    verify(executor).executeAndSwallowIOExceptions(eq("a1"), eq("Album"), any());
    verifyNoInteractions(retryingExecutor);
  }

  @Test
  void importItem_usesDefaultExecutorWhenNoRetryingExecutorProvided() throws Exception {
    AmazonMediaImporter retryingImporter =
        new AmazonMediaImporter(monitor, dataStore, client, null, true);
    MediaAlbum album = new MediaAlbum("a1", "Album", null);
    MediaContainerResource resource =
        media(Collections.singletonList(album), Collections.emptyList(), Collections.emptyList());

    retryingImporter.importItem(jobId, executor, authData, resource);

    verify(executor).executeAndSwallowIOExceptions(eq("a1"), eq("Album"), any());
  }

  // ---------------------------------------------------------------------------
  // Unified MEDIA container registers albums, photos AND videos with the executor.
  // ---------------------------------------------------------------------------

  @Test
  void importItem_registersAlbumsPhotosAndVideos() throws Exception {
    MediaAlbum album = new MediaAlbum("a1", "Album", null);
    PhotoModel photo = new PhotoModel("pic.jpg", "tempkey",
        null, "image/jpeg", "p1", null, true, (Date) null);
    VideoModel video = new VideoModel("clip.mp4", "tempkey",
        "A video", "video/mp4", "vid-1", null, true, null);
    MediaContainerResource resource = media(
        Collections.singletonList(album),
        Collections.singletonList(photo),
        Collections.singletonList(video));

    ImportResult result = importer.importItem(jobId, executor, authData, resource);

    assertEquals(ImportResult.OK, result);
    verify(executor).executeAndSwallowIOExceptions(eq("a1"), eq("Album"), any());
    verify(executor).executeAndSwallowIOExceptions(
        eq(photo.getIdempotentId()), eq("pic.jpg"), any());
    verify(executor).executeAndSwallowIOExceptions(
        eq(video.getIdempotentId()), eq("clip.mp4"), any());
    // ...and nothing else.
    verify(executor, times(3)).executeAndSwallowIOExceptions(any(), any(), any());
  }

  // ---------------------------------------------------------------------------
  // Storage-quota classification -> terminal DestinationMemoryFullException.
  // ---------------------------------------------------------------------------

  @Test
  void importItem_noActiveSubscription_throwsDestinationMemoryFull() throws Exception {
    PhotoModel photo = new PhotoModel("pic.jpg", "tempkey",
        null, "image/jpeg", "p1", null, true, (Date) null);
    MediaContainerResource resource = media(
        Collections.emptyList(), Collections.singletonList(photo), Collections.emptyList());

    stubDownload();
    executorRunsCallable();
    when(client.uploadContent(any(), any(), any(), any(Long.class), any(), any(Boolean.class), any()))
        .thenThrow(new AmazonPhotosApiException(403, "NoActiveSubscriptionFound", "no subscription"));

    assertThrows(DestinationMemoryFullException.class,
        () -> importer.importItem(jobId, executor, authData, resource));
  }

  // ---------------------------------------------------------------------------
  // createAlbum body (requires the JobMetadata worker singleton, mocked statically).
  // ---------------------------------------------------------------------------

  @Test
  void importItem_createAlbum_invokesClientWithSuffixedNameAndReturnsId() throws Exception {
    MediaAlbum album = new MediaAlbum("a1", "Vacation", "desc");
    MediaContainerResource resource =
        media(Collections.singletonList(album), Collections.emptyList(), Collections.emptyList());

    AmazonPhotosNode node = new AmazonPhotosNode();
    node.setId("amazon-album-1");
    when(client.createAlbum(any())).thenReturn(node);
    executorRunsCallable();

    try (MockedStatic<JobMetadata> jm = mockStatic(JobMetadata.class)) {
      jm.when(JobMetadata::getExportService).thenReturn("Google");
      importer.importItem(jobId, executor, authData, resource);
    }

    org.mockito.ArgumentCaptor<String> name = org.mockito.ArgumentCaptor.forClass(String.class);
    verify(client).createAlbum(name.capture());
    assertEquals("Vacation" + AmazonImportHelper.IMPORTED_SUFFIX + "Google", name.getValue());
  }

  // ---------------------------------------------------------------------------
  // uploadPhoto happy path (covers uploadedTime + favorite mapping, upload, temp cleanup).
  // ---------------------------------------------------------------------------

  @Test
  void importItem_uploadPhoto_happyPath_withUploadedTimeAndFavorite() throws Exception {
    Date uploaded = new Date(1_700_000_000_000L);
    PhotoModel photo = new PhotoModel("pic.jpg", "tempkey", null, "image/jpeg",
        "p1", null, true, null, uploaded, new FavoriteInfo(true, uploaded));
    MediaContainerResource resource = media(
        Collections.emptyList(), Collections.singletonList(photo), Collections.emptyList());

    stubDownload();
    executorRunsCallable();
    AmazonPhotosNode node = new AmazonPhotosNode();
    node.setId("uploaded-1");
    when(client.uploadContent(eq("pic.jpg"), any(), any(), any(Long.class),
        eq(uploaded.toInstant().toString()), eq(true), eq(null))).thenReturn(node);

    importer.importItem(jobId, executor, authData, resource);

    verify(client).uploadContent(eq("pic.jpg"), any(), any(), any(Long.class),
        eq(uploaded.toInstant().toString()), eq(true), eq(null));
    // inTempStore == true -> temp data is cleaned up after upload.
    verify(dataStore).removeData(jobId, "tempkey");
  }

  // ---------------------------------------------------------------------------
  // uploadVideo happy path (covers uploadedTime mapping, non-favorite, upload, temp cleanup).
  // ---------------------------------------------------------------------------

  @Test
  void importItem_uploadVideo_happyPath_withUploadedTime() throws Exception {
    Date uploaded = new Date(1_700_000_000_000L);
    VideoModel video = new VideoModel("clip.mp4", "tempkey", "A video", "video/mp4",
        "vid-1", null, true, uploaded);
    MediaContainerResource resource = media(
        Collections.emptyList(), Collections.emptyList(), Collections.singletonList(video));

    stubDownload();
    executorRunsCallable();
    AmazonPhotosNode node = new AmazonPhotosNode();
    node.setId("uploaded-1");
    when(client.uploadContent(eq("clip.mp4"), any(), any(), any(Long.class),
        eq(uploaded.toInstant().toString()), eq(false), eq(null))).thenReturn(node);

    importer.importItem(jobId, executor, authData, resource);

    verify(client).uploadContent(eq("clip.mp4"), any(), any(), any(Long.class),
        eq(uploaded.toInstant().toString()), eq(false), eq(null));
    verify(dataStore).removeData(jobId, "tempkey");
  }

  @Test
  void importItem_uploadVideo_favorited_passesFavoriteTrue() throws Exception {
    Date uploaded = new Date(1_700_000_000_000L);
    VideoModel video = new VideoModel("clip.mp4", "tempkey", "A video", "video/mp4",
        "vid-1", null, true, uploaded, new FavoriteInfo(true, uploaded));
    MediaContainerResource resource = media(
        Collections.emptyList(), Collections.emptyList(), Collections.singletonList(video));

    stubDownload();
    executorRunsCallable();
    AmazonPhotosNode node = new AmazonPhotosNode();
    node.setId("uploaded-1");
    when(client.uploadContent(eq("clip.mp4"), any(), any(), any(Long.class), any(),
        eq(true), any())).thenReturn(node);

    importer.importItem(jobId, executor, authData, resource);

    // Guards the fix: a favorited video is uploaded with isFavorite=true, not a hardcoded false.
    verify(client).uploadContent(eq("clip.mp4"), any(), any(), any(Long.class), any(),
        eq(true), any());
  }

  // ---------------------------------------------------------------------------
  // Duplicate handling: swallowed as success for both photos and videos.
  // ---------------------------------------------------------------------------

  @Test
  void importItem_uploadPhoto_duplicate_isSkippedWithoutError() throws Exception {
    PhotoModel photo = new PhotoModel("pic.jpg", "tempkey",
        null, "image/jpeg", "p1", null, true, (Date) null);
    MediaContainerResource resource = media(
        Collections.emptyList(), Collections.singletonList(photo), Collections.emptyList());

    stubDownload();
    executorRunsCallable();
    when(client.uploadContent(any(), any(), any(), any(Long.class), any(), any(Boolean.class), any()))
        .thenThrow(new AmazonPhotosApiException(409, "DuplicatesConflictError", "dup"));

    ImportResult result = importer.importItem(jobId, executor, authData, resource);

    assertEquals(ImportResult.OK, result);
    verify(client).uploadContent(eq("pic.jpg"), any(), any(), any(Long.class), any(), any(Boolean.class), any());
  }

  @Test
  void importItem_uploadVideo_duplicate_isSkippedWithoutError() throws Exception {
    VideoModel video = new VideoModel("clip.mp4", "tempkey",
        "A video", "video/mp4", "vid-1", null, true, null);
    MediaContainerResource resource = media(
        Collections.emptyList(), Collections.emptyList(), Collections.singletonList(video));

    stubDownload();
    executorRunsCallable();
    when(client.uploadContent(any(), any(), any(), any(Long.class), any(), any(Boolean.class), any()))
        .thenThrow(new AmazonPhotosApiException(409, "DuplicatesConflictError", "dup"));

    ImportResult result = importer.importItem(jobId, executor, authData, resource);

    assertEquals(ImportResult.OK, result);
    verify(client).uploadContent(eq("clip.mp4"), any(), any(), any(Long.class), any(), any(Boolean.class), any());
  }

  @Test
  void importItem_uploadPhoto_nonDuplicateNonQuotaError_propagates() throws Exception {
    PhotoModel photo = new PhotoModel("pic.jpg", "tempkey",
        null, "image/jpeg", "p1", null, true, (Date) null);
    MediaContainerResource resource = media(
        Collections.emptyList(), Collections.singletonList(photo), Collections.emptyList());

    stubDownload();
    executorRunsCallable();
    when(client.uploadContent(any(), any(), any(), any(Long.class), any(), any(Boolean.class), any()))
        .thenThrow(new AmazonPhotosApiException(403, "ForbiddenAccess", "denied"));

    assertThrows(AmazonPhotosApiException.class,
        () -> importer.importItem(jobId, executor, authData, resource));
  }

  @Test
  void productionConstructor_buildsWithoutNetwork() {
    // Exercises the production wiring; the client is built lazily per job, not at construction.
    assertNotNull(new AmazonMediaImporter(
        monitor, "client-id", "client-secret", dataStore, null, false));
  }

  @Test
  void importItem_photoWithAlbum_resolvesToCreatedAlbumNodeId() throws Exception {
    // Albums are created before items, so a photo referencing an album resolves (via the executor
    // cache) to that album's Amazon node id, which is passed as the uploadContent albumId.
    PhotoModel photo = new PhotoModel("pic.jpg", "tempkey",
        null, "image/jpeg", "p1", "a1", true, (Date) null);
    MediaContainerResource resource = media(
        Collections.emptyList(), Collections.singletonList(photo), Collections.emptyList());

    stubDownload();
    executorRunsCallable();
    when(executor.getCachedValue("a1")).thenReturn("amazon-album-1");
    AmazonPhotosNode node = new AmazonPhotosNode();
    node.setId("uploaded-1");
    when(client.uploadContent(eq("pic.jpg"), any(), any(), any(Long.class), any(),
        any(Boolean.class), eq("amazon-album-1"))).thenReturn(node);

    importer.importItem(jobId, executor, authData, resource);

    verify(client).uploadContent(eq("pic.jpg"), any(), any(), any(Long.class), any(),
        any(Boolean.class), eq("amazon-album-1"));
  }

  private void stubDownload() throws Exception {
    InputStream stream = new java.io.ByteArrayInputStream(new byte[]{1, 2, 3});
    when(dataStore.getStream(eq(jobId), eq("tempkey")))
        .thenReturn(new TemporaryPerJobDataStore.InputStreamWrapper(stream));
    File tempFile = File.createTempFile("test", ".tmp");
    tempFile.deleteOnExit();
    when(dataStore.getTempFileFromInputStream(any(), any(), any())).thenReturn(tempFile);
  }

  private void executorRunsCallable() throws Exception {
    when(executor.executeAndSwallowIOExceptions(any(), any(), any()))
        .thenAnswer(invocation ->
            ((java.util.concurrent.Callable<?>) invocation.getArgument(2)).call());
  }
}
