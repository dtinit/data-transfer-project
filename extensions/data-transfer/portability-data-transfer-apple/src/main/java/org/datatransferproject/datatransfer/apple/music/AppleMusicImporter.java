/*
 * Copyright 2024 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.apple.music;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.apple.AppleInterfaceFactory;
import org.datatransferproject.datatransfer.apple.constants.AppleMusicConstants;
import org.datatransferproject.datatransfer.apple.exceptions.AppleContentException;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.JobMetadata;
import org.datatransferproject.types.common.models.music.MusicContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;

/**
 * Apple Music implementation for the Importer interface.
 * This is the main entry point for importing playlists and playlist-tracks from the source services into Apple Music.
 */
public class AppleMusicImporter implements Importer<TokensAndUrlAuthData, MusicContainerResource> {

    private final AppCredentials appCredentials;
    private final String exportingService;
    private final Monitor monitor;
    private final AppleInterfaceFactory factory;

    public AppleMusicImporter(AppCredentials appCredentials, Monitor monitor) {
        this(appCredentials, JobMetadata.getExportService(), monitor, new AppleInterfaceFactory());
    }

    @VisibleForTesting
    AppleMusicImporter(
            @Nonnull final AppCredentials appCredentials,
            @Nonnull final String exportingService,
            @Nonnull final Monitor monitor,
            @Nonnull AppleInterfaceFactory factory) {
        this.appCredentials = appCredentials;
        this.exportingService = exportingService;
        this.monitor = monitor;
        this.factory = factory;
    }

    @Override
    public ImportResult importItem(
            UUID jobId,
            IdempotentImportExecutor idempotentImportExecutor,
            TokensAndUrlAuthData authData,
            MusicContainerResource data)
            throws Exception {

        if (data == null) {
            // Nothing to do
            return new ImportResult(new AppleContentException("Null MusicContainerResource received on AppleMusicImporter::importItem"));
        }

        int playlistsCount = 0;
        int playlistItemsCount = 0;

        AppleMusicInterface musicInterface = factory
                .getOrCreateMusicInterface(jobId, authData, appCredentials, exportingService, monitor);

        if (!data.getPlaylists().isEmpty()) {
            playlistsCount = musicInterface.importPlaylists(jobId, idempotentImportExecutor, data.getPlaylists());
        }

        if (!data.getPlaylistItems().isEmpty()) {
            playlistItemsCount = musicInterface.importMusicPlaylistItems(jobId, idempotentImportExecutor, data.getPlaylistItems());
        }

        final Map<String, Integer> counts =
                new ImmutableMap.Builder<String, Integer>()
                        .put(AppleMusicConstants.PLAYLISTS_COUNT_DATA_NAME, playlistsCount)
                        .put(AppleMusicConstants.PLAYLIST_ITEMS_COUNT_DATA_NAME, playlistItemsCount)
                        .build();
        return ImportResult.OK
                .copyWithCounts(counts);
    }
}
