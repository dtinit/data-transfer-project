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

import static org.datatransferproject.datatransfer.google.music.GoogleMusicExporter.GOOGLE_PLAYLIST_NAME_PREFIX;
import static org.datatransferproject.datatransfer.google.music.GoogleMusicExporter.PLAYLIST_TOKEN_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.api.client.json.gson.GsonFactory;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.musicModels.GooglePlaylist;
import org.datatransferproject.datatransfer.google.musicModels.GooglePlaylistItem;
import org.datatransferproject.datatransfer.google.musicModels.GoogleRelease;
import org.datatransferproject.datatransfer.google.musicModels.GoogleTrack;
import org.datatransferproject.datatransfer.google.musicModels.PlaylistItemListResponse;
import org.datatransferproject.datatransfer.google.musicModels.PlaylistListResponse;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.datatransferproject.types.common.PaginationData;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.ContainerResource;
import org.datatransferproject.types.common.models.IdOnlyContainerResource;
import org.datatransferproject.types.common.models.music.MusicContainerResource;
import org.datatransferproject.types.common.models.music.MusicPlaylist;
import org.datatransferproject.types.common.models.music.MusicPlaylistItem;
import org.datatransferproject.types.common.models.music.MusicRecording;
import org.datatransferproject.types.common.models.music.MusicRelease;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GoogleMusicExporterTest {

  static final String PLAYLIST_PAGE_TOKEN = "playlist_page_token";
  static final String PLAYLIST_ITEM_TOKEN = "playlist_item_token";

  private final UUID uuid = UUID.randomUUID();

  private GoogleMusicExporter googleMusicExporter;
  private GoogleMusicHttpApi musicHttpApi;

  private PlaylistListResponse playlistListResponse;
  private PlaylistItemListResponse playlistItemListResponse;

  @BeforeEach
  public void setUp() throws IOException, InvalidTokenException, PermissionDeniedException {
    GoogleCredentialFactory credentialFactory = mock(GoogleCredentialFactory.class);
    musicHttpApi = mock(GoogleMusicHttpApi.class);

    Monitor monitor = mock(Monitor.class);

    googleMusicExporter =
        new GoogleMusicExporter(
            credentialFactory, GsonFactory.getDefaultInstance(), musicHttpApi, monitor);

    playlistListResponse = mock(PlaylistListResponse.class);
    playlistItemListResponse = mock(PlaylistItemListResponse.class);

    when(musicHttpApi.listPlaylists(any(Optional.class))).thenReturn(playlistListResponse);
    when(musicHttpApi.listPlaylistItems(any(String.class), any(Optional.class)))
        .thenReturn(playlistItemListResponse);

    verifyNoInteractions(credentialFactory);
  }

  @Test
  public void exportPlaylistFirstSet()
      throws IOException, InvalidTokenException, PermissionDeniedException {
    setUpSinglePlaylist(GOOGLE_PLAYLIST_NAME_PREFIX + "p1_id");
    when(playlistListResponse.getNextPageToken()).thenReturn(PLAYLIST_PAGE_TOKEN);

    StringPaginationToken inputPaginationToken = new StringPaginationToken(PLAYLIST_TOKEN_PREFIX);

    // Run test
    ExportResult<MusicContainerResource> result =
        googleMusicExporter.exportPlaylists(null, Optional.of(inputPaginationToken), uuid);

    // Check results
    // Verify correct methods were called
    verify(musicHttpApi).listPlaylists(Optional.empty());
    verify(playlistListResponse).getPlaylists();

    // Check pagination token
    ContinuationData continuationData = result.getContinuationData();
    StringPaginationToken paginationToken =
        (StringPaginationToken) continuationData.getPaginationData();
    assertEquals(PLAYLIST_TOKEN_PREFIX + PLAYLIST_PAGE_TOKEN, paginationToken.getToken());

    // Check playlists field of container
    Collection<MusicPlaylist> actualPlaylists = result.getExportedData().getPlaylists();
    assertThat(actualPlaylists.stream().map(MusicPlaylist::getId).collect(Collectors.toList()))
        .containsExactly("p1_id");

    // Check playlistItems field of container (should be empty)
    List<MusicPlaylistItem> actualPlaylistItems = result.getExportedData().getPlaylistItems();
    assertThat(actualPlaylistItems).isEmpty();
    // Should be one container in the resource list
    List<ContainerResource> actualResources = continuationData.getContainerResources();
    assertThat(
        actualResources.stream()
            .map(a -> ((IdOnlyContainerResource) a).getId())
            .collect(Collectors.toList()))
        .containsExactly("p1_id");
  }

  @Test
  public void exportPlaylistSubsequentSet()
      throws IOException, InvalidTokenException, PermissionDeniedException {
    setUpSinglePlaylist(GOOGLE_PLAYLIST_NAME_PREFIX + "p1_id");
    when(playlistListResponse.getNextPageToken()).thenReturn(null);

    StringPaginationToken inputPaginationToken =
        new StringPaginationToken(PLAYLIST_TOKEN_PREFIX + PLAYLIST_PAGE_TOKEN);

    // Run test
    ExportResult<MusicContainerResource> result =
        googleMusicExporter.exportPlaylists(null, Optional.of(inputPaginationToken), uuid);

    // Check results
    // Verify correct methods were called
    verify(musicHttpApi).listPlaylists(Optional.of(PLAYLIST_PAGE_TOKEN));
    verify(playlistListResponse).getPlaylists();

    // Check pagination token - should be absent
    ContinuationData continuationData = result.getContinuationData();
    StringPaginationToken paginationData =
        (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationData.getToken()).isEmpty();
  }

  @Test
  public void exportPlaylistItemFirstSet()
      throws IOException, InvalidTokenException, PermissionDeniedException, ParseException {
    GooglePlaylistItem playlistItem = setUpSinglePlaylistItem("t1_isrc", "r1_icpn");
    when(playlistItemListResponse.getPlaylistItems())
        .thenReturn(new GooglePlaylistItem[]{playlistItem});
    when(playlistItemListResponse.getNextPageToken()).thenReturn(PLAYLIST_ITEM_TOKEN);

    IdOnlyContainerResource idOnlyContainerResource = new IdOnlyContainerResource("p1_id");

    ExportResult<MusicContainerResource> result =
        googleMusicExporter.exportPlaylistItems(null, idOnlyContainerResource, Optional.empty(),
            uuid);

    // Check results
    // Verify correct methods were called
    verify(musicHttpApi).listPlaylistItems("p1_id", Optional.empty());
    verify(playlistItemListResponse).getPlaylistItems();

    // Check pagination
    ContinuationData continuationData = result.getContinuationData();
    StringPaginationToken paginationToken =
        (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationToken.getToken()).isEqualTo(PLAYLIST_ITEM_TOKEN);

    // Check playlist field of container (should be empty)
    Collection<MusicPlaylist> actualPlaylists = result.getExportedData().getPlaylists();
    assertThat(actualPlaylists).isEmpty();

    // Check playlistItems field of container
    List<MusicPlaylistItem> actualPlaylistItems = result.getExportedData().getPlaylistItems();
    assertThat(
        actualPlaylistItems.stream()
            .map(MusicPlaylistItem::getPlaylistId)
            .collect(Collectors.toList()))
        .containsExactly("p1_id"); // for download
    assertThat(
        actualPlaylistItems.stream()
            .map(MusicPlaylistItem::getTrack)
            .collect(Collectors.toList()))
        .containsExactly(
            new MusicRecording("t1_isrc", null, 0L, new MusicRelease("r1_icpn", null, null), null,
                false));
  }

  @Test
  public void exportPlaylistItemSubsequentSet()
      throws IOException, InvalidTokenException, PermissionDeniedException, ParseException {
    GooglePlaylistItem playlistItem = setUpSinglePlaylistItem("t1_isrc", "r1_icpn");
    when(playlistItemListResponse.getPlaylistItems())
        .thenReturn(new GooglePlaylistItem[]{playlistItem});
    when(playlistItemListResponse.getNextPageToken()).thenReturn(null);

    StringPaginationToken inputPaginationToken = new StringPaginationToken(PLAYLIST_ITEM_TOKEN);
    IdOnlyContainerResource idOnlyContainerResource = new IdOnlyContainerResource("p1_id");

    // Run test
    ExportResult<MusicContainerResource> result =
        googleMusicExporter.exportPlaylistItems(
            null, idOnlyContainerResource, Optional.of(inputPaginationToken), uuid);

    // Check results
    // Verify correct methods were called
    verify(musicHttpApi).listPlaylistItems("p1_id", Optional.of(PLAYLIST_ITEM_TOKEN));
    verify(playlistItemListResponse).getPlaylistItems();

    // Check pagination token
    ContinuationData continuationData = result.getContinuationData();
    PaginationData paginationToken = continuationData.getPaginationData();
    assertThat(paginationToken).isNull();
  }

  /**
   * Sets up a response with a single playlist, containing a single playlist item
   */
  private void setUpSinglePlaylist(String playlistName) {
    GooglePlaylist playlistEntry = new GooglePlaylist();
    playlistEntry.setName(playlistName);
    playlistEntry.setTitle("p1_title");
    playlistEntry.setDescription("p1_description");

    when(playlistListResponse.getPlaylists()).thenReturn(new GooglePlaylist[]{playlistEntry});
  }

  /**
   * Sets up a response for a single playlist item
   */
  private GooglePlaylistItem setUpSinglePlaylistItem(String isrc, String icpn) {
    GooglePlaylistItem playlistItemEntry = new GooglePlaylistItem();
    GoogleTrack track = new GoogleTrack();
    GoogleRelease release = new GoogleRelease();
    release.setIcpn(icpn);
    track.setIsrc(isrc);
    track.setRelease(release);
    playlistItemEntry.setTrack(track);
    return playlistItemEntry;
  }
}
