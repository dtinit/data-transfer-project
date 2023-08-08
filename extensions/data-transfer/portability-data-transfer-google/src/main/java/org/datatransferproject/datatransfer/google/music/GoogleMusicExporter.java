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
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.IOException;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.musicModels.GoogleArtist;
import org.datatransferproject.datatransfer.google.musicModels.GooglePlaylist;
import org.datatransferproject.datatransfer.google.musicModels.GooglePlaylistItem;
import org.datatransferproject.datatransfer.google.musicModels.GoogleRelease;
import org.datatransferproject.datatransfer.google.musicModels.GoogleTrack;
import org.datatransferproject.datatransfer.google.musicModels.PlaylistItemListResponse;
import org.datatransferproject.datatransfer.google.musicModels.PlaylistListResponse;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.PaginationData;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.IdOnlyContainerResource;
import org.datatransferproject.types.common.models.music.MusicContainerResource;
import org.datatransferproject.types.common.models.music.MusicGroup;
import org.datatransferproject.types.common.models.music.MusicPlaylist;
import org.datatransferproject.types.common.models.music.MusicPlaylistItem;
import org.datatransferproject.types.common.models.music.MusicRecording;
import org.datatransferproject.types.common.models.music.MusicRelease;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class GoogleMusicExporter implements Exporter<TokensAndUrlAuthData, MusicContainerResource> {

  static final String PLAYLIST_TRACK_RELEASE_TOKEN_PREFIX = "playlist:track:release:";
  static final String PLAYLIST_TRACK_TOKEN_PREFIX = "playlist:track:";
  static final String PLAYLIST_RELEASE_TOKEN_PREFIX = "playlist:release:";
  static final String TRACK_RELEASE_TOKEN_PREFIX = "track:release:";
  static final String PLAYLIST_TOKEN_PREFIX = "playlist:";
  static final String TRACK_TOKEN_PREFIX = "track:";
  static final String RELEASE_TOKEN_PREFIX = "release:";

  static final String GOOGLE_PLAYLIST_NAME_PREFIX = "playlists/";

  private final GoogleCredentialFactory credentialFactory;
  private final JsonFactory jsonFactory;
  private volatile GoogleMusicHttpApi musicHttpApi;

  private final Monitor monitor;

  public GoogleMusicExporter(
      GoogleCredentialFactory credentialFactory, JsonFactory jsonFactory, Monitor monitor) {
    this.credentialFactory = credentialFactory;
    this.jsonFactory = jsonFactory;
    this.monitor = monitor;
  }

  @VisibleForTesting
  GoogleMusicExporter(
      GoogleCredentialFactory credentialFactory,
      JsonFactory jsonFactory,
      GoogleMusicHttpApi musicHttpApi,
      Monitor monitor) {
    this.credentialFactory = credentialFactory;
    this.jsonFactory = jsonFactory;
    this.musicHttpApi = musicHttpApi;
    this.monitor = monitor;
  }

  @Override
  public ExportResult<MusicContainerResource> export(
      UUID jobId, TokensAndUrlAuthData authData, Optional<ExportInformation> exportInformation)
      throws IOException, InvalidTokenException, PermissionDeniedException, ParseException {
    // TODO: Remove the logic when testing is finished.
    // Local demo-server test usage. Start transfer job without ExportInformation.
    if (!exportInformation.isPresent()) {
      StringPaginationToken paginationToken = new StringPaginationToken(PLAYLIST_TOKEN_PREFIX);
      return exportPlaylists(authData, Optional.of(paginationToken), jobId);
    }

    if (exportInformation.get().getContainerResource() instanceof IdOnlyContainerResource) {
      // if ExportInformation is an id only container, this is a request to export playlist items.
      return exportPlaylistItems(
          authData,
          (IdOnlyContainerResource) exportInformation.get().getContainerResource(),
          Optional.ofNullable(exportInformation.get().getPaginationData()), jobId);
    }

    StringPaginationToken paginationToken =
        (StringPaginationToken) exportInformation.get().getPaginationData();

    boolean paginationDataPresent = paginationToken != null;

    if (paginationDataPresent && paginationToken.getToken().startsWith(PLAYLIST_TOKEN_PREFIX)) {
      return exportPlaylists(authData, Optional.of(paginationToken), jobId);
    } else if (paginationDataPresent && paginationToken.getToken().startsWith(TRACK_TOKEN_PREFIX)) {
      // TODO: export tracks
      return new ExportResult<>(ResultType.END, null, null);
    } else if (paginationDataPresent
        && paginationToken.getToken().startsWith(RELEASE_TOKEN_PREFIX)) {
      // TODO: export releases
      return new ExportResult<>(ResultType.END, null, null);
    } else {
      // There is nothing to export.
      return new ExportResult<>(ResultType.END, null, null);
    }
  }

  // TODO(@jzacsh): Replace pageTokenPrefix and paginationToken with simplified and general class or
  // functions.
  @VisibleForTesting
  ExportResult<MusicContainerResource> exportPlaylists(
      TokensAndUrlAuthData authData, Optional<PaginationData> paginationData, UUID jobId)
      throws IOException, InvalidTokenException, PermissionDeniedException {
    Optional<String> paginationToken = Optional.empty();
    String pageTokenPrefix = "";
    if (paginationData.isPresent()) {
      String token = ((StringPaginationToken) paginationData.get()).getToken();
      Preconditions.checkArgument(
          token.startsWith(PLAYLIST_TOKEN_PREFIX), "Invalid pagination token %s", token);
      pageTokenPrefix = token.substring(0, getTokenPrefixLength(token));
      if (getTokenPrefixLength(token) < token.length()) {
        paginationToken = Optional.of(token.substring(getTokenPrefixLength(token)));
      }
    }

    PlaylistListResponse playlistListResponse =
        getOrCreateMusicHttpApi(authData).listPlaylists(paginationToken);

    PaginationData nextPageData;
    String token = playlistListResponse.getNextPageToken();
    List<MusicPlaylist> playlists = new ArrayList<>();
    GooglePlaylist[] googlePlaylists = playlistListResponse.getPlaylists();
    ResultType resultType = ResultType.END;

    if (Strings.isNullOrEmpty(token)) {
      nextPageData =
          new StringPaginationToken(pageTokenPrefix.substring(PLAYLIST_TOKEN_PREFIX.length()));
    } else {
      nextPageData = new StringPaginationToken(pageTokenPrefix + token);
      resultType = ResultType.CONTINUE;
    }
    ContinuationData continuationData = new ContinuationData(nextPageData);

    if (googlePlaylists != null && googlePlaylists.length > 0) {
      for (GooglePlaylist googlePlaylist : googlePlaylists) {
        Instant createTime = googlePlaylist.getCreateTime() == null ? null
            : Instant.parse(googlePlaylist.getCreateTime());
        Instant updateTime = googlePlaylist.getUpdateTime() == null ? null
            : Instant.parse(googlePlaylist.getUpdateTime());
        MusicPlaylist musicPlaylist =
            new MusicPlaylist(
                googlePlaylist.getName().substring(GOOGLE_PLAYLIST_NAME_PREFIX.length()),
                googlePlaylist.getTitle(),
                googlePlaylist.getDescription(),
                createTime, updateTime);
        playlists.add(musicPlaylist);

        monitor.debug(
            () ->
                String.format(
                    "%s: Google Music exporting playlist: %s %s", jobId, musicPlaylist.getId(),
                    musicPlaylist.getTimeCreated().toString()));

        // Add playlist id to continuation data
        continuationData.addContainerResource(new IdOnlyContainerResource(musicPlaylist.getId()));
      }
    }

    MusicContainerResource containerResource =
        new MusicContainerResource(playlists, null, null, null);
    return new ExportResult<>(resultType, containerResource, continuationData);
  }

  @VisibleForTesting
  ExportResult<MusicContainerResource> exportPlaylistItems(
      TokensAndUrlAuthData authData,
      IdOnlyContainerResource playlistData,
      Optional<PaginationData> paginationData, UUID jobId)
      throws IOException, InvalidTokenException, PermissionDeniedException, ParseException {
    String playlistId = playlistData.getId();
    Optional<String> paginationToken =
        paginationData.map((PaginationData value) -> ((StringPaginationToken) value).getToken());

    PlaylistItemListResponse playlistItemListResponse =
        getOrCreateMusicHttpApi(authData).listPlaylistItems(playlistId, paginationToken);

    PaginationData nextPageData = null;
    if (!Strings.isNullOrEmpty(playlistItemListResponse.getNextPageToken())) {
      nextPageData = new StringPaginationToken(playlistItemListResponse.getNextPageToken());
    }
    ContinuationData continuationData = new ContinuationData(nextPageData);

    MusicContainerResource containerResource = null;
    GooglePlaylistItem[] googlePlaylistItems = playlistItemListResponse.getPlaylistItems();
    List<MusicPlaylistItem> playlistItems = new ArrayList<>();
    if (googlePlaylistItems != null && googlePlaylistItems.length > 0) {
      for (GooglePlaylistItem googlePlaylistItem : googlePlaylistItems) {
        playlistItems.add(convertPlaylistItem(playlistId, googlePlaylistItem));
        monitor.debug(
            () ->
                String.format(
                    "%s: Google Music exporting playlist item in %s : %s", jobId, playlistId,
                    googlePlaylistItem.getTrack().getArtists().length));
      }
      containerResource = new MusicContainerResource(null, playlistItems, null, null);
    }

    return new ExportResult<>(ResultType.CONTINUE, containerResource, continuationData);
  }

  private int getTokenPrefixLength(String token) {
    final ImmutableList<String> knownPrefixes =
        ImmutableList.of(
            PLAYLIST_TRACK_RELEASE_TOKEN_PREFIX,
            PLAYLIST_TRACK_TOKEN_PREFIX,
            PLAYLIST_RELEASE_TOKEN_PREFIX,
            PLAYLIST_TOKEN_PREFIX,
            TRACK_RELEASE_TOKEN_PREFIX,
            TRACK_TOKEN_PREFIX,
            RELEASE_TOKEN_PREFIX);

    for (String prefix : knownPrefixes) {
      if (token.startsWith(prefix)) {
        return prefix.length();
      }
    }
    return 0;
  }

  private List<MusicGroup> createMusicGroups(GoogleArtist[] artists) {
    if (artists == null) {
      return null;
    }
    List<MusicGroup> musicGroups = new ArrayList<>();
    for (GoogleArtist artist : artists) {
      musicGroups.add(new MusicGroup(artist.getTitle()));
    }
    return musicGroups;
  }

  private MusicPlaylistItem convertPlaylistItem(
      String playlistId, GooglePlaylistItem googlePlaylistItem)
      throws InvalidProtocolBufferException, ParseException {
    GoogleTrack track = googlePlaylistItem.getTrack();
    GoogleRelease release = track.getRelease();
    return new MusicPlaylistItem(
        new MusicRecording(
            track.getIsrc(),
            track.getTitle(),
            track.convertDurationToMillions(),
            new MusicRelease(
                release.getIcpn(),
                release.getTitle(),
                createMusicGroups(release.getArtists())),
            createMusicGroups(track.getArtists())),
        playlistId,
        googlePlaylistItem.getOrder());
  }

  private synchronized GoogleMusicHttpApi getOrCreateMusicHttpApi(TokensAndUrlAuthData authData) {
    return musicHttpApi == null ? makeMusicHttpApi(authData) : musicHttpApi;
  }

  private synchronized GoogleMusicHttpApi makeMusicHttpApi(TokensAndUrlAuthData authData) {
    Credential credential = credentialFactory.createCredential(authData);
    return new GoogleMusicHttpApi(
        credential, jsonFactory, monitor, credentialFactory, /* arbitrary writesPerSecond */ 1.0);
  }
}
