/*
 * Copyright 2019 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.spotify.playlists;


import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.specification.Paging;
import com.wrapper.spotify.model_objects.specification.PlaylistSimplified;
import com.wrapper.spotify.model_objects.specification.PlaylistTrack;
import com.wrapper.spotify.model_objects.specification.Track;
import com.wrapper.spotify.model_objects.specification.User;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.models.playlists.MusicAlbum;
import org.datatransferproject.types.common.models.playlists.MusicGroup;
import org.datatransferproject.types.common.models.playlists.MusicPlaylist;
import org.datatransferproject.types.common.models.playlists.MusicRecording;
import org.datatransferproject.types.common.models.playlists.PlaylistContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

/**
 * Exports playlists from Spotify.
 **/
public class SpotifyPlaylistExporter implements
    Exporter<TokensAndUrlAuthData, PlaylistContainerResource> {

  private final Monitor monitor;
  private final SpotifyApi spotifyApi;

  public SpotifyPlaylistExporter(Monitor monitor, SpotifyApi spotifyApi) {
    this.monitor = monitor;
    this.spotifyApi = spotifyApi;
  }

  @Override
  public ExportResult<PlaylistContainerResource> export(UUID jobId,
      TokensAndUrlAuthData authData, Optional<ExportInformation> exportInformation)
      throws Exception {
    spotifyApi.setAccessToken(authData.getAccessToken());
    spotifyApi.setRefreshToken(authData.getRefreshToken());

    User user = spotifyApi.getCurrentUsersProfile().build().execute();

    return new ExportResult<>(
        ResultType.END,
        enumeratePlaylists(user.getId()));
  }

  private PlaylistContainerResource enumeratePlaylists(String userId)
      throws IOException, SpotifyWebApiException {
    List<MusicPlaylist> results = new ArrayList<>();
    int offset = 0;
    Paging<PlaylistSimplified> playlists;
    do {
      monitor.debug(() -> "Fetching playlists with offset %s", offset);
      playlists = spotifyApi.getListOfUsersPlaylists(userId)
          .offset(offset)
          .build()
          .execute();
      for (PlaylistSimplified playlist : playlists.getItems()) {
        results.add(new MusicPlaylist(
            playlist.getName(),
            fetchPlaylist(playlist.getId())));
      }
      offset += playlists.getItems().length;
    } while (!Strings.isNullOrEmpty(playlists.getNext()) && playlists.getItems().length > 0);
    return new PlaylistContainerResource(results);
  }

  private ImmutableList<MusicRecording> fetchPlaylist(String playlistId)
      throws IOException, SpotifyWebApiException {
    int offset = 0;
    Paging<PlaylistTrack> playlistTrackResults;
    ImmutableList.Builder<MusicRecording> results = new ImmutableList.Builder<>(); 
    do {
      monitor.debug(() -> "Fetching playlist's %s tracks with offset %s, next: %s",
          playlistId, offset);
      playlistTrackResults = spotifyApi.getPlaylistsTracks(playlistId)
          .offset(offset)
          .build()
          .execute();
      for (PlaylistTrack track : playlistTrackResults.getItems()) {
        results.add(convertTrack(track));
      }
      offset += playlistTrackResults.getItems().length;
    } while (!Strings.isNullOrEmpty(playlistTrackResults.getNext())
        && playlistTrackResults.getItems().length > 0);
    return results.build();
  }

  private MusicRecording convertTrack(PlaylistTrack playlistTrack) {
    Track track = playlistTrack.getTrack();
    monitor.debug(() -> "Converting: %s", playlistTrack);
    return new MusicRecording(
        track.getName(),
        track.getExternalIds().getExternalIds().get("isrc"),
        new MusicAlbum(track.getAlbum().getName()),
        new MusicGroup(track.getArtists()[0].getName()));
  }
}
