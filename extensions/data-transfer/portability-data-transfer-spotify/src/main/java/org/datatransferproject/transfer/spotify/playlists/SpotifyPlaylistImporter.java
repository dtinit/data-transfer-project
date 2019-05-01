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
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.specification.Paging;
import com.wrapper.spotify.model_objects.specification.Track;
import com.wrapper.spotify.model_objects.specification.User;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.provider.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.models.playlists.MusicPlaylist;
import org.datatransferproject.types.common.models.playlists.MusicRecording;
import org.datatransferproject.types.common.models.playlists.PlaylistContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.io.IOException;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Imports playlists into Spotify.
 **/
public class SpotifyPlaylistImporter
    implements Importer<TokensAndUrlAuthData, PlaylistContainerResource> {
  private final Monitor monitor;
  private final SpotifyApi spotifyApi;

  public SpotifyPlaylistImporter(Monitor monitor, SpotifyApi spotifyApi) {
    this.monitor = monitor;
    this.spotifyApi = spotifyApi;
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      TokensAndUrlAuthData authData,
      PlaylistContainerResource data) throws Exception {
    spotifyApi.setAccessToken(authData.getAccessToken());
    spotifyApi.setRefreshToken(authData.getRefreshToken());

    User user = spotifyApi.getCurrentUsersProfile().build().execute();
    for (MusicPlaylist playlist : data.getLists()) {
      createPlaylist(idempotentExecutor, playlist, user.getId());
    }
    return ImportResult.OK;
  }

  private void createPlaylist(IdempotentImportExecutor idempotentExecutor,
      MusicPlaylist playlist,
      String userId)
      throws IOException, SpotifyWebApiException {
    String playlistId = idempotentExecutor.executeAndSwallowExceptions(
        playlist.getIdentifier(),
        "Playlist: " + playlist.getHeadline(),
        () -> spotifyApi
            .createPlaylist(userId, playlist.getHeadline())
            .collaborative(false)
            .public_(false)
            .name("Imported - " + playlist.getHeadline())
            .build()
            .execute()
            .getId());
    if (playlistId != null) {
      for (MusicRecording track : playlist.getTrack()) {
        addTrack(idempotentExecutor, playlistId, playlist.getHeadline(), track);
      }
    }
  }

  private void addTrack(
      IdempotentImportExecutor idempotentExecutor,
      String playlistId,
      String playlistName,
      MusicRecording track)
      throws IOException {
    idempotentExecutor.executeAndSwallowExceptions(
        playlistId + "-" + track.hashCode(),
        playlistName + " - " + track.getHeadline(),
        () -> {
          Track spotifyTrack = searchForSong(track);
          spotifyApi
              .addTracksToPlaylist(playlistId, new String[] {spotifyTrack.getUri()})
              .position(0)
              .build()
              .execute();
          return null;
        }
    );
  }

  private Track searchForSong(MusicRecording track)
      throws IOException, SpotifyWebApiException {
    // TODO: right now this depends on an ISRC being present, we should add fallback
    // logic.
    checkArgument(!Strings.isNullOrEmpty(track.getIsrcCode()), "No ISRC code present for: "
        + track.getHeadline());
    Paging<Track> searchResponse = spotifyApi
        .searchTracks("isrc:" + track.getIsrcCode())
        .build()
        .execute();
    if (searchResponse.getItems().length == 0) {
      throw new IOException("Couldn't find track: " + track.getHeadline()
          + " with code: " + track.getIsrcCode());
    }
    return searchResponse.getItems()[0];
  }
}
