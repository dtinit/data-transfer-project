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

import static com.google.common.truth.Truth.assertThat;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.apple.AppleInterfaceFactory;
import org.datatransferproject.datatransfer.apple.constants.AppleMusicConstants;
import org.datatransferproject.datatransfer.apple.music.musicproto.MusicProtocol;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.idempotentexecutor.InMemoryIdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.types.common.models.music.MusicContainerResource;
import org.datatransferproject.types.common.models.music.MusicGroup;
import org.datatransferproject.types.common.models.music.MusicPlaylist;
import org.datatransferproject.types.common.models.music.MusicPlaylistItem;
import org.datatransferproject.types.common.models.music.MusicRecording;
import org.datatransferproject.types.common.models.music.MusicRelease;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import org.datatransferproject.types.transfer.errors.ErrorDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.stubbing.Answer;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class AppleMusicImporterTest {

    private AppleMusicInterface appleMusicInterface;
    protected AppleInterfaceFactory factory;

    protected TokensAndUrlAuthData authData;
    protected AppCredentials appCredentials;
    protected Monitor monitor;
    static final String EXPORTING_SERVICE = "ExportingService";
    protected IdempotentImportExecutor executor;
    protected UUID uuid = UUID.randomUUID();

    private AppleMusicImporter appleMusicImporter;

    @BeforeEach
    public void setup() throws Exception {
        monitor = mock(Monitor.class);
        authData = mock(TokensAndUrlAuthData.class);
        appCredentials = mock(AppCredentials.class);
        executor = new InMemoryIdempotentImportExecutor(monitor);
        appleMusicInterface = setupMusicInterface();
        factory = mock(AppleInterfaceFactory.class);
        when(factory.getOrCreateMusicInterface(any(), any(), any(), anyString(), any()))
                .thenReturn(appleMusicInterface);

        appleMusicImporter = new AppleMusicImporter(
                new AppCredentials("key", "secret"), EXPORTING_SERVICE, monitor, factory);
    }

    private AppleMusicInterface setupMusicInterface() throws Exception {
        AppleMusicInterface musicInterface = mock(AppleMusicInterface.class);
        Map<String, Object> fieldsToInject = new HashMap<>();
        fieldsToInject.put("baseUrl", "https://dummy-apis.music.apple.com");
        fieldsToInject.put("appCredentials", new AppCredentials("key", "secret"));
        fieldsToInject.put("exportingService", EXPORTING_SERVICE);
        fieldsToInject.put("monitor", monitor);

        fieldsToInject.entrySet()
                .forEach(entry -> {
                    try {
                        ReflectionUtils.findFields(
                                AppleMusicInterface.class,
                                f -> f.getName().equals(entry.getKey()),
                                ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                                .stream()
                                .findFirst()
                                .get()
                                .set(musicInterface, entry.getValue());
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });
        when(musicInterface.importPlaylists(any(), any(), any())).thenCallRealMethod();
        when(musicInterface.importMusicPlaylistItems(any(), any(), any())).thenCallRealMethod();
        return musicInterface;
    }

    @Test
    public void importPlaylists() throws Exception {

        List<MusicPlaylist> musicPlaylists = createTestMusicPlaylists();
        setUpImportPlaylistsBatchResponse(musicPlaylists.stream().collect(
                Collectors.toMap(MusicPlaylist::getId, playlist -> SC_OK)));

        MusicContainerResource playlistsResource = new MusicContainerResource(musicPlaylists, null, null, null);

        final ImportResult importResult = appleMusicImporter.importItem(uuid, executor, authData, playlistsResource);

        verify(appleMusicInterface)
                .importPlaylistsBatch(uuid.toString(), musicPlaylists);

        assertThat(importResult.getCounts().isPresent());

        // Should be the same as the number of playlists sent in.
        assertThat(importResult.getCounts().get().get(AppleMusicConstants.PLAYLISTS_COUNT_DATA_NAME) == playlistsResource.getPlaylists().size());
        // No playlist items were sent.
        assertThat(importResult.getCounts().get().get(AppleMusicConstants.PLAYLIST_ITEMS_COUNT_DATA_NAME) == 0);
    }

    @Test
    public void importPlaylistsFails() throws Exception {
        MusicPlaylist playlist1 = createTestMusicPlaylist();
        List<MusicPlaylist> musicPlaylists = List.of(playlist1);
        setUpImportPlaylistsBatchResponse(musicPlaylists.stream().collect(
                Collectors.toMap(MusicPlaylist::getId, playlist -> SC_BAD_REQUEST)));

        MusicContainerResource playlistsResource = new MusicContainerResource(musicPlaylists, null, null, null);
        final ImportResult importResult = appleMusicImporter.importItem(uuid, executor, authData, playlistsResource);

        verify(appleMusicInterface)
                .importPlaylistsBatch(uuid.toString(), musicPlaylists);

        assertThat(executor.getErrors()).hasSize(musicPlaylists.size());
        assertThat(importResult.getCounts().isPresent());
        ErrorDetail errorDetail = executor.getErrors().iterator().next();
        assertThat(errorDetail.id().equals(playlist1.getId()));
        assertThat(errorDetail.exception()).contains("Failed to create playlist");
    }

    private void setUpImportPlaylistsBatchResponse(@Nonnull final Map<String, Integer> dataIdToStatus)
            throws CopyExceptionWithFailureReason, IOException, URISyntaxException {
        when(appleMusicInterface.importPlaylistsBatch(any(String.class), any(List.class)))
                .thenAnswer((Answer<MusicProtocol.ImportMusicPlaylistsResponse>)
                        invocation -> {
                            Object[] args = invocation.getArguments();
                            final List<MusicPlaylist> musicPlaylists = (List<MusicPlaylist>) args[1];
                            final List<MusicProtocol.MusicPlaylistResponse> musicPlaylistResponses =
                                    musicPlaylists.stream()
                                            .map(playlist -> MusicProtocol.MusicPlaylistResponse.newBuilder()
                                                    .setId(playlist.getId())
                                                    .setName(playlist.getTitle())
                                                    .setStatus(MusicProtocol.Status.newBuilder()
                                                            .setCode(dataIdToStatus.get(playlist.getId()))
                                                            .build())
                                                    .build())
                                            .collect(Collectors.toList());
                            return MusicProtocol.ImportMusicPlaylistsResponse.newBuilder()
                                    .addAllMusicPlaylistResponse(musicPlaylistResponses)
                                    .build();
                        }
                );
    }

    @Test
    public void testImportPlaylistTracks() throws Exception {
        List<MusicPlaylistItem> musicPlaylistItems = createTestPlaylistItems(randomString());
        setUpImportPlaylistTracksBatchResponse(musicPlaylistItems.stream().collect(
                Collectors.toMap(item -> item.getTrack().getIsrcCode(), item -> SC_OK)));

        MusicContainerResource playlistItemsResource = new MusicContainerResource(null, musicPlaylistItems, null, null);

        final ImportResult importResult = appleMusicImporter.importItem(uuid, executor, authData, playlistItemsResource);

        verify(appleMusicInterface)
                .importMusicPlaylistItemsBatch(uuid.toString(), musicPlaylistItems);

        assertThat(importResult.getCounts().isPresent());

        assertThat(importResult.getCounts().get().get(AppleMusicConstants.PLAYLIST_ITEMS_COUNT_DATA_NAME) == playlistItemsResource.getPlaylistItems().size());

        assertThat(importResult.getCounts().get().get(AppleMusicConstants.PLAYLISTS_COUNT_DATA_NAME) == 0);
    }

    @Test
    public void testImportPlaylistTracksFailure() throws Exception {
        MusicPlaylistItem item1 = createTestPlaylistItem(randomString(), 1);
        List<MusicPlaylistItem> musicPlaylistItems = List.of(item1);
        setUpImportPlaylistTracksBatchResponse(musicPlaylistItems.stream().collect(
                Collectors.toMap(item -> item.getTrack().getIsrcCode(), item -> SC_BAD_REQUEST)));

        MusicContainerResource playlistItemsResource = new MusicContainerResource(null, musicPlaylistItems, null, null);

        final ImportResult importResult = appleMusicImporter.importItem(uuid, executor, authData, playlistItemsResource);

        verify(appleMusicInterface)
                .importMusicPlaylistItemsBatch(uuid.toString(), musicPlaylistItems);

        assertThat(executor.getErrors()).hasSize(musicPlaylistItems.size());
        assertThat(importResult.getCounts().isPresent());
        ErrorDetail errorDetail = executor.getErrors().iterator().next();
        assertThat(errorDetail.id().equals(item1.toString()));
        assertThat(errorDetail.exception()).contains("Failed to import playlist track");
    }

    @Test
    public void importTwoPlaylistItemsInDifferentPlaylists() throws Exception {
        MusicPlaylistItem item1 = createTestPlaylistItem(randomString(), 1);
        MusicPlaylistItem item2 = createTestPlaylistItem(randomString(), 1);
        List<MusicPlaylistItem> musicPlaylistItems = List.of(item1, item2);
        setUpImportPlaylistTracksBatchResponse(musicPlaylistItems.stream().collect(
                Collectors.toMap(item -> item.getTrack().getIsrcCode(), item -> SC_OK)));

        MusicContainerResource itemsResource = new MusicContainerResource(null, musicPlaylistItems, null, null);
        final ImportResult importResult = appleMusicImporter.importItem(uuid, executor, authData, itemsResource);

        verify(appleMusicInterface)
                .importMusicPlaylistItemsBatch(uuid.toString(), musicPlaylistItems);

        assertThat(executor.getErrors()).isEmpty();
        assertThat(importResult.getCounts().isPresent());

        assertThat(importResult.getCounts().get().get(AppleMusicConstants.PLAYLIST_ITEMS_COUNT_DATA_NAME) == itemsResource.getPlaylistItems().size());

        assertThat(importResult.getCounts().get().get(AppleMusicConstants.PLAYLISTS_COUNT_DATA_NAME) == 0);
    }

    private void setUpImportPlaylistTracksBatchResponse(@Nonnull final Map<String, Integer> dataIdToStatus) throws Exception {
        when(appleMusicInterface.importMusicPlaylistItemsBatch(any(String.class), any(List.class)))
                .thenAnswer((Answer<MusicProtocol.ImportMusicPlaylistTracksResponse>)
                        invocation -> {
                            Object[] args = invocation.getArguments();
                            final List<MusicPlaylistItem> musicPlaylistItems = (List<MusicPlaylistItem>) args[1];
                            final List<MusicProtocol.MusicPlaylistTrackResponse> musicPlaylistTrackResponses =
                                    musicPlaylistItems.stream()
                                            .map(item -> MusicProtocol.MusicPlaylistTrackResponse.newBuilder()
                                                    .setName(item.getTrack().getTitle())
                                                    .setStatus(MusicProtocol.Status.newBuilder()
                                                            .setCode(dataIdToStatus.get(item.getTrack().getIsrcCode())) // TODO: Setup based on ISRC. Best id I guess.
                                                            .build())
                                                    .build())
                                            .collect(Collectors.toList());
                            return MusicProtocol.ImportMusicPlaylistTracksResponse.newBuilder()
                                    .addAllMusicPlaylistTrackResponse(musicPlaylistTrackResponses)
                                    .build();
                        }
                );
    }

    private List<MusicPlaylist> createTestMusicPlaylists() {
        int numMusicPlaylist = RandomUtils.nextInt(0, 100);
        List<MusicPlaylist> musicPlaylists = new ArrayList<>();

        for (int i = 0; i < numMusicPlaylist; i++) {
            musicPlaylists.add(createTestMusicPlaylist());
        }

        return musicPlaylists;
    }

    private MusicPlaylist createTestMusicPlaylist() {
        String id = randomString();
        String title = randomString();
        String description = RandomUtils.nextBoolean() ? randomString() : null;
        Instant timeCreated = randomInstant();
        Instant timeUpdated = timeCreated.plusMillis(RandomUtils.nextLong(0L, 630000000000L));

        return new MusicPlaylist(id, title, description, timeCreated, timeUpdated);
    }

    private List<MusicPlaylistItem> createTestPlaylistItems(String playlistId) {
        int numMusicPlaylistItem = RandomUtils.nextInt(0, 100);
        List<MusicPlaylistItem> musicPlaylistItems = new ArrayList<>();

        for (int i = 0; i < numMusicPlaylistItem; i++) {
            musicPlaylistItems.add(createTestPlaylistItem(playlistId, i + 1));
        }

        return musicPlaylistItems;
    }

    private MusicPlaylistItem createTestPlaylistItem(String playlistId, int order) {
        return new MusicPlaylistItem(createTestMusicRecording(), playlistId, order);
    }

    private MusicRecording createTestMusicRecording() {
        String isrcCode = RandomStringUtils.random(12);
        String title = randomString();
        long durationMillis = RandomUtils.nextLong();
        MusicRelease musicRelease = createTestMusicRelease();
        List<MusicGroup> byArtists = createTestMusicGroups();

        return new MusicRecording(isrcCode, title, durationMillis, musicRelease, byArtists, true);
    }

    private MusicRelease createTestMusicRelease() {
        String icpnCode = randomString();
        String title = randomString();
        return new MusicRelease(icpnCode, title, createTestMusicGroups());
    }

    private List<MusicGroup> createTestMusicGroups() {
        int numMusicGroups = RandomUtils.nextInt(1, 10);
        List<MusicGroup> musicGroups = new ArrayList<>();
        for (int i = 0; i < numMusicGroups; i++) {
            musicGroups.add(createTestMusicGroup());
        }
        return musicGroups;
    }

    private MusicGroup createTestMusicGroup() {
        return new MusicGroup(randomString());
    }

    private String randomString() {
        return RandomStringUtils.random(RandomUtils.nextInt(1,256));
    }

    private Instant randomInstant() {
        long randomEpochMillis = RandomUtils.nextLong(1007337600000L, 1704317942000L);
        return Instant.ofEpochMilli(randomEpochMillis);
    }


}
