/*
 * Copyright 2023 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.datatransferproject.datatransfer.google.common.gphotos;

import static java.lang.String.format;
import static org.datatransferproject.datatransfer.google.photos.GooglePhotosInterface.ERROR_HASH_MISMATCH;
import static org.datatransferproject.datatransfer.google.videos.GoogleVideosInterface.uploadBatchOfVideos;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.json.JsonFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.rpc.Code;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.common.GooglePhotosImportUtils;
import org.datatransferproject.datatransfer.google.mediaModels.BatchMediaItemResponse;
import org.datatransferproject.datatransfer.google.mediaModels.GoogleAlbum;
import org.datatransferproject.datatransfer.google.mediaModels.NewMediaItem;
import org.datatransferproject.datatransfer.google.mediaModels.NewMediaItemResult;
import org.datatransferproject.datatransfer.google.mediaModels.NewMediaItemUpload;
import org.datatransferproject.datatransfer.google.mediaModels.Status;
import org.datatransferproject.datatransfer.google.photos.GooglePhotosInterface;
import org.datatransferproject.datatransfer.google.photos.PhotoResult;
import org.datatransferproject.spi.cloud.connection.ConnectionProvider;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore.InputStreamWrapper;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.spi.transfer.i18n.BaseMultilingualDictionary;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.idempotentexecutor.ItemImportResult;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.datatransferproject.spi.transfer.types.UploadErrorException;
import org.datatransferproject.types.common.DownloadableFile;
import org.datatransferproject.types.common.ImportableItem;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

/**
 * Upload content to the Google Photos servers' APIs - be it photos, video, etc.
 *
 * APIs offered aim to be agnostic to file types, and try to be the general wrapper for all upload
 * needs for the Photos SDKs, is in contrast to the predecessors used in the DTP codebase like
 * {@link PhotosLibraryClient} or {@link GooglePhotosInterface} which are each used and/or managed
 * for a specific use-case and we hope to delete in favor of classes in this package.
 *
 * WARNING: this should be constructed PER request so as to not conflate job IDs or auth data across
 * processes. That is: do NOT cache an instance of this object across your requests, say by storing
 * the instance as a member of your adapter's Importer or Exporter implementations.
 */
// TODO(aksingh737,jzacsh) finish refactoring Google{Photos,Video,Media}{Importer,Exporter} classes
// so they're not all drifting-forks of each other, and instead share code with the help of small
// interfaces. We can start by using some of the de-duplication that happened in
// org.datatransferproject.datatransfer.google.common.gphotos package
public class GPhotosUpload {

    private UUID jobId;

    private IdempotentImportExecutor executor;

    private TokensAndUrlAuthData authData;

    // We partition into groups of 49 as 50 is the maximum number of items that can be created
    // in one call. (We use 49 to avoid potential off by one errors)
    // https://developers.google.com/photos/library/guides/upload-media#creating-media-item
    private static final int BATCH_UPLOAD_SIZE = 49;

    /**
     * WARNING: this should be constructed PER request so as to not conflate job IDs or auth data
     * across processes. That is: do NOT cache an instance of this object across your requests, say by
     * storing the instance as a member of your adapter's Importer or Exporter implementations.
     */
    public GPhotosUpload(UUID jobId, IdempotentImportExecutor executor, TokensAndUrlAuthData authData) {
        this.jobId = jobId;
        this.executor = executor;
        this.authData = authData;
    }

    /**
     * Imports all `items` by fanning out to `batchImporter` upload calls as specified by the.
     *
     * Returns the number of uploaded bytes, as summed across all `items` that were uploaded.
     */
    // TODO(aksingh737,jzacsh) WARNING: delete the duplicated GooglePhotosImporter code by pulling
    // this out of Media into a new GphotoMedia class that exposes these methods for _both_
    // GoogleMediaImporter _and_ GooglePhotosImporter to use.
    public <T extends DownloadableFile> long uploadItemsViaBatching(Collection<T> items, ItemBatchUploader<T> importer) throws Exception {
        long bytes = 0L;
        if (items == null || items.size() <= 0) {
            return bytes;
        }
        Map<String, List<T>> itemsByAlbumId = items.stream().filter(item -> !executor.isKeyCached(item.getIdempotentId())).collect(Collectors.groupingBy(DownloadableFile::getFolderId));
        for (Entry<String, List<T>> albumEntry : itemsByAlbumId.entrySet()) {
            String originalAlbumId = albumEntry.getKey();
            String googleAlbumId;
            if (Strings.isNullOrEmpty(originalAlbumId)) {
                // This is ok, since NewMediaItemUpload will ignore all null values and it's possible to
                // upload a NewMediaItem without a corresponding album id.
                googleAlbumId = null;
            } else {
                // Note this will throw if creating the album failed, which is what we want
                // because that will also mark this photo as being failed.
                googleAlbumId = executor.getCachedValue(originalAlbumId);
            }
            UnmodifiableIterator<List<T>> batches = Iterators.partition(albumEntry.getValue().iterator(), BATCH_UPLOAD_SIZE);
            while (batches.hasNext()) {
                long batchBytes = importer.uploadToAlbum(jobId, authData, batches.next(), executor, googleAlbumId);
                bytes += batchBytes;
            }
        }
        return bytes;
    }

    // TODO(aksingh737,jzacsh) consider renaming lower-level gphotos code (ie: anything of the "google
    // photos" product but not a "photo" from "google"; examples: the GooglePhotosInterface that
    // interacts with gphotos teams' upstream SDKs, interfaces like this one below, PhotoResult,
    // etc.). eg maybe start a org.datatransferproject.datatransfer.google.gphotos package for things
    // that are wrapping the gphotos SDKs (the examples already mentioned) and make that package
    // importable by the other google adapters.
    @FunctionalInterface
    public interface ItemBatchUploader<T> {

        /**
         * Returns the number of uploaded bytes, as summed across all `items` that were uploaded in this
         * batch.
         */
        public long uploadToAlbum(UUID jobId, TokensAndUrlAuthData authData, List<T> batch, IdempotentImportExecutor executor, String targetAlbumId) throws Exception;
    }
}
