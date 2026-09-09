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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/** Unit tests for {@link AmazonImportHelper} error classification and client provisioning. */
class AmazonImportHelperTest {

  private static final TokensAndUrlAuthData AUTH_DATA =
      new TokensAndUrlAuthData("access", "refresh", "http://token-url");

  private AmazonImportHelper helper;

  @BeforeEach
  void setUp() {
    helper = new AmazonImportHelper(mock(TemporaryPerJobDataStore.class));
  }

  private static AmazonPhotosApiException error(int status, String errorCode) {
    return new AmazonPhotosApiException(status, errorCode, "message");
  }

  @Test
  void isStorageQuotaExceeded_insufficientStorage_isTrue() {
    assertTrue(helper.isStorageQuotaExceeded(error(403, UploadErrorCodes.INSUFFICIENT_STORAGE)));
  }

  @Test
  void isStorageQuotaExceeded_noActiveSubscriptionFound_isTrue() {
    assertTrue(helper.isStorageQuotaExceeded(
        error(403, UploadErrorCodes.NO_ACTIVE_SUBSCRIPTION_FOUND)));
  }

  @Test
  void isStorageQuotaExceeded_otherErrorCode_isFalse() {
    assertFalse(helper.isStorageQuotaExceeded(error(403, "ForbiddenAccess")));
  }

  @Test
  void isStorageQuotaExceeded_nullErrorCode_isFalse() {
    assertFalse(helper.isStorageQuotaExceeded(error(500, null)));
  }

  @Test
  void isDuplicate_duplicatesConflictError_isTrue() {
    assertTrue(helper.isDuplicate(error(409, UploadErrorCodes.DUPLICATES_CONFLICT_ERROR)));
  }

  @Test
  void isDuplicate_otherErrorCode_isFalse() {
    assertFalse(helper.isDuplicate(error(409, UploadErrorCodes.INSUFFICIENT_STORAGE)));
  }

  // ---------------------------------------------------------------------------
  // Client provisioning: per-job isolation + injected/test seam.
  // ---------------------------------------------------------------------------

  @Test
  void getOrCreateClient_returnsInjectedClientWithoutBuilding() throws Exception {
    AmazonPhotosInterface injected = mock(AmazonPhotosInterface.class);
    AmazonImportHelper h =
        new AmazonImportHelper(mock(TemporaryPerJobDataStore.class), injected, mock(Monitor.class));

    assertEquals(injected, h.getOrCreateClient(UUID.randomUUID(), AUTH_DATA));
    // Injected clients are supplied ready-to-use; the helper must not resolve endpoints itself.
    verify(injected, never()).resolveEndpoints();
  }

  @Test
  void getOrCreateClient_isolatesClientPerJobAndReusesWithinJob() throws Exception {
    AmazonImportHelper h = spy(new AmazonImportHelper(
        mock(TemporaryPerJobDataStore.class), "client-id", "client-secret", mock(Monitor.class)));
    AmazonPhotosInterface built = mock(AmazonPhotosInterface.class);
    doReturn(built).when(h).createClient(eq(AUTH_DATA));
    UUID jobA = UUID.randomUUID();
    UUID jobB = UUID.randomUUID();

    // Same job: the client is built once and reused across chunks (endpoints resolved once).
    assertEquals(built, h.getOrCreateClient(jobA, AUTH_DATA));
    assertEquals(built, h.getOrCreateClient(jobA, AUTH_DATA));
    verify(h, times(1)).createClient(eq(AUTH_DATA));
    verify(built, times(1)).resolveEndpoints();

    // Different job: a fresh client is built, so one job's credentials never serve another.
    h.getOrCreateClient(jobB, AUTH_DATA);
    verify(h, times(2)).createClient(eq(AUTH_DATA));
  }

  @Test
  void createClient_buildsClientWithoutNetwork() {
    AmazonImportHelper h = new AmazonImportHelper(
        mock(TemporaryPerJobDataStore.class), "client-id", "client-secret", mock(Monitor.class));
    // Constructing the client does no network I/O (endpoint resolution is a separate call).
    assertNotNull(h.createClient(AUTH_DATA));
  }

  // ---------------------------------------------------------------------------
  // Album resolution: items resolve their parent album from the executor cache, which is
  // populated because albums are always created before any item that references them.
  // ---------------------------------------------------------------------------

  @Test
  void resolveTargetAlbumId_nullAlbumId_returnsNull() throws Exception {
    IdempotentImportExecutor executor = mock(IdempotentImportExecutor.class);
    // An albumless item has no parent to resolve.
    assertNull(helper.resolveTargetAlbumId(null, executor));
    verifyNoInteractions(executor);
  }

  @Test
  void resolveTargetAlbumId_cachedAlbum_returnsAmazonNodeId() throws Exception {
    IdempotentImportExecutor executor = mock(IdempotentImportExecutor.class);
    // Album was created earlier this job, so its source id maps to the Amazon node id.
    when(executor.getCachedValue("google-album")).thenReturn("amazon-node-1");
    assertEquals("amazon-node-1", helper.resolveTargetAlbumId("google-album", executor));
  }

  @Test
  void resolveTargetAlbumId_uncachedAlbum_propagates() throws Exception {
    IdempotentImportExecutor executor = mock(IdempotentImportExecutor.class);
    // No silent root fallback: an album id that wasn't created (not cached) fails the item so its
    // album organization isn't silently lost, rather than reassigning it to some default album.
    when(executor.getCachedValue("missing")).thenThrow(new IllegalArgumentException("not found"));
    assertThrows(IllegalArgumentException.class,
        () -> helper.resolveTargetAlbumId("missing", executor));
  }
}
