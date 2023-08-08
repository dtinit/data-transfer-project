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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.json.JsonFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.google.protobuf.util.Durations;
import com.google.rpc.Code;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.musicModels.BatchPlaylistItemRequest;
import org.datatransferproject.datatransfer.google.musicModels.BatchPlaylistItemResponse;
import org.datatransferproject.datatransfer.google.musicModels.CreatePlaylistItemRequest;
import org.datatransferproject.datatransfer.google.musicModels.GoogleArtist;
import org.datatransferproject.datatransfer.google.musicModels.GooglePlaylist;
import org.datatransferproject.datatransfer.google.musicModels.GooglePlaylistItem;
import org.datatransferproject.datatransfer.google.musicModels.GoogleRelease;
import org.datatransferproject.datatransfer.google.musicModels.GoogleTrack;
import org.datatransferproject.datatransfer.google.musicModels.NewPlaylistItemResult;
import org.datatransferproject.datatransfer.google.musicModels.Status;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.spi.transfer.types.CopyException;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.datatransferproject.types.common.models.music.MusicContainerResource;
import org.datatransferproject.types.common.models.music.MusicGroup;
import org.datatransferproject.types.common.models.music.MusicPlaylist;
import org.datatransferproject.types.common.models.music.MusicPlaylistItem;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class GoogleMusicImporter implements Importer<TokensAndUrlAuthData, MusicContainerResource> {

  // TODO(critical WIP-feature step): fine tune the batch size when inserting playlist items
  private static final int PLAYLIST_ITEM_BATCH_SIZE = 49;

  private final GoogleCredentialFactory credentialFactory;
  private final JsonFactory jsonFactory;
  private volatile GoogleMusicHttpApi musicHttpApi;
  private final Map<UUID, GoogleMusicHttpApi> musicHttpApisMap;

  private final Monitor monitor;
  private final double writesPerSecond;

  public GoogleMusicImporter(
      GoogleCredentialFactory credentialFactory,
      JsonFactory jsonFactory,
      Monitor monitor,
      double writesPerSecond) {
    this(credentialFactory, jsonFactory, null, new HashMap<>(), monitor, writesPerSecond);
  }

  @VisibleForTesting
  GoogleMusicImporter(
      GoogleCredentialFactory credentialFactory,
      JsonFactory jsonFactory,
      GoogleMusicHttpApi musicHttpApi,
      Map<UUID, GoogleMusicHttpApi> musicHttpApisMap,
      Monitor monitor,
      double writesPerSecond) {
    this.credentialFactory = credentialFactory;
    this.jsonFactory = jsonFactory;
    this.musicHttpApi = musicHttpApi;
    this.musicHttpApisMap = musicHttpApisMap;
    this.monitor = monitor;
    this.writesPerSecond = writesPerSecond;
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
      return ImportResult.OK;
    }

    // Update playlists
    for (MusicPlaylist playlist : data.getPlaylists()) {
      idempotentImportExecutor.executeAndSwallowIOExceptions(
          playlist.getId(),
          playlist.getTitle(),
          () -> importSinglePlaylist(jobId, authData, playlist));
    }

    // Create playlistItems
    importPlaylistItems(data.getPlaylistItems(), idempotentImportExecutor, jobId, authData);

    // TODO: create tracks

    // TODO: create releases

    return ImportResult.OK;
  }

  @VisibleForTesting
  String importSinglePlaylist(
      UUID jobId, TokensAndUrlAuthData authData, MusicPlaylist inputPlaylist)
      throws IOException, CopyException {
    // Set up GooglePlaylist
    GooglePlaylist googlePlaylist = new GooglePlaylist();
    googlePlaylist.setDescription(inputPlaylist.getDescription());
    googlePlaylist.setTitle(inputPlaylist.getTitle());

    getOrCreateMusicInterface(jobId, authData)
          .createPlaylist(googlePlaylist, inputPlaylist.getId());
    return inputPlaylist.getId();
  }

  void importPlaylistItems(
      List<MusicPlaylistItem> playlistItems,
      IdempotentImportExecutor executor,
      UUID jobId,
      TokensAndUrlAuthData authData)
      throws Exception {
    if (playlistItems != null && !playlistItems.isEmpty()) {
      Map<String, List<MusicPlaylistItem>> playlistItemsByPlaylist =
          playlistItems.stream()
              .filter(playlistItem -> !executor.isKeyCached(playlistItem.toString()))
              .collect(Collectors.groupingBy(MusicPlaylistItem::getPlaylistId));

      for (Entry<String, List<MusicPlaylistItem>> playlistEntry :
          playlistItemsByPlaylist.entrySet()) {
        String originalPlaylistId = playlistEntry.getKey();
        UnmodifiableIterator<List<MusicPlaylistItem>> batches =
            Iterators.partition(playlistEntry.getValue().iterator(), PLAYLIST_ITEM_BATCH_SIZE);
        while (batches.hasNext()) {
          importPlaylistItemBatch(jobId, authData, batches.next(), executor, originalPlaylistId);
        }
      }
    }

    return;
  }

  private void importPlaylistItemBatch(
      UUID jobId,
      TokensAndUrlAuthData authData,
      List<MusicPlaylistItem> playlistItems,
      IdempotentImportExecutor executor,
      String playlistId)
      throws Exception {
    // Note this be null if the playlist create failed, which is what we want
    // because that will also mark this batch of playlist items as being failed.
    if (!executor.isKeyCached(playlistId)) {
      for (MusicPlaylistItem playlistItem : playlistItems) {
        executor.executeAndSwallowIOExceptions(
            playlistItem.toString(),
            playlistItem.toString(),
            () -> {
              throw new IOException(
                  String.format(
                      "Fail to create Playlist %s for PlaylistItem : %s",
                      playlistId, playlistItem));
            });
      }
      return;
    }
    List<CreatePlaylistItemRequest> createPlaylistItemRequests = new ArrayList<>();
    for (MusicPlaylistItem playlistItem : playlistItems) {
      createPlaylistItemRequests.add(
          buildCreatePlaylistItemRequest(playlistItem, playlistId));
    }

    BatchPlaylistItemRequest batchRequest =
        new BatchPlaylistItemRequest(createPlaylistItemRequests, playlistId);
    try {
      BatchPlaylistItemResponse responsePlaylistItem =
          getOrCreateMusicInterface(jobId, authData).createPlaylistItems(batchRequest);
      for (int i = 0; i < responsePlaylistItem.getResults().length; i++) {
        NewPlaylistItemResult playlistItemResult = responsePlaylistItem.getResults()[i];
        // playlistItemResult should be success or skippable failure.
        // TODO(critical WIP-feature step): Replace it with skippable failure support.
        executor.executeAndSwallowIOExceptions(
            playlistItems.get(i).toString(),
            playlistItems.get(i).toString(),
            () -> processNewPlaylistItemResult(playlistItemResult));
      }
    } catch (IOException e) {
      if (StringUtils.contains(e.getMessage(), "permanent failure")) {
        // Permanent Failure: terminate the transfer job and notify the end user
        // TODO(critical WIP-feature step): Add permanent failures.
        throw new CopyException("Permanent Failure:", e);
      } else if (StringUtils.contains(e.getMessage(), "skippable failure")) {
        // Skippable Failure: we skip this batch and log some data to understand it better
        // TODO(critical WIP-feature step): Add skippable failures.
        monitor.info(() -> "Skippable Failure:", e);
      } else {
        // Retryable Failure: retry the batch
        throw e;
      }
    }

    return;
  }

  private String processNewPlaylistItemResult(NewPlaylistItemResult playlistItemResult)
      throws Exception {
    Status status = playlistItemResult.getStatus();
    if (status.getCode() != Code.OK_VALUE) {
      throw new IOException(
          String.format(
              "PlaylistItem could not be created. Code: %d Message: %s",
              status.getCode(), status.getMessage()));
    }
    return playlistItemResult.getPlaylistItem().getTrack().getIsrc();
  }

  private GoogleArtist[] getArtists(List<MusicGroup> artists) {
    if (artists == null || artists.isEmpty()) {
      return null;
    }
    GoogleArtist[] googleArtists = new GoogleArtist[artists.size()];
    for (int i = 0; i < artists.size(); i++) {
      GoogleArtist googleArtist = new GoogleArtist();
      googleArtist.setTitle(artists.get(i).getName());
      googleArtists[i] = googleArtist;
    }
    return googleArtists;
  }

  private CreatePlaylistItemRequest buildCreatePlaylistItemRequest(
      MusicPlaylistItem playlistItem, String parent) {
    GooglePlaylistItem googlePlaylistItem = new GooglePlaylistItem();
    GoogleTrack googleTrack = new GoogleTrack();
    GoogleRelease googleRelease = new GoogleRelease();

    googleRelease.setIcpn(playlistItem.getTrack().getMusicRelease().getIcpnCode());
    googleRelease.setReleaseTitle(playlistItem.getTrack().getMusicRelease().getTitle());
    googleRelease.setArtistTitles(
        getArtists(playlistItem.getTrack().getMusicRelease().getByArtists()));

    googleTrack.setIsrc(playlistItem.getTrack().getIsrcCode());
    googleTrack.setTitle(playlistItem.getTrack().getTitle());
    googleTrack.setArtists(getArtists(playlistItem.getTrack().getByArtists()));
    googleTrack.setDuration(
        Durations.toString(Durations.fromMillis(playlistItem.getTrack().getDurationMillis())));
    googleTrack.setRelease(googleRelease);

    googlePlaylistItem.setTrack(googleTrack);
    googlePlaylistItem.setOrder(playlistItem.getOrder());

    return new CreatePlaylistItemRequest(parent, googlePlaylistItem);
  }

  private synchronized GoogleMusicHttpApi getOrCreateMusicInterface(
      UUID jobId, TokensAndUrlAuthData authData) {

    if (musicHttpApi != null) {
      return musicHttpApi;
    }

    if (musicHttpApisMap.containsKey(jobId)) {
      return musicHttpApisMap.get(jobId);
    }

    GoogleMusicHttpApi newMusicHttpApi = makeMusicHttpApi(authData);
    musicHttpApisMap.put(jobId, newMusicHttpApi);

    return newMusicHttpApi;
  }

  private synchronized GoogleMusicHttpApi makeMusicHttpApi(TokensAndUrlAuthData authData) {
    Credential credential = credentialFactory.createCredential(authData);
    return new GoogleMusicHttpApi(
        credential, jsonFactory, monitor, credentialFactory, writesPerSecond);
  }
}
