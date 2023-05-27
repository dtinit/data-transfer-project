/*
 * Copyright 2023 The Data Transfer Project Authors.
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
package org.datatransferproject.datatransfer.apple.photos;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.apple.AppleInterfaceFactory;
import org.datatransferproject.datatransfer.apple.constants.ApplePhotosConstants;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.JobMetadata;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.jetbrains.annotations.NotNull;

/**
 * An Apple importer to import the Photos into Apple iCloud-photos.
 */
public class ApplePhotosImporter implements Importer<TokensAndUrlAuthData, PhotosContainerResource> {

    private final AppCredentials appCredentials;

    private final String exportingService;

    private final Monitor monitor;

    private final AppleInterfaceFactory factory;

    public ApplePhotosImporter(@NotNull final AppCredentials appCredentials, @NotNull final Monitor monitor) {
        this(appCredentials, JobMetadata.getExportService(), monitor, new AppleInterfaceFactory());
    }

    @VisibleForTesting
    ApplePhotosImporter(@NotNull final AppCredentials appCredentials, @NotNull final String exportingService, @NotNull final Monitor monitor, @NotNull AppleInterfaceFactory factory) {
        this.appCredentials = appCredentials;
        this.exportingService = exportingService;
        this.monitor = monitor;
        this.factory = factory;
    }

    @Override
    public ImportResult importItem(UUID jobId, IdempotentImportExecutor idempotentImportExecutor, TokensAndUrlAuthData authData, PhotosContainerResource data) throws Exception {
        if (data == null) {
            return ImportResult.OK;
        }
        AppleMediaInterface mediaInterface = factory.getOrCreateMediaInterface(jobId, authData, appCredentials, exportingService, monitor);
        // Uploads album metadata
        final int albumCount = mediaInterface.importAlbums(jobId, idempotentImportExecutor, data.getAlbums().stream().map(MediaAlbum::photoToMediaAlbum).collect(Collectors.toList()), DataVertical.PHOTOS.getDataType());
        final Map<String, Long> importPhotosResult = mediaInterface.importAllMedia(jobId, idempotentImportExecutor, data.getPhotos(), DataVertical.PHOTOS.getDataType());
        // generate import result
        final ImportResult result = ImportResult.OK;
        final Map<String, Integer> counts = new ImmutableMap.Builder<String, Integer>().put(PhotosContainerResource.ALBUMS_COUNT_DATA_NAME, albumCount).put(PhotosContainerResource.PHOTOS_COUNT_DATA_NAME, importPhotosResult.get(ApplePhotosConstants.COUNT_KEY).intValue()).build();
        return result.copyWithBytes(importPhotosResult.get(ApplePhotosConstants.BYTES_KEY)).copyWithCounts(counts);
    }
}
