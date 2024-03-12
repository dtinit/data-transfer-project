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

package org.datatransferproject.datatransfer.apple.music.data.converters;

import org.apache.commons.lang3.StringUtils;
import org.datatransferproject.datatransfer.apple.music.musicproto.MusicProtocol;
import org.datatransferproject.types.common.models.music.MusicGroup;
import org.datatransferproject.types.common.models.music.MusicPlaylist;
import org.datatransferproject.types.common.models.music.MusicPlaylistItem;
import org.datatransferproject.types.common.models.music.MusicRecording;
import org.datatransferproject.types.common.models.music.MusicRelease;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Helper class for converting incoming DTP format playlists and playlist-tracks to Apple Music format for transport to
 * Apple Music Data Transfer services.
 */
public class AppleMusicPlaylistConverter {

    public static List<MusicProtocol.MusicTrack> convertToAppleMusicPlaylistTracks(Collection<MusicPlaylistItem> musicPlaylistItems) {
        List<MusicProtocol.MusicTrack> convertedTracks = new ArrayList<>();
        for (MusicPlaylistItem item : musicPlaylistItems) {
            if (item == null) {
                throw new IllegalStateException("MusicPlaylistItem cannot be null");
            }

            if (StringUtils.isBlank(item.getPlaylistId())) {
                throw new IllegalStateException("MusicPlaylistIte::getPlaylistId cannot be blank");
            }

            MusicProtocol.MusicTrack.Builder trackBuilder = convertMusicRecordingToTrackBuilder(item.getTrack());

            trackBuilder.setPlaylistId(item.getPlaylistId())
                    .setPlaylistPosition(item.getOrder());

            convertedTracks.add(trackBuilder.build());
        }

        return convertedTracks;
    }

    public static List<MusicProtocol.MusicPlaylist> convertToAppleMusicPlaylist(Collection<MusicPlaylist> musicPlaylists) {
        List<MusicProtocol.MusicPlaylist> convertedPlaylists = new ArrayList<>();
        for (MusicPlaylist playlist : musicPlaylists) {
            if (playlist == null) {
                throw new IllegalStateException("MusicPlaylist cannot be null");
            }

            MusicProtocol.MusicPlaylist.Builder playlistBuilder = MusicProtocol.MusicPlaylist.newBuilder();

            if (!StringUtils.isBlank(playlist.getId())) {
                playlistBuilder.setId(playlist.getId());
            }

            if (!StringUtils.isBlank(playlist.getTitle())) {
                playlistBuilder.setTitle(playlist.getTitle());
            }

            if (!StringUtils.isBlank(playlist.getDescription())) {
                playlistBuilder.setDescription(playlist.getDescription());
            }

            if (playlist.getTimeCreated() != null) {
                playlistBuilder.setTimeCreated(playlist.getTimeCreated().toEpochMilli());
            }

            if (playlist.getTimeUpdated() != null) {
                playlistBuilder.setTimeUpdated(playlist.getTimeUpdated().toEpochMilli());
            }

            convertedPlaylists.add(playlistBuilder.build());
        }

        return convertedPlaylists;
    }

    public static MusicProtocol.MusicTrack.Builder convertMusicRecordingToTrackBuilder(MusicRecording recording) {
        MusicProtocol.MusicTrack.Builder trackBuilder = MusicProtocol.MusicTrack.newBuilder();

        if (recording == null) {
            // Return an empty track
            // Tracks without metadata will be ignored by importing system.
            return trackBuilder;
        }

        if (!StringUtils.isBlank(recording.getTitle())) {
            trackBuilder.setTitle(recording.getTitle());
        }

        if (!StringUtils.isBlank(recording.getIsrcCode())) {
            trackBuilder.setIsrcCode(recording.getIsrcCode());
        }

        trackBuilder.setDurationMillis(recording.getDurationMillis());

        if (recording.getByArtists() != null && !recording.getByArtists().isEmpty()) {
            List<MusicProtocol.MusicArtist> trackArtists = convertMusicGroupsToMusicArtists(recording.getByArtists());
            if (!trackArtists.isEmpty()) {
                trackBuilder.addAllTrackArtist(trackArtists);
            }
        }

        if (recording.getMusicRelease() != null) {
            trackBuilder.setMusicAlbum(convertMusicReleaseToMusicAlbum(recording.getMusicRelease()));
        }

        trackBuilder.setIsExplicit(recording.getIsExplicit());

        return trackBuilder;
    }

    public static MusicProtocol.MusicAlbum convertMusicReleaseToMusicAlbum(MusicRelease release) {
        MusicProtocol.MusicAlbum.Builder albumBuilder = MusicProtocol.MusicAlbum.newBuilder();

        if (release == null) {
            return albumBuilder.build();
        }

        if (!StringUtils.isBlank(release.getTitle())) {
            albumBuilder.setTitle(release.getTitle());
        }

        if (!StringUtils.isBlank(release.getIcpnCode())) {
            albumBuilder.setIcpnCode(release.getIcpnCode());
        }

        if (release.getByArtists() != null && !release.getByArtists().isEmpty()) {
            List<MusicProtocol.MusicArtist> albumArtists = convertMusicGroupsToMusicArtists(release.getByArtists());
            albumBuilder.addAllAlbumArtist(albumArtists);
        }

        return albumBuilder.build();
    }

    public static List<MusicProtocol.MusicArtist> convertMusicGroupsToMusicArtists(Collection<MusicGroup> musicGroups) {

        if (musicGroups == null || musicGroups.isEmpty()) {
            return Collections.emptyList();
        }

        List<MusicProtocol.MusicArtist> musicArtists = new ArrayList<>();
        for (MusicGroup musicGroup : musicGroups) {
            if (!StringUtils.isBlank(musicGroup.getName())) {
                MusicProtocol.MusicArtist artist = MusicProtocol.MusicArtist.newBuilder()
                        .setName(musicGroup.getName())
                        .build();
                musicArtists.add(artist);
            }
        }
        return musicArtists;
    }
}
