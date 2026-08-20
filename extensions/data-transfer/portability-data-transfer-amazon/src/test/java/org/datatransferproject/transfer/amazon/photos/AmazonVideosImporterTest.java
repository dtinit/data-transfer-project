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
import org.datatransferproject.types.common.models.videos.VideoAlbum;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
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
public class AmazonVideosImporterTest {

  @Mock private Monitor monitor;
  @Mock private TemporaryPerJobDataStore dataStore;
  @Mock private IdempotentImportExecutor executor;
  @Mock private IdempotentImportExecutor retryingExecutor;
  @Mock private AmazonPhotosInterface client;

  private TokensAndUrlAuthData authData;
  private AmazonVideosImporter importer;
  private UUID jobId;

  @BeforeEach
  void setUp() {
    authData = new TokensAndUrlAuthData("access", "refresh", "http://token-url");
    importer = new AmazonVideosImporter(monitor, dataStore, client);
    jobId = UUID.randomUUID();
  }

  @Test
  void importItem_createsAlbums() throws Exception {
    VideoAlbum album = new VideoAlbum("album1", "Vacation Videos", "My videos");
    VideosContainerResource resource = new VideosContainerResource(
        List.of(album), Collections.emptyList());

    importer.importItem(jobId, executor, authData, resource);

    verify(executor).executeAndSwallowIOExceptions(
        eq("album1"), eq("Vacation Videos"), any());
  }

  @Test
  void importItem_registersVideosWithExecutor() throws Exception {
    VideoModel video = new VideoModel("clip.mp4", "http://example.com/clip.mp4",
        "A video", "video/mp4", "vid-1", "album1", false, null);

    VideosContainerResource resource = new VideosContainerResource(
        Collections.emptyList(), List.of(video));

    importer.importItem(jobId, executor, authData, resource);

    verify(executor).executeAndSwallowIOExceptions(
        eq(video.getIdempotentId()), eq("clip.mp4"), any());
  }

  @Test
  void importItem_emptyResource() throws Exception {
    VideosContainerResource resource = new VideosContainerResource(
        Collections.emptyList(), Collections.emptyList());

    ImportResult result = importer.importItem(jobId, executor, authData, resource);

    assertEquals(ImportResult.OK, result);
  }

  @Test
  void importItem_uploadsVideoWithAlbumId() throws Exception {
    VideoModel video = new VideoModel("clip.mp4", "tempkey",
        "A video", "video/mp4", "vid-1", "album1", true, null);

    VideosContainerResource resource = new VideosContainerResource(
        Collections.emptyList(), List.of(video));

    InputStream fakeStream = new java.io.ByteArrayInputStream(new byte[]{1, 2, 3});
    when(dataStore.getStream(eq(jobId), eq("tempkey")))
        .thenReturn(new TemporaryPerJobDataStore.InputStreamWrapper(fakeStream));
    File tempFile = File.createTempFile("test", ".tmp");
    tempFile.deleteOnExit();
    when(dataStore.getTempFileFromInputStream(any(), any(), any())).thenReturn(tempFile);
    when(executor.executeAndSwallowIOExceptions(any(), any(), any()))
        .thenAnswer(invocation -> {
          java.util.concurrent.Callable<?> callable = invocation.getArgument(2);
          return callable.call();
        });
    when(executor.getCachedValue("album1")).thenReturn("amazon-album-id");
    when(client.uploadContent(any(), any(), any(), any(Long.class), any(), any(Boolean.class), eq("amazon-album-id")))
        .thenReturn(new AmazonPhotosNode());

    importer.importItem(jobId, executor, authData, resource);

    // Verifies albumId is passed to uploadContent
    verify(client).uploadContent(eq("clip.mp4"), any(), any(), any(Long.class), any(), any(Boolean.class), eq("amazon-album-id"));
  }

  @Test
  void importItem_uploadsVideoWithoutAlbumWhenNoneProvided() throws Exception {
    VideoModel video = new VideoModel("clip.mp4", "tempkey",
        "A video", "video/mp4", "vid-1", null, true, null);

    VideosContainerResource resource = new VideosContainerResource(
        Collections.emptyList(), List.of(video));

    InputStream fakeStream = new java.io.ByteArrayInputStream(new byte[]{1, 2, 3});
    when(dataStore.getStream(eq(jobId), eq("tempkey")))
        .thenReturn(new TemporaryPerJobDataStore.InputStreamWrapper(fakeStream));
    File tempFile = File.createTempFile("test", ".tmp");
    tempFile.deleteOnExit();
    when(dataStore.getTempFileFromInputStream(any(), any(), any())).thenReturn(tempFile);
    when(executor.executeAndSwallowIOExceptions(any(), any(), any()))
        .thenAnswer(invocation -> {
          java.util.concurrent.Callable<?> callable = invocation.getArgument(2);
          return callable.call();
        });
    when(client.uploadContent(any(), any(), any(), any(Long.class), any(), any(Boolean.class), eq(null)))
        .thenReturn(new AmazonPhotosNode());

    importer.importItem(jobId, executor, authData, resource);

    // Verifies null albumId when no album provided
    verify(client).uploadContent(eq("clip.mp4"), any(), any(), any(Long.class), any(), any(Boolean.class), eq(null));
  }

  // Path traversal protection: dataId is used as temp-file prefix, separators are sanitized.
  @Test
  void downloadToTempFile_sanitizesDataIdForPrefix() throws Exception {
    VideoModel video = new VideoModel("clip.mp4", "tempkey",
        "A video", "video/mp4", "../../etc/passwd", null, true, null);

    VideosContainerResource resource = new VideosContainerResource(
        Collections.emptyList(), List.of(video));

    InputStream fakeStream = new java.io.ByteArrayInputStream(new byte[]{1, 2, 3});
    when(dataStore.getStream(eq(jobId), eq("tempkey")))
        .thenReturn(new TemporaryPerJobDataStore.InputStreamWrapper(fakeStream));
    File tempFile = File.createTempFile("test", ".tmp");
    tempFile.deleteOnExit();
    when(dataStore.getTempFileFromInputStream(any(), any(), any())).thenReturn(tempFile);
    when(executor.executeAndSwallowIOExceptions(any(), any(), any()))
        .thenAnswer(invocation -> {
          java.util.concurrent.Callable<?> callable = invocation.getArgument(2);
          return callable.call();
        });
    when(client.uploadContent(any(), any(), any(), any(Long.class), any(), any(Boolean.class), any()))
        .thenReturn(new AmazonPhotosNode());

    importer.importItem(jobId, executor, authData, resource);

    org.mockito.ArgumentCaptor<String> prefixCaptor =
        org.mockito.ArgumentCaptor.forClass(String.class);
    verify(dataStore).getTempFileFromInputStream(any(), prefixCaptor.capture(), eq(".tmp"));
    String prefix = prefixCaptor.getValue();
    assertEquals(".._.._etc_passwd", prefix);
  }

  // Verifies that if album creation failed, video upload also fails rather than
  // silently uploading without album (preventing data loss of album organization).
  @Test
  void importItem_albumCreationFailed_videoUploadAlsoFails() throws Exception {
    VideoModel video = new VideoModel("clip.mp4", "tempkey",
        "A video", "video/mp4", "vid-1", "failed-album", true, null);

    VideosContainerResource resource = new VideosContainerResource(
        Collections.emptyList(), List.of(video));

    when(executor.getCachedValue("failed-album"))
        .thenThrow(new IllegalStateException("Album creation failed"));
    // Simulate real executor: invokes callable, catches exception, records error
    when(executor.executeAndSwallowIOExceptions(any(), any(), any()))
        .thenAnswer(invocation -> {
          java.util.concurrent.Callable<?> callable = invocation.getArgument(2);
          try {
            return callable.call();
          } catch (Exception e) {
            // Executor swallows and records — upload never completes
            return null;
          }
        });

    importer.importItem(jobId, executor, authData, resource);

    // uploadContent should never be called since album resolution threw before upload
    verify(client, org.mockito.Mockito.never())
        .uploadContent(any(), any(), any(), any(Long.class), any(), any(Boolean.class), any());
  }

  // ---------------------------------------------------------------------------
  // Retrying-executor selection (platform retry opt-in).
  // ---------------------------------------------------------------------------

  @Test
  void importItem_usesRetryingExecutorWhenEnabled() throws Exception {
    AmazonVideosImporter retryingImporter =
        new AmazonVideosImporter(monitor, dataStore, client, retryingExecutor, true);
    VideoAlbum album = new VideoAlbum("album1", "Vacation Videos", "My videos");
    VideosContainerResource resource = new VideosContainerResource(
        List.of(album), Collections.emptyList());

    retryingImporter.importItem(jobId, executor, authData, resource);

    verify(retryingExecutor).executeAndSwallowIOExceptions(
        eq("album1"), eq("Vacation Videos"), any());
    verifyNoInteractions(executor);
  }

  @Test
  void importItem_usesDefaultExecutorWhenRetryingDisabled() throws Exception {
    AmazonVideosImporter retryingImporter =
        new AmazonVideosImporter(monitor, dataStore, client, retryingExecutor, false);
    VideoAlbum album = new VideoAlbum("album1", "Vacation Videos", "My videos");
    VideosContainerResource resource = new VideosContainerResource(
        List.of(album), Collections.emptyList());

    retryingImporter.importItem(jobId, executor, authData, resource);

    verify(executor).executeAndSwallowIOExceptions(eq("album1"), eq("Vacation Videos"), any());
    verifyNoInteractions(retryingExecutor);
  }

  @Test
  void importItem_usesDefaultExecutorWhenNoRetryingExecutorProvided() throws Exception {
    AmazonVideosImporter retryingImporter =
        new AmazonVideosImporter(monitor, dataStore, client, null, true);
    VideoAlbum album = new VideoAlbum("album1", "Vacation Videos", "My videos");
    VideosContainerResource resource = new VideosContainerResource(
        List.of(album), Collections.emptyList());

    retryingImporter.importItem(jobId, executor, authData, resource);

    verify(executor).executeAndSwallowIOExceptions(eq("album1"), eq("Vacation Videos"), any());
  }

  // ---------------------------------------------------------------------------
  // Storage-quota classification -> terminal DestinationMemoryFullException.
  // ---------------------------------------------------------------------------

  @Test
  void importItem_noActiveSubscription_throwsDestinationMemoryFull() throws Exception {
    VideoModel video = new VideoModel("clip.mp4", "tempkey",
        "A video", "video/mp4", "vid-1", null, true, null);
    VideosContainerResource resource = new VideosContainerResource(
        Collections.emptyList(), List.of(video));

    InputStream fakeStream = new java.io.ByteArrayInputStream(new byte[]{1, 2, 3});
    when(dataStore.getStream(eq(jobId), eq("tempkey")))
        .thenReturn(new TemporaryPerJobDataStore.InputStreamWrapper(fakeStream));
    File tempFile = File.createTempFile("test", ".tmp");
    tempFile.deleteOnExit();
    when(dataStore.getTempFileFromInputStream(any(), any(), any())).thenReturn(tempFile);
    when(executor.executeAndSwallowIOExceptions(any(), any(), any()))
        .thenAnswer(invocation -> {
          java.util.concurrent.Callable<?> callable = invocation.getArgument(2);
          return callable.call();
        });
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
    VideoAlbum album = new VideoAlbum("album1", "Trips", "desc");
    VideosContainerResource resource = new VideosContainerResource(
        List.of(album), Collections.emptyList());

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
    assertEquals("Trips" + AmazonImportHelper.IMPORTED_SUFFIX + "Google", name.getValue());
  }

  // ---------------------------------------------------------------------------
  // uploadVideo happy path (covers uploadedTime mapping, upload, temp cleanup).
  // ---------------------------------------------------------------------------

  @Test
  void importItem_uploadVideo_happyPath_withUploadedTime() throws Exception {
    Date uploaded = new Date(1_700_000_000_000L);
    VideoModel video = new VideoModel("clip.mp4", "tempkey", "A video", "video/mp4",
        "vid-1", null, true, uploaded);
    VideosContainerResource resource = new VideosContainerResource(
        Collections.emptyList(), List.of(video));

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
  void importItem_uploadVideo_duplicate_isSkippedWithoutError() throws Exception {
    VideoModel video = new VideoModel("clip.mp4", "tempkey",
        "A video", "video/mp4", "vid-1", null, true, null);
    VideosContainerResource resource = new VideosContainerResource(
        Collections.emptyList(), List.of(video));

    stubDownload();
    executorRunsCallable();
    when(client.uploadContent(any(), any(), any(), any(Long.class), any(), any(Boolean.class), any()))
        .thenThrow(new AmazonPhotosApiException(409, "DuplicatesConflictError", "dup"));

    ImportResult result = importer.importItem(jobId, executor, authData, resource);

    assertEquals(ImportResult.OK, result);
    verify(client).uploadContent(eq("clip.mp4"), any(), any(), any(Long.class), any(), any(Boolean.class), any());
  }

  @Test
  void importItem_uploadVideo_nonDuplicateNonQuotaError_propagates() throws Exception {
    VideoModel video = new VideoModel("clip.mp4", "tempkey",
        "A video", "video/mp4", "vid-1", null, true, null);
    VideosContainerResource resource = new VideosContainerResource(
        Collections.emptyList(), List.of(video));

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
    assertNotNull(new AmazonVideosImporter(
        monitor, "client-id", "client-secret", dataStore, null, false));
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
