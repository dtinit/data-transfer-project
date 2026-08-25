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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.transfer.JobMetadata;
import org.datatransferproject.transfer.amazon.photos.model.AmazonPhotosNode;
import org.datatransferproject.types.common.models.FavoriteInfo;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
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
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class AmazonPhotosImporterTest {

  @Mock private Monitor monitor;
  @Mock private TemporaryPerJobDataStore dataStore;
  @Mock private IdempotentImportExecutor executor;
  @Mock private IdempotentImportExecutor retryingExecutor;
  @Mock private AmazonPhotosInterface client;

  private TokensAndUrlAuthData authData;
  private AmazonPhotosImporter importer;
  private UUID jobId;

  @BeforeEach
  void setUp() {
    authData = new TokensAndUrlAuthData("access", "refresh", "http://token-url");
    importer = new AmazonPhotosImporter(monitor, dataStore, client);
    jobId = UUID.randomUUID();
  }

  @Test
  void importItem_createsAlbums() throws Exception {
    PhotoAlbum album = new PhotoAlbum("a1", "Vacation", null);
    PhotosContainerResource resource = new PhotosContainerResource(
        Collections.singletonList(album), Collections.emptyList());

    ImportResult result = importer.importItem(jobId, executor, authData, resource);

    assertEquals(ImportResult.OK, result);
    verify(executor).executeAndSwallowIOExceptions(eq("a1"), eq("Vacation"), any());
  }

  @Test
  void importItem_registersPhotosWithExecutor() throws Exception {
    PhotoAlbum album = new PhotoAlbum("a1", "Album", null);
    PhotoModel photo1 = new PhotoModel("pic1.jpg", "http://example.com/1.jpg",
        null, "image/jpeg", "p1", "a1", false);
    PhotoModel photo2 = new PhotoModel("pic2.jpg", "http://example.com/2.jpg",
        null, "image/jpeg", "p2", "a1", false);

    PhotosContainerResource resource = new PhotosContainerResource(
        ImmutableList.of(album), ImmutableList.of(photo1, photo2));

    ImportResult result = importer.importItem(jobId, executor, authData, resource);

    assertEquals(ImportResult.OK, result);
    // Album + both photos are registered with the executor under their own keys/names.
    verify(executor).executeAndSwallowIOExceptions(eq("a1"), eq("Album"), any());
    verify(executor).executeAndSwallowIOExceptions(
        eq(photo1.getIdempotentId()), eq("pic1.jpg"), any());
    verify(executor).executeAndSwallowIOExceptions(
        eq(photo2.getIdempotentId()), eq("pic2.jpg"), any());
    // ...and nothing else.
    verify(executor, times(3)).executeAndSwallowIOExceptions(any(), any(), any());
  }

  @Test
  void importItem_emptyResource() throws Exception {
    PhotosContainerResource resource = new PhotosContainerResource(
        Collections.emptyList(), Collections.emptyList());

    ImportResult result = importer.importItem(jobId, executor, authData, resource);

    assertEquals(ImportResult.OK, result);
    verify(executor, never()).executeAndSwallowIOExceptions(any(), any(), any());
    verifyNoInteractions(client);
  }

  @Test
  void importItem_usesAlbumIdAsExecutorKey() throws Exception {
    PhotoAlbum album = new PhotoAlbum("google-album-id", "My Album", null);
    PhotosContainerResource resource = new PhotosContainerResource(
        Collections.singletonList(album), Collections.emptyList());

    importer.importItem(jobId, executor, authData, resource);

    verify(executor).executeAndSwallowIOExceptions(eq("google-album-id"), eq("My Album"), any());
  }

  @Test
  void importItem_usesIdempotentIdForPhotos() throws Exception {
    PhotoModel photo = new PhotoModel("pic.jpg", "http://example.com/pic.jpg",
        null, "image/jpeg", "photo-data-id", "a1", false);

    PhotosContainerResource resource = new PhotosContainerResource(
        Collections.emptyList(), Collections.singletonList(photo));

    importer.importItem(jobId, executor, authData, resource);

    verify(executor).executeAndSwallowIOExceptions(
        eq(photo.getIdempotentId()), eq("pic.jpg"), any());
  }

  // Verifies that path separators in photo titles do not affect filesystem operations.
  // The importer uses dataId (not title) as the temp-file prefix, so hostile titles
  // cannot cause path traversal. This test confirms the title is passed as-is to the
  // executor while the safe dataId is used for file operations.
  @Test
  void importItem_hostileTitleUsesDataIdForFileSystem() throws Exception {
    PhotoModel photo = new PhotoModel("../../etc/passwd", "tempkey",
        null, "image/jpeg", "safe-data-id", "a1", true, (Date) null);

    PhotosContainerResource resource = new PhotosContainerResource(
        Collections.emptyList(), Collections.singletonList(photo));

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
        .thenReturn(new org.datatransferproject.transfer.amazon.photos.model.AmazonPhotosNode());

    importer.importItem(jobId, executor, authData, resource);

    // Title with path separators is passed verbatim to executor (not a filesystem sink)
    verify(executor).executeAndSwallowIOExceptions(
        eq(photo.getIdempotentId()), eq("../../etc/passwd"), any());

    // But the temp-file prefix uses dataId, not the title
    org.mockito.ArgumentCaptor<String> prefixCaptor =
        org.mockito.ArgumentCaptor.forClass(String.class);
    verify(dataStore).getTempFileFromInputStream(any(), prefixCaptor.capture(), eq(".tmp"));
    assertEquals("safe-data-id", prefixCaptor.getValue());
  }

  @Test
  void importItem_filenameLongerThan255Chars_registersWithExecutor() throws Exception {
    String longName = "a".repeat(300) + ".jpg";
    PhotoModel photo = new PhotoModel(longName, "http://example.com/long.jpg",
        null, "image/jpeg", "long-id", "a1", false);

    PhotosContainerResource resource = new PhotosContainerResource(
        Collections.emptyList(), Collections.singletonList(photo));

    importer.importItem(jobId, executor, authData, resource);

    // Title gets truncated to 200 chars by transmogrification
    String expectedTitle = "a".repeat(200);
    verify(executor).executeAndSwallowIOExceptions(
        eq(photo.getIdempotentId()), eq(expectedTitle), any());
  }

  // ---------------------------------------------------------------------------
  // Path traversal protection tests for dataId.
  //
  // The importer builds temp-file prefixes from photo.getDataId() via
  //   prefix = dataId.replaceAll("[/\\\\]", "_")
  // These tests inject hostile path values into dataId and assert:
  //   1. The prefix contains no path separators after sanitization.
  //   2. Files.createTempFile keeps the file inside java.io.tmpdir.
  // ---------------------------------------------------------------------------

  private String captureTempPrefixForDataId(String dataId) throws Exception {
    PhotoModel photo = new PhotoModel("title.jpg", "tempkey",
        null, "image/jpeg", dataId, "a1", true, (Date) null);
    PhotosContainerResource resource = new PhotosContainerResource(
        Collections.emptyList(), Collections.singletonList(photo));

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
        .thenReturn(new org.datatransferproject.transfer.amazon.photos.model.AmazonPhotosNode());

    importer.importItem(jobId, executor, authData, resource);

    org.mockito.ArgumentCaptor<String> prefixCaptor =
        org.mockito.ArgumentCaptor.forClass(String.class);
    verify(dataStore).getTempFileFromInputStream(any(), prefixCaptor.capture(), eq(".tmp"));
    return prefixCaptor.getValue();
  }

  /** The sanitized prefix must never re-introduce a path separator and must stay in tmpdir. */
  private void assertPrefixIsContained(String prefix) throws Exception {
    assertFalse(prefix.contains("/"), "Prefix should not contain forward slash: " + prefix);
    assertFalse(prefix.contains("\\"), "Prefix should not contain backslash: " + prefix);
    // Containment: the real JDK sink must accept the prefix AND keep the file
    // inside java.io.tmpdir. createTempFile prepends the prefix to a random name,
    // so the created file's parent must be exactly the temp directory.
    File created = File.createTempFile(prefix, ".tmp");
    created.deleteOnExit();
    File tmpDir = new File(System.getProperty("java.io.tmpdir")).getCanonicalFile();
    assertEquals(tmpDir, created.getCanonicalFile().getParentFile(),
        "Temp file escaped the temp directory: " + created.getCanonicalPath());
  }

  @Test
  void downloadToTempFile_forwardSlashTraversalInDataId_isContained() throws Exception {
    assertPrefixIsContained(captureTempPrefixForDataId("../../../../etc/passwd"));
  }

  @Test
  void downloadToTempFile_backslashTraversalInDataId_isContained() throws Exception {
    assertPrefixIsContained(captureTempPrefixForDataId("..\\..\\..\\windows\\system32"));
  }

  @Test
  void downloadToTempFile_absolutePathInDataId_isContained() throws Exception {
    assertPrefixIsContained(captureTempPrefixForDataId("/etc/cron.d/evil"));
  }

  @Test
  void downloadToTempFile_mixedSeparatorTraversalInDataId_isContained() throws Exception {
    assertPrefixIsContained(captureTempPrefixForDataId("..\\/..\\/secret"));
  }

  @Test
  void downloadToTempFile_urlEncodedSeparatorInDataId_isContained() throws Exception {
    // The regex strips literal separators only; %2F is NOT decoded by the JDK sink,
    // so it is harmless here -- this test documents/locks that behavior.
    assertPrefixIsContained(captureTempPrefixForDataId("..%2F..%2Fetc%2Fpasswd"));
  }

  @Test
  void downloadToTempFile_dotSegmentsWithoutSeparatorInDataId_isContained() throws Exception {
    assertPrefixIsContained(captureTempPrefixForDataId("....etc....passwd"));
  }

  // Verifies that if album creation failed, photo upload also fails rather than
  // silently uploading without album (preventing data loss of album organization).
  @Test
  void importItem_albumCreationFailed_photoUploadAlsoFails() throws Exception {
    PhotoModel photo = new PhotoModel("pic.jpg", "tempkey",
        null, "image/jpeg", "photo-1", "failed-album", true, (Date) null);

    PhotosContainerResource resource = new PhotosContainerResource(
        Collections.emptyList(), Collections.singletonList(photo));

    when(executor.getCachedValue("failed-album"))
        .thenThrow(new IllegalStateException("Album creation failed"));
    when(executor.executeAndSwallowIOExceptions(any(), any(), any()))
        .thenAnswer(invocation -> {
          java.util.concurrent.Callable<?> callable = invocation.getArgument(2);
          try {
            return callable.call();
          } catch (Exception e) {
            return null;
          }
        });

    importer.importItem(jobId, executor, authData, resource);

    verify(client, never())
        .uploadContent(any(), any(), any(), any(Long.class), any(), any(Boolean.class), any());
  }

  // ---------------------------------------------------------------------------
  // Retrying-executor selection (platform retry opt-in).
  // ---------------------------------------------------------------------------

  @Test
  void importItem_usesRetryingExecutorWhenEnabled() throws Exception {
    AmazonPhotosImporter retryingImporter =
        new AmazonPhotosImporter(monitor, dataStore, client, retryingExecutor, true);
    PhotoAlbum album = new PhotoAlbum("a1", "Album", null);
    PhotosContainerResource resource = new PhotosContainerResource(
        Collections.singletonList(album), Collections.emptyList());

    retryingImporter.importItem(jobId, executor, authData, resource);

    verify(retryingExecutor).executeAndSwallowIOExceptions(eq("a1"), eq("Album"), any());
    verifyNoInteractions(executor);
  }

  @Test
  void importItem_usesDefaultExecutorWhenRetryingDisabled() throws Exception {
    AmazonPhotosImporter retryingImporter =
        new AmazonPhotosImporter(monitor, dataStore, client, retryingExecutor, false);
    PhotoAlbum album = new PhotoAlbum("a1", "Album", null);
    PhotosContainerResource resource = new PhotosContainerResource(
        Collections.singletonList(album), Collections.emptyList());

    retryingImporter.importItem(jobId, executor, authData, resource);

    verify(executor).executeAndSwallowIOExceptions(eq("a1"), eq("Album"), any());
    verifyNoInteractions(retryingExecutor);
  }

  @Test
  void importItem_usesDefaultExecutorWhenNoRetryingExecutorProvided() throws Exception {
    AmazonPhotosImporter retryingImporter =
        new AmazonPhotosImporter(monitor, dataStore, client, null, true);
    PhotoAlbum album = new PhotoAlbum("a1", "Album", null);
    PhotosContainerResource resource = new PhotosContainerResource(
        Collections.singletonList(album), Collections.emptyList());

    retryingImporter.importItem(jobId, executor, authData, resource);

    verify(executor).executeAndSwallowIOExceptions(eq("a1"), eq("Album"), any());
  }

  // ---------------------------------------------------------------------------
  // Storage-quota classification -> terminal DestinationMemoryFullException.
  // ---------------------------------------------------------------------------

  @Test
  void importItem_noActiveSubscription_throwsDestinationMemoryFull() throws Exception {
    PhotoModel photo = new PhotoModel("pic.jpg", "tempkey",
        null, "image/jpeg", "p1", null, true, (Date) null);
    PhotosContainerResource resource = new PhotosContainerResource(
        Collections.emptyList(), Collections.singletonList(photo));

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
    PhotoAlbum album = new PhotoAlbum("a1", "Vacation", "desc");
    PhotosContainerResource resource = new PhotosContainerResource(
        Collections.singletonList(album), Collections.emptyList());

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
    PhotosContainerResource resource = new PhotosContainerResource(
        Collections.emptyList(), Collections.singletonList(photo));

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

  @Test
  void importItem_uploadPhoto_duplicate_isSkippedWithoutError() throws Exception {
    PhotoModel photo = new PhotoModel("pic.jpg", "tempkey",
        null, "image/jpeg", "p1", null, true, (Date) null);
    PhotosContainerResource resource = new PhotosContainerResource(
        Collections.emptyList(), Collections.singletonList(photo));

    stubDownload();
    executorRunsCallable();
    when(client.uploadContent(any(), any(), any(), any(Long.class), any(), any(Boolean.class), any()))
        .thenThrow(new AmazonPhotosApiException(409, "DuplicatesConflictError", "dup"));

    // Duplicate is swallowed as success -> importItem completes without throwing.
    ImportResult result = importer.importItem(jobId, executor, authData, resource);

    assertEquals(ImportResult.OK, result);
    verify(client).uploadContent(eq("pic.jpg"), any(), any(), any(Long.class), any(), any(Boolean.class), any());
  }

  @Test
  void importItem_uploadPhoto_nonDuplicateNonQuotaError_propagates() throws Exception {
    PhotoModel photo = new PhotoModel("pic.jpg", "tempkey",
        null, "image/jpeg", "p1", null, true, (Date) null);
    PhotosContainerResource resource = new PhotosContainerResource(
        Collections.emptyList(), Collections.singletonList(photo));

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
    assertNotNull(new AmazonPhotosImporter(
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
