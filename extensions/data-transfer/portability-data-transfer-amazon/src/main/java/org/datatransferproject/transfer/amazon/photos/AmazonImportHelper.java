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

import org.datatransferproject.spi.cloud.connection.ConnectionProvider;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.types.common.DownloadableItem;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shared helper for Amazon Photos/Videos importers.
 * Holds common collaborators and provides reusable import operations.
 */
class AmazonImportHelper {

  static final String IMPORTED_SUFFIX = " - Imported from ";

  private final TemporaryPerJobDataStore dataStore;
  private final ConnectionProvider connectionProvider;
  private final String clientId;
  private final String clientSecret;
  private final Monitor monitor;
  // Test seam: when set it is always returned in place of a real client.
  private final AmazonPhotosInterface injectedClient;
  // Safeguard to avoid sharing a client across jobs: keying by jobId keeps each job's credentials
  // separate. Bounded, access-order LRU so a long-lived (multi-job) process can't accumulate
  // clients/tokens without limit; an evicted job just rebuilds its client on its next chunk.
  // Guarded by getOrCreateClient (synchronized).
  private static final int MAX_CACHED_CLIENTS = 1000;
  private final Map<UUID, AmazonPhotosInterface> clientsByJob =
      new LinkedHashMap<UUID, AmazonPhotosInterface>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, AmazonPhotosInterface> eldest) {
          return size() > MAX_CACHED_CLIENTS;
        }
      };

  /** Classification/download-only helper (no client provisioning); used where no client is built. */
  AmazonImportHelper(TemporaryPerJobDataStore dataStore) {
    this(dataStore, null, null, null, null);
  }

  /** Production helper that builds a real Amazon client per job from the given app credentials. */
  AmazonImportHelper(TemporaryPerJobDataStore dataStore, String clientId, String clientSecret,
                     Monitor monitor) {
    this(dataStore, clientId, clientSecret, monitor, null);
  }

  /** Test helper: {@code injectedClient} is always returned by {@link #getOrCreateClient}. */
  AmazonImportHelper(TemporaryPerJobDataStore dataStore, AmazonPhotosInterface injectedClient) {
    this(dataStore, null, null, null, injectedClient);
  }

  private AmazonImportHelper(TemporaryPerJobDataStore dataStore, String clientId,
                             String clientSecret, Monitor monitor,
                             AmazonPhotosInterface injectedClient) {
    this.dataStore = dataStore;
    this.connectionProvider = new ConnectionProvider(dataStore);
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.monitor = monitor;
    this.injectedClient = injectedClient;
  }

  /**
   * Returns the client for this job, creating and caching it (and resolving endpoints) on first
   * use. Injected clients are returned as-is; per-job caching keeps each job's credentials
   * isolated and avoids re-resolving endpoints on every chunk of the same job.
   */
  AmazonPhotosInterface getOrCreateClient(UUID jobId, TokensAndUrlAuthData authData)
      throws IOException {
    if (injectedClient != null) {
      return injectedClient;
    }
    synchronized (this) {
      AmazonPhotosInterface existing = clientsByJob.get(jobId);
      if (existing != null) {
        return existing;
      }
    }
    // Build and resolve endpoints outside the lock: resolveEndpoints() is a network call, so
    // holding the shared lock across it would let one job's latency stall getOrCreateClient for
    // every other concurrent job. Two concurrent first-chunks of the same new job may each build
    // once; putIfAbsent dedupes storage and the loser is discarded.
    AmazonPhotosInterface created = createClient(authData);
    created.resolveEndpoints();
    synchronized (this) {
      AmazonPhotosInterface race = clientsByJob.putIfAbsent(jobId, created);
      return race != null ? race : created;
    }
  }

  // Visible-for-testing seam: endpoint resolution otherwise requires a live network call, so
  // tests override this to inject a fake client.
  AmazonPhotosInterface createClient(TokensAndUrlAuthData authData) {
    return new AmazonPhotosClient(
        AmazonPhotosClient.createDefaultHttpClient(),
        authData.getAccessToken(), authData.getRefreshToken(), clientId, clientSecret, monitor);
  }

  /** Creates a new MD5 MessageDigest instance. */
  MessageDigest newMd5Digest() {
    return Md5Utils.newDigest();
  }

  /** Converts a byte array to its lowercase hex string representation. */
  String toHexString(byte[] bytes) {
    return Md5Utils.toHexString(bytes);
  }

  /** Whether the API error indicates the item already exists (duplicate). */
  boolean isDuplicate(AmazonPhotosApiException e) {
    return e.isErrorCode(UploadErrorCodes.DUPLICATES_CONFLICT_ERROR);
  }

  /** Whether the API error indicates the destination storage quota is exceeded. */
  boolean isStorageQuotaExceeded(AmazonPhotosApiException e) {
    return e.isErrorCode(UploadErrorCodes.INSUFFICIENT_STORAGE)
        || e.isErrorCode(UploadErrorCodes.NO_ACTIVE_SUBSCRIPTION_FOUND);
  }

  /**
   * Downloads a content item to a temp file, computing MD5 in a single pass.
   *
   * <p>The provider-supplied {@code dataId} is untrusted filesystem input, so path separators are
   * stripped before using it as the temp-file prefix — otherwise {@code Files.createTempFile}
   * rejects it with an IllegalArgumentException. Only the local prefix is affected; the content
   * sent to Amazon is unchanged.
   */
  File downloadToTempFile(UUID jobId, DownloadableItem item, String dataId,
                          MessageDigest md5) throws IOException {
    String prefix = dataId.replaceAll("[/\\\\]", "_");
    try (InputStream raw = connectionProvider.getInputStreamForItem(jobId, item).getStream();
         DigestInputStream dis = new DigestInputStream(raw, md5)) {
      return dataStore.getTempFileFromInputStream(dis, prefix, ".tmp");
    }
  }

  /**
   * Resolves the target album ID from the executor cache.
   * If albumId is provided but not cached (album creation failed), throws to prevent
   * silent data loss — photos/videos should not be uploaded without their album.
   */
  String resolveTargetAlbumId(String albumId, IdempotentImportExecutor executor)
      throws Exception {
    if (albumId == null) {
      return null;
    }
    // This will throw if album creation failed, which marks the item as failed too,
    // preventing silent loss of album organization.
    return executor.getCachedValue(albumId);
  }

  /** Removes temp data from the job store. */
  void cleanupTempData(UUID jobId, String fetchableUrl) throws IOException {
    dataStore.removeData(jobId, fetchableUrl);
  }
}
