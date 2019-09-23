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

package org.datatransferproject.transfer.deezer.playlists;


import com.google.api.client.http.HttpTransport;
import com.google.common.base.Strings;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.deezer.DeezerApi;
import org.datatransferproject.transfer.deezer.model.Error;
import org.datatransferproject.transfer.deezer.model.InsertResponse;
import org.datatransferproject.transfer.deezer.model.Track;
import org.datatransferproject.types.common.models.playlists.MusicPlaylist;
import org.datatransferproject.types.common.models.playlists.MusicRecording;
import org.datatransferproject.types.common.models.playlists.PlaylistContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.transfer.serviceconfig.TransferServiceConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

/**
 * Imports playlists into Deezer.
 **/
public class DeezerPlaylistImporter
    implements Importer<TokensAndUrlAuthData, PlaylistContainerResource> {
  private final Monitor monitor;
  private final HttpTransport httpTransport;
  private final TransferServiceConfig transferServiceConfig;

  public DeezerPlaylistImporter(
      Monitor monitor,
      HttpTransport httpTransport,
      TransferServiceConfig transferServiceConfig) {
    this.monitor = monitor;
    this.httpTransport = httpTransport;
    this.transferServiceConfig = transferServiceConfig;
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      TokensAndUrlAuthData authData,
      PlaylistContainerResource data) throws Exception {
    DeezerApi api = new DeezerApi(
        authData.getAccessToken(),
        httpTransport,
        transferServiceConfig);
    for (MusicPlaylist playlist : data.getLists()) {
      createPlaylist(idempotentExecutor, api, playlist);
    }
    return ImportResult.OK;
  }

  private void createPlaylist(
      IdempotentImportExecutor idempotentExecutor,
      DeezerApi api,
      MusicPlaylist playlist)
      throws Exception {
    Long newPlaylistId = idempotentExecutor.executeAndSwallowIOExceptions(
        playlist.getIdentifier(),
        playlist.getHeadline(),
        () -> createPlaylist(api, playlist));
    if (null == newPlaylistId) {
      monitor.severe(() -> format("Couldn't create playlist: %s", playlist));
      // Playlist couldn't be created error will be reported to user.
      return;
    }
    List<Long> ids = new ArrayList<>();
    for (MusicRecording track : playlist.getTrack()) {
      Long newSongId = idempotentExecutor.executeAndSwallowIOExceptions(
          newPlaylistId + "-" + track.hashCode(),
          "Track: " + track + " in " + playlist.getHeadline(),
          () -> lookupTrack(api, track));
      ids.add(newSongId);
    }
    idempotentExecutor.executeAndSwallowIOExceptions(
        newPlaylistId + "-tracks",
        "Playlist: " + playlist.getHeadline(),
        () -> {
          Error insertResponse = api.insertTracksInPlaylist(newPlaylistId, ids);
          if (insertResponse != null) {
            throw new IOException("problem inserting tracks into playlist: " + playlist + " error: "
                + insertResponse);
          }
          return null;
        }
    );
  }

  private Long createPlaylist(DeezerApi api, MusicPlaylist playlist) {
    try {
      InsertResponse createResponse = api.createPlaylist("Imported - " + playlist.getHeadline());
      if (createResponse.getError() != null) {
        throw new IOException("problem creating playlist: " + playlist + " error: "
            + createResponse.getError());
      }
      if (createResponse.getError() != null) {
        throw new IOException("Problem creating playlist: "
            + playlist + ": " + createResponse.getError());
      }

      return createResponse.getId();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private Long lookupTrack(DeezerApi api, MusicRecording track) throws IOException {
    checkArgument(!Strings.isNullOrEmpty(track.getIsrcCode()), "IRCS code is required");
    Track foundTrack = api.lookupTrackByIsrc(track.getIsrcCode());
    if (foundTrack == null) {
      throw new IllegalArgumentException("Couldn't find matching Deezer track for: " + track);
    }
    return foundTrack.getId();
  }
}
