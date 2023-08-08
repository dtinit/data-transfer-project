/*
 * Copyright 2022 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.google.music;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.json.gson.GsonFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.util.Durations;
import com.google.rpc.Code;
import java.io.IOException;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.musicModels.BatchPlaylistItemRequest;
import org.datatransferproject.datatransfer.google.musicModels.BatchPlaylistItemResponse;
import org.datatransferproject.datatransfer.google.musicModels.CreatePlaylistItemRequest;
import org.datatransferproject.datatransfer.google.musicModels.GooglePlaylist;
import org.datatransferproject.datatransfer.google.musicModels.GooglePlaylistItem;
import org.datatransferproject.datatransfer.google.musicModels.GoogleRelease;
import org.datatransferproject.datatransfer.google.musicModels.GoogleTrack;
import org.datatransferproject.datatransfer.google.musicModels.NewPlaylistItemResult;
import org.datatransferproject.datatransfer.google.musicModels.Status;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.idempotentexecutor.InMemoryIdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.datatransferproject.types.common.models.music.MusicContainerResource;
import org.datatransferproject.types.common.models.music.MusicPlaylist;
import org.datatransferproject.types.common.models.music.MusicPlaylistItem;
import org.datatransferproject.types.common.models.music.MusicRecording;
import org.datatransferproject.types.common.models.music.MusicRelease;
import org.datatransferproject.types.transfer.errors.ErrorDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public final class GoogleMusicImporterTest {

  private final UUID uuid = UUID.randomUUID();
  private GoogleMusicImporter googleMusicImporter;
  private GoogleMusicHttpApi googleMusicHttpApi;
  private IdempotentImportExecutor executor;
  private Monitor monitor;

  @BeforeEach
  public void setUp() {
    GoogleCredentialFactory credentialFactory = mock(GoogleCredentialFactory.class);
    googleMusicHttpApi = Mockito.mock(GoogleMusicHttpApi.class);
    monitor = Mockito.mock(Monitor.class);
    executor = new InMemoryIdempotentImportExecutor(monitor);

    googleMusicImporter =
        new GoogleMusicImporter(
            credentialFactory,
            GsonFactory.getDefaultInstance(),
            googleMusicHttpApi,
            null,
            monitor,
            1.0);
  }

  @Test
  public void importPlaylist() throws Exception {
    // Set up
    MusicPlaylist playlist = new MusicPlaylist("p1_id", "p1_title", null, null, null);
    ImmutableList<MusicPlaylist> playlists = ImmutableList.of(playlist);
    MusicContainerResource data = new MusicContainerResource(playlists, null, null, null);

    GooglePlaylist responsePlaylist = new GooglePlaylist();
    responsePlaylist.setTitle("p1_title");
    when(googleMusicHttpApi.createPlaylist(any(GooglePlaylist.class), any(String.class)))
        .thenReturn(responsePlaylist);

    // Run test
    ImportResult importResult = googleMusicImporter.importItem(uuid, executor, null, data);

    // Check results
    ArgumentCaptor<GooglePlaylist> playlistArgumentCaptor =
        ArgumentCaptor.forClass(GooglePlaylist.class);
    ArgumentCaptor<String> playlistIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(googleMusicHttpApi)
        .createPlaylist(playlistArgumentCaptor.capture(), playlistIdArgumentCaptor.capture());
    assertEquals("p1_title", playlistArgumentCaptor.getValue().getTitle());
    assertEquals("p1_id", playlistIdArgumentCaptor.getValue());
    assertTrue(executor.isKeyCached("p1_id"));
    assertEquals(importResult, ImportResult.OK);
  }

  @Test
  public void importTwoPlaylistItemsInDifferentPlaylist() throws Exception {
    importPlaylistSetUp("p1_id", "p1_title");
    importPlaylistSetUp("p2_id", "p2_title");

    MusicPlaylistItem playlistItem1 =
        new MusicPlaylistItem(
            new MusicRecording(
                "item1_isrc", null, 180000L, new MusicRelease("r1_icpn", null, null), null),
            "p1_id",
            1);
    MusicPlaylistItem playlistItem2 =
        new MusicPlaylistItem(
            new MusicRecording(
                "item1_isrc", null, 180000L, new MusicRelease("r1_icpn", null, null), null),
            "p2_id",
            1);
    GooglePlaylistItem googlePlaylistItem = buildGooglePlaylistItem("item1_isrc", "r1_icpn");
    BatchPlaylistItemRequest batchPlaylistItemRequest1 =
        new BatchPlaylistItemRequest(
            Lists.newArrayList(
                new CreatePlaylistItemRequest("p1_id", googlePlaylistItem)),
            "p1_id");
    BatchPlaylistItemRequest batchPlaylistItemRequest2 =
        new BatchPlaylistItemRequest(
            Lists.newArrayList(
                new CreatePlaylistItemRequest("p2_id", googlePlaylistItem)),
            "p2_id");
    BatchPlaylistItemResponse batchPlaylistItemResponse =
        new BatchPlaylistItemResponse(
            new NewPlaylistItemResult[]{
                buildPlaylistItemResult("item1_isrc", "r1_icpn", Code.OK_VALUE)
            });
    when(googleMusicHttpApi.createPlaylistItems(eq(batchPlaylistItemRequest1)))
        .thenReturn(batchPlaylistItemResponse);
    when(googleMusicHttpApi.createPlaylistItems(eq(batchPlaylistItemRequest2)))
        .thenReturn(batchPlaylistItemResponse);

    // Run test
    googleMusicImporter.importPlaylistItems(
        Lists.newArrayList(playlistItem1, playlistItem2), executor, uuid, null);

    // Two playlist items are imported
    assertTrue(executor.isKeyCached(String.valueOf(playlistItem1)));
    assertTrue(executor.isKeyCached(String.valueOf(playlistItem2)));
    // Expected executor to have no errors
    assertThat(executor.getErrors()).isEmpty();
  }

  @Test
  public void failOnePlaylistItem() throws Exception {
    importPlaylistSetUp("p1_id", "p1_title");
    importPlaylistSetUp("p2_id", "p2_title");

    MusicPlaylistItem playlistItem1 =
        new MusicPlaylistItem(
            new MusicRecording(
                "item1_isrc", null, 180000L, new MusicRelease("r1_icpn", null, null), null),
            "p1_id",
            1);
    MusicPlaylistItem playlistItem2 =
        new MusicPlaylistItem(
            new MusicRecording(
                "item1_isrc", null, 180000L, new MusicRelease("r1_icpn", null, null), null),
            "p2_id",
            1);
    GooglePlaylistItem googlePlaylistItem = buildGooglePlaylistItem("item1_isrc", "r1_icpn");
    BatchPlaylistItemRequest batchPlaylistItemRequest1 =
        new BatchPlaylistItemRequest(
            Lists.newArrayList(
                new CreatePlaylistItemRequest("p1_id", googlePlaylistItem)),
            "p1_id");
    BatchPlaylistItemRequest batchPlaylistItemRequest2 =
        new BatchPlaylistItemRequest(
            Lists.newArrayList(
                new CreatePlaylistItemRequest("p2_id", googlePlaylistItem)),
            "p2_id");
    BatchPlaylistItemResponse batchPlaylistItemResponse1 =
        new BatchPlaylistItemResponse(
            new NewPlaylistItemResult[]{
                buildPlaylistItemResult("item1_isrc", "r1_icpn", Code.OK_VALUE)
            });
    BatchPlaylistItemResponse batchPlaylistItemResponse2 =
        new BatchPlaylistItemResponse(
            new NewPlaylistItemResult[]{
                buildPlaylistItemResult("item1_isrc", "r1_icpn", Code.INVALID_ARGUMENT_VALUE)
            });
    when(googleMusicHttpApi.createPlaylistItems(eq(batchPlaylistItemRequest1)))
        .thenReturn(batchPlaylistItemResponse1);
    when(googleMusicHttpApi.createPlaylistItems(eq(batchPlaylistItemRequest2)))
        .thenReturn(batchPlaylistItemResponse2);

    // Run test
    googleMusicImporter.importPlaylistItems(
        Lists.newArrayList(playlistItem1, playlistItem2), executor, uuid, null);

    // One playlist item is imported
    assertTrue(executor.isKeyCached(String.valueOf(playlistItem1)));
    // Expected executor to have one error
    assertThat(executor.getErrors()).hasSize(1);
    ErrorDetail errorDetail = executor.getErrors().iterator().next();
    assertEquals(String.valueOf(playlistItem2), errorDetail.id());
    assertThat(errorDetail.exception()).contains("PlaylistItem could not be created.");
  }

  @Test
  public void importPlaylistItemsCreatePlaylistFailure() throws Exception {
    MusicPlaylistItem playlistItem1 =
        new MusicPlaylistItem(
            new MusicRecording(
                "item1_isrc", null, 180000L, new MusicRelease("r1_icpn", null, null), null),
            "p1_id",
            1);
    MusicPlaylistItem playlistItem2 =
        new MusicPlaylistItem(
            new MusicRecording(
                "item2_isrc", null, 180000L, new MusicRelease("r1_icpn", null, null), null),
            "p1_id",
            1);

    // Run test
    googleMusicImporter.importPlaylistItems(
        Lists.newArrayList(playlistItem1, playlistItem2), executor, uuid, null);

    // Expected executor to have two errors
    assertThat(executor.getErrors()).hasSize(2);
    ErrorDetail errorDetail = executor.getErrors().iterator().next();
    assertEquals(String.valueOf(playlistItem1), errorDetail.id());
    assertThat(errorDetail.exception()).contains("Fail to create Playlist p1_id");
  }

  @Test
  public void importPlaylistItemsSkippableFauilre() throws Exception {
    importPlaylistSetUp("p1_id", "p1_title");

    MusicPlaylistItem playlistItem1 =
        new MusicPlaylistItem(
            new MusicRecording(
                "item1_isrc", null, 180000L, new MusicRelease("r1_icpn", null, null), null),
            "p1_id",
            1);
    MusicPlaylistItem playlistItem2 =
        new MusicPlaylistItem(
            new MusicRecording(
                "item2_isrc", null, 180000L, new MusicRelease("r1_icpn", null, null), null),
            "p1_id",
            1);
    GooglePlaylistItem googlePlaylistItem1 = buildGooglePlaylistItem("item1_isrc", "r1_icpn");
    GooglePlaylistItem googlePlaylistItem2 = buildGooglePlaylistItem("item2_isrc", "r1_icpn");

    BatchPlaylistItemRequest batchPlaylistItemRequest =
        new BatchPlaylistItemRequest(
            Lists.newArrayList(
                new CreatePlaylistItemRequest("p1_id", googlePlaylistItem1),
                new CreatePlaylistItemRequest("p1_id", googlePlaylistItem2)),
            "p1_id");

    when(googleMusicHttpApi.createPlaylistItems(eq(batchPlaylistItemRequest)))
        .thenThrow(new IOException("skippable failure"));

    // Run test
    googleMusicImporter.importPlaylistItems(
        Lists.newArrayList(playlistItem1, playlistItem2), executor, uuid, null);

    // Only log the error not throw.
    assertThat(executor.getErrors()).isEmpty();
  }

  private void importPlaylistSetUp(String playlistId, String playlistTitle)
      throws Exception {
    MusicPlaylist playlist = new MusicPlaylist(playlistId, playlistTitle, null, null, null);
    ImmutableList<MusicPlaylist> playlists = ImmutableList.of(playlist);
    MusicContainerResource data = new MusicContainerResource(playlists, null, null, null);

    GooglePlaylist responsePlaylist = new GooglePlaylist();
    responsePlaylist.setName(playlistId);
    responsePlaylist.setTitle(playlistTitle);
    when(googleMusicHttpApi.createPlaylist(any(GooglePlaylist.class), any(String.class)))
        .thenReturn(responsePlaylist);

    ImportResult importResult = googleMusicImporter.importItem(uuid, executor, null /*authData*/,
        data);
  }

  private GooglePlaylistItem buildGooglePlaylistItem(String trackIsrc, String releaseIcpn) {
    GooglePlaylistItem playlistItem = new GooglePlaylistItem();
    GoogleRelease release = new GoogleRelease();
    release.setIcpn(releaseIcpn);
    GoogleTrack track = new GoogleTrack();
    track.setIsrc(trackIsrc);
    track.setDuration(Durations.toString(Durations.fromMillis(180000L)));
    track.setRelease(release);

    playlistItem.setTrack(track);
    playlistItem.setOrder(1);
    return playlistItem;
  }

  private NewPlaylistItemResult buildPlaylistItemResult(
      String trackIsrc, String releaseIcpn, int code) {
    // We do a lot of mocking as building the actual objects would require changing the constructors
    // which messed up deserialization so best to leave them unchanged.
    GooglePlaylistItem playlistItem = buildGooglePlaylistItem(trackIsrc, releaseIcpn);
    Status status = Mockito.mock(Status.class);
    when(status.getCode()).thenReturn(code);
    NewPlaylistItemResult result = Mockito.mock(NewPlaylistItemResult.class);
    when(result.getStatus()).thenReturn(status);
    when(result.getPlaylistItem()).thenReturn(playlistItem);
    return result;
  }
}
