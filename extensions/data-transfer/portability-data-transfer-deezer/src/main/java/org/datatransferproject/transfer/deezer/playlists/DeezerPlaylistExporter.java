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
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.transfer.deezer.DeezerApi;
import org.datatransferproject.transfer.deezer.model.PlaylistDetails;
import org.datatransferproject.transfer.deezer.model.PlaylistSummary;
import org.datatransferproject.transfer.deezer.model.Track;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.models.playlists.MusicAlbum;
import org.datatransferproject.types.common.models.playlists.MusicGroup;
import org.datatransferproject.types.common.models.playlists.MusicPlaylist;
import org.datatransferproject.types.common.models.playlists.MusicRecording;
import org.datatransferproject.types.common.models.playlists.PlaylistContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

/**
 * Exports playlists from Deezer.
 **/
public class DeezerPlaylistExporter implements
    Exporter<TokensAndUrlAuthData, PlaylistContainerResource> {

  private final Monitor monitor;
  private final HttpTransport httpTransport;

  public DeezerPlaylistExporter(Monitor monitor, HttpTransport httpTransport) {
    this.monitor = monitor;
    this.httpTransport = httpTransport;
  }

  @Override
  public ExportResult<PlaylistContainerResource> export(UUID jobId,
      TokensAndUrlAuthData authData, Optional<ExportInformation> exportInformation)
      throws Exception {
    DeezerApi api = new DeezerApi(authData.getAccessToken(), httpTransport);

    return new ExportResult<>(
        ResultType.END, enumeratePlaylists(api));
  }

  private PlaylistContainerResource enumeratePlaylists(DeezerApi api)
      throws IOException {
    List<MusicPlaylist> results = new ArrayList<>();
    for (PlaylistSummary playlistSummary : api.getPlaylists()) {
      results.add(new MusicPlaylist(
          playlistSummary.getTitle(),
          fetchPlaylist(api, playlistSummary.getId())));
    }
    return new PlaylistContainerResource(results);
  }

  private ImmutableList<MusicRecording> fetchPlaylist(DeezerApi api, long playlistId)
      throws IOException {

    ImmutableList.Builder<MusicRecording> results = new ImmutableList.Builder<>();

      monitor.debug(() -> "Fetching playlist's %s tracks", playlistId);
      PlaylistDetails playlistDetails = api.getPlaylistDetails(playlistId);
      for (Track track : playlistDetails.getTrackCollection().getTracks()) {
        results.add(convertTrack(api, track.getId()));
      }
    return results.build();
  }

  private MusicRecording convertTrack(DeezerApi api, long trackId) throws IOException {
    Track track = api.getTrack(trackId);
    return new MusicRecording(
        track.getTitle(),
        track.getIsrc(),
        new MusicAlbum(track.getAlbum().getTitle()),
        new MusicGroup(track.getAlbum().getTitle()));
  }
}
