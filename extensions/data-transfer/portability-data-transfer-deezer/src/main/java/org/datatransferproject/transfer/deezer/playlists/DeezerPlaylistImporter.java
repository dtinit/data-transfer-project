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


import static com.google.common.base.Preconditions.checkArgument;

import com.google.api.client.http.HttpTransport;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
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

/**
 * Imports playlists into Deezer.
 **/
public class DeezerPlaylistImporter
    implements Importer<TokensAndUrlAuthData, PlaylistContainerResource> {
  private final Monitor monitor;
  private final HttpTransport httpTransport;

  public DeezerPlaylistImporter(Monitor monitor, HttpTransport httpTransport) {
    this.monitor = monitor;
    this.httpTransport = httpTransport;
  }

  @Override
  public ImportResult importItem(UUID jobId, TokensAndUrlAuthData authData,
      PlaylistContainerResource data) throws IOException {
    DeezerApi api = new DeezerApi(authData.getAccessToken(), httpTransport);
    for (MusicPlaylist playlist : data.getLists()) {
      createPlaylist(api, playlist);
    }

    return ImportResult.OK;
  }

  private void createPlaylist(DeezerApi api, MusicPlaylist playlist)
      throws IOException {
    InsertResponse createResponse = api.createPlaylist("Imported - " + playlist.getHeadline());
    if (createResponse.getError() != null) {
      throw new IOException("problem creating playlist: " + playlist + " error: "
          + createResponse.getError());
    }
    List<Long> ids = new ArrayList<>();
    for (MusicRecording track : playlist.getTrack()) {
      ids.add(lookupTrack(api, track));
    }
    Error insertResponse = api.insertTracksInPlaylist(createResponse.getId(), ids);
    if (insertResponse != null) {
      throw new IOException("problem inserting tracks into playlist: " + playlist + " error: "
          + insertResponse);
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
