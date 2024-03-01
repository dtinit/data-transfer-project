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

import org.apache.commons.lang3.RandomUtils;
import org.datatransferproject.datatransfer.apple.music.musicproto.MusicProtocol;
import org.datatransferproject.types.common.models.music.MusicGroup;
import org.datatransferproject.types.common.models.music.MusicPlaylist;
import org.datatransferproject.types.common.models.music.MusicPlaylistItem;
import org.datatransferproject.types.common.models.music.MusicRecording;
import org.datatransferproject.types.common.models.music.MusicRelease;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AppleMusicPlaylistConverterTest {

    @Test
    public void testConvertToAppleMusicPlaylistTracks() {
        String expectedISRC = "1234567890ab";
        String expectedTitle = "Expected Track Title";
        long expectedDurationMillis = RandomUtils.nextLong();
        String expectedArtistName = "Expected Artist Name";
        MusicGroup musicGroup = new MusicGroup(expectedArtistName);
        String expectedICPN = "1234567890abcdefg";
        String expectedAlbumTitle = "Expected Album Title";
        MusicRelease musicRelease = new MusicRelease(expectedICPN, expectedAlbumTitle, List.of(musicGroup));
        boolean expectedIsExplicit = true;
        MusicRecording musicRecording = new MusicRecording(expectedISRC, expectedTitle, expectedDurationMillis, musicRelease, List.of(musicGroup), expectedIsExplicit);
        String expectedPlaylistId = "ExpectedPlaylist.ID";
        int expectedOrder = 1;
        String invalidPlaylistId = " ";

        MusicPlaylistItem musicPlaylistItem = new MusicPlaylistItem(musicRecording, expectedPlaylistId, expectedOrder);

        List<MusicPlaylistItem> playlistItems = new ArrayList<>();
        playlistItems.add(musicPlaylistItem);
        playlistItems.add(null);

        // One null playlist item
        List<MusicProtocol.MusicTrack> oneNullMusicTracks = AppleMusicPlaylistConverter.convertToAppleMusicPlaylistTracks(playlistItems);
        Assertions.assertNotNull(oneNullMusicTracks);
        Assertions.assertFalse(oneNullMusicTracks.isEmpty());
        Assertions.assertEquals(oneNullMusicTracks.size(), 1);
        MusicProtocol.MusicTrack validMusicTrack = oneNullMusicTracks.get(0);
        Assertions.assertNotNull(validMusicTrack);
        Assertions.assertTrue(validMusicTrack.hasPlaylistId());
        Assertions.assertEquals(validMusicTrack.getPlaylistId(), expectedPlaylistId);
        Assertions.assertTrue(validMusicTrack.hasPlaylistPosition());
        Assertions.assertEquals(validMusicTrack.getPlaylistPosition(), expectedOrder);
        // Tested in testConvertMusicRecordingToTrackBuilder
        Assertions.assertTrue(validMusicTrack.hasMusicAlbum());
        Assertions.assertTrue(validMusicTrack.hasIsrcCode());
        Assertions.assertTrue(validMusicTrack.hasTitle());
        Assertions.assertTrue(validMusicTrack.hasDurationMillis());
        Assertions.assertTrue(validMusicTrack.hasIsExplicit());
        Assertions.assertEquals(validMusicTrack.getIsExplicit(), expectedIsExplicit);

        // One playlist item with invalid playlist id
        MusicPlaylistItem invalidPlaylistIdItem = new MusicPlaylistItem(musicRecording, invalidPlaylistId, expectedOrder);
        List<MusicProtocol.MusicTrack> oneInvalidMusicTrack = AppleMusicPlaylistConverter.convertToAppleMusicPlaylistTracks(List.of(musicPlaylistItem, invalidPlaylistIdItem));
        Assertions.assertNotNull(oneInvalidMusicTrack);
        Assertions.assertFalse(oneInvalidMusicTrack.isEmpty());
        Assertions.assertEquals(oneInvalidMusicTrack.size(), 1);
        MusicProtocol.MusicTrack validMusicTrack2 = oneInvalidMusicTrack.get(0);
        Assertions.assertNotNull(validMusicTrack2);
        Assertions.assertTrue(validMusicTrack2.hasPlaylistId());
        Assertions.assertEquals(validMusicTrack2.getPlaylistId(), expectedPlaylistId);
        Assertions.assertTrue(validMusicTrack2.hasPlaylistPosition());
        Assertions.assertEquals(validMusicTrack2.getPlaylistPosition(), expectedOrder);
        // Tested in testConvertMusicRecordingToTrackBuilder
        Assertions.assertTrue(validMusicTrack2.hasMusicAlbum());
        Assertions.assertTrue(validMusicTrack2.hasIsrcCode());
        Assertions.assertTrue(validMusicTrack2.hasTitle());
        Assertions.assertTrue(validMusicTrack2.hasDurationMillis());
        Assertions.assertTrue(validMusicTrack2.hasIsExplicit());
        Assertions.assertEquals(validMusicTrack2.getIsExplicit(), expectedIsExplicit);
    }

    @Test
    public void testConvertToAppleMusicPlaylist() {
        String expectedId1 = "expectedId1";
        String expectedTitle1 = "Expected Title 1";
        String expectedDescription1 = "Expected Description";
        Instant expectedTimeCreated1 = Instant.now();
        Instant expectedTimeUpdated1 = expectedTimeCreated1.plusMillis(10000L);

        // No playlists
        List<MusicProtocol.MusicPlaylist> emptyCollectionMusicPlaylists = AppleMusicPlaylistConverter.convertToAppleMusicPlaylist(List.of());
        Assertions.assertNotNull(emptyCollectionMusicPlaylists);
        Assertions.assertTrue(emptyCollectionMusicPlaylists.isEmpty());

        // One playlist and one null
        MusicPlaylist fullMusicPlaylist = new MusicPlaylist(expectedId1, expectedTitle1, expectedDescription1, expectedTimeCreated1, expectedTimeUpdated1);
        List<MusicPlaylist> musicPlaylists = new ArrayList<>();
        musicPlaylists.add(null);
        musicPlaylists.add(fullMusicPlaylist);
        List<MusicProtocol.MusicPlaylist> oneValidPlaylistMusicPlaylists = AppleMusicPlaylistConverter.convertToAppleMusicPlaylist(musicPlaylists);
        Assertions.assertNotNull(oneValidPlaylistMusicPlaylists);
        Assertions.assertFalse(oneValidPlaylistMusicPlaylists.isEmpty());
        MusicProtocol.MusicPlaylist validMusicPlaylist = oneValidPlaylistMusicPlaylists.get(0);
        Assertions.assertNotNull(validMusicPlaylist);
        Assertions.assertTrue(validMusicPlaylist.hasId());
        Assertions.assertEquals(validMusicPlaylist.getId(), expectedId1);
        Assertions.assertTrue(validMusicPlaylist.hasDescription());
        Assertions.assertEquals(validMusicPlaylist.getDescription(), expectedDescription1);
        Assertions.assertTrue(validMusicPlaylist.hasTimeCreated());
        Assertions.assertEquals(validMusicPlaylist.getTimeCreated(), expectedTimeCreated1.toEpochMilli());
        Assertions.assertTrue(validMusicPlaylist.hasTimeUpdated());
        Assertions.assertEquals(validMusicPlaylist.getTimeUpdated(), expectedTimeUpdated1.toEpochMilli());

        // Totally invalid MusicPlaylist
        MusicPlaylist invalidMusicPlaylist = new MusicPlaylist(expectedId1, null, null, null, null);
        List<MusicProtocol.MusicPlaylist> invalidMusicPlaylists = AppleMusicPlaylistConverter.convertToAppleMusicPlaylist(List.of(invalidMusicPlaylist));
        Assertions.assertNotNull(invalidMusicPlaylists);
        Assertions.assertFalse(invalidMusicPlaylists.isEmpty());
        MusicProtocol.MusicPlaylist invalidMusicPlaylistResult = invalidMusicPlaylists.get(0);
        Assertions.assertNotNull(invalidMusicPlaylistResult);
        Assertions.assertTrue(invalidMusicPlaylistResult.hasId());
        Assertions.assertEquals(invalidMusicPlaylistResult.getId(), expectedId1);
        Assertions.assertFalse(invalidMusicPlaylistResult.hasDescription());
        Assertions.assertFalse(invalidMusicPlaylistResult.hasTimeCreated());
        Assertions.assertFalse(invalidMusicPlaylistResult.hasTimeUpdated());
    }

    @Test
    public void testConvertMusicRecordingToTrackBuilder() {
        String expectedISRC = "1234567890ab";
        String expectedTitle = "Expected Track Title";
        long expectedDurationMillis = RandomUtils.nextLong();
        String expectedArtistName = "Expected Artist Name";
        MusicGroup musicGroup = new MusicGroup(expectedArtistName);
        String expectedICPN = "1234567890abcdefg";
        String expectedAlbumTitle = "Expected Album Title";
        MusicRelease musicRelease = new MusicRelease(expectedICPN, expectedAlbumTitle, List.of(musicGroup));
        boolean expectedIsExplicit = true;

        // It isn't expected that we would get a null MusicRecording, but we should handle it gracefully if we do.
        MusicProtocol.MusicTrack nullMusicRecordingMusicTrack = AppleMusicPlaylistConverter.convertMusicRecordingToTrackBuilder(null).build();
        Assertions.assertNotNull(nullMusicRecordingMusicTrack);
        Assertions.assertFalse(nullMusicRecordingMusicTrack.hasPlaylistId()); // Base builder has no playlist id
        Assertions.assertFalse(nullMusicRecordingMusicTrack.hasPlaylistPosition()); // Base builder has no playlist position
        Assertions.assertFalse(nullMusicRecordingMusicTrack.hasIsrcCode());
        Assertions.assertFalse(nullMusicRecordingMusicTrack.hasTitle());
        Assertions.assertFalse(nullMusicRecordingMusicTrack.hasDurationMillis());
        Assertions.assertFalse(nullMusicRecordingMusicTrack.hasMusicAlbum());
        Assertions.assertEquals(nullMusicRecordingMusicTrack.getTrackArtistCount(), 0);
        Assertions.assertEquals(nullMusicRecordingMusicTrack.getIsExplicit(), false);


        MusicRecording emptyISRCMusicRecording = new MusicRecording(null, expectedTitle, expectedDurationMillis, musicRelease, List.of(musicGroup), false);
        MusicProtocol.MusicTrack emptyISRCMusicTrack = AppleMusicPlaylistConverter.convertMusicRecordingToTrackBuilder(emptyISRCMusicRecording).build();
        Assertions.assertNotNull(emptyISRCMusicTrack);
        Assertions.assertFalse(emptyISRCMusicTrack.hasPlaylistId()); // Base builder has no playlist id
        Assertions.assertFalse(emptyISRCMusicTrack.hasPlaylistPosition()); // Base builder has no playlist position
        Assertions.assertFalse(emptyISRCMusicTrack.hasIsrcCode());
        Assertions.assertTrue(emptyISRCMusicTrack.hasTitle());
        Assertions.assertEquals(emptyISRCMusicTrack.getTitle(), expectedTitle);
        Assertions.assertTrue(emptyISRCMusicTrack.hasDurationMillis());
        Assertions.assertEquals(emptyISRCMusicTrack.getDurationMillis(), expectedDurationMillis);
        Assertions.assertTrue(emptyISRCMusicTrack.hasMusicAlbum());
        Assertions.assertEquals(emptyISRCMusicTrack.getMusicAlbum().getTitle(), expectedAlbumTitle);
        Assertions.assertEquals(emptyISRCMusicTrack.getTrackArtistCount(), 1);
        Assertions.assertEquals(emptyISRCMusicTrack.getIsExplicit(), false);


        MusicRecording emptyTitleMusicRecording = new MusicRecording(expectedISRC, null, expectedDurationMillis, musicRelease, List.of(musicGroup), false);
        MusicProtocol.MusicTrack emptyTitleMusicTrack = AppleMusicPlaylistConverter.convertMusicRecordingToTrackBuilder(emptyTitleMusicRecording).build();
        Assertions.assertNotNull(emptyTitleMusicTrack);
        Assertions.assertFalse(emptyTitleMusicTrack.hasPlaylistId()); // Base builder has no playlist id
        Assertions.assertFalse(emptyTitleMusicTrack.hasPlaylistPosition()); // Base builder has no playlist position
        Assertions.assertTrue(emptyTitleMusicTrack.hasIsrcCode());
        Assertions.assertEquals(emptyTitleMusicTrack.getIsrcCode(), expectedISRC);
        Assertions.assertFalse(emptyTitleMusicTrack.hasTitle());
        Assertions.assertTrue(emptyTitleMusicTrack.hasDurationMillis());
        Assertions.assertEquals(emptyTitleMusicTrack.getDurationMillis(), expectedDurationMillis);
        Assertions.assertTrue(emptyTitleMusicTrack.hasMusicAlbum());
        Assertions.assertEquals(emptyTitleMusicTrack.getMusicAlbum().getTitle(), expectedAlbumTitle);
        Assertions.assertEquals(emptyTitleMusicTrack.getTrackArtistCount(), 1);
        Assertions.assertEquals(emptyTitleMusicTrack.getIsExplicit(), false);

        MusicRecording emptyMusicReleaseMusicRecording = new MusicRecording(expectedISRC, expectedTitle, expectedDurationMillis, null, List.of(musicGroup), false);
        MusicProtocol.MusicTrack emptyMusicReleaseMusicTrack = AppleMusicPlaylistConverter.convertMusicRecordingToTrackBuilder(emptyMusicReleaseMusicRecording).build();
        Assertions.assertNotNull(emptyMusicReleaseMusicTrack);
        Assertions.assertFalse(emptyMusicReleaseMusicTrack.hasPlaylistId()); // Base builder has no playlist id
        Assertions.assertFalse(emptyMusicReleaseMusicTrack.hasPlaylistPosition()); // Base builder has no playlist position
        Assertions.assertTrue(emptyMusicReleaseMusicTrack.hasIsrcCode());
        Assertions.assertEquals(emptyMusicReleaseMusicTrack.getIsrcCode(), expectedISRC);
        Assertions.assertTrue(emptyMusicReleaseMusicTrack.hasTitle());
        Assertions.assertEquals(emptyMusicReleaseMusicTrack.getTitle(), expectedTitle);
        Assertions.assertTrue(emptyMusicReleaseMusicTrack.hasDurationMillis());
        Assertions.assertEquals(emptyMusicReleaseMusicTrack.getDurationMillis(), expectedDurationMillis);
        Assertions.assertFalse(emptyMusicReleaseMusicTrack.hasMusicAlbum());
        Assertions.assertEquals(emptyMusicReleaseMusicTrack.getTrackArtistCount(), 1);
        Assertions.assertEquals(emptyMusicReleaseMusicTrack.getIsExplicit(), false);

        MusicRecording emptyMusicGroupsMusicRecording = new MusicRecording(expectedISRC, expectedTitle, expectedDurationMillis, musicRelease, List.of(), false);
        MusicProtocol.MusicTrack emptyMusicGroupsMusicTrack = AppleMusicPlaylistConverter.convertMusicRecordingToTrackBuilder(emptyMusicGroupsMusicRecording).build();
        Assertions.assertNotNull(emptyMusicGroupsMusicTrack);
        Assertions.assertFalse(emptyMusicGroupsMusicTrack.hasPlaylistId()); // Base builder has no playlist id
        Assertions.assertFalse(emptyMusicGroupsMusicTrack.hasPlaylistPosition()); // Base builder has no playlist position
        Assertions.assertTrue(emptyMusicGroupsMusicTrack.hasIsrcCode());
        Assertions.assertEquals(emptyMusicGroupsMusicTrack.getIsrcCode(), expectedISRC);
        Assertions.assertTrue(emptyMusicGroupsMusicTrack.hasTitle());
        Assertions.assertEquals(emptyMusicGroupsMusicTrack.getTitle(), expectedTitle);
        Assertions.assertTrue(emptyMusicGroupsMusicTrack.hasDurationMillis());
        Assertions.assertEquals(emptyMusicGroupsMusicTrack.getDurationMillis(), expectedDurationMillis);
        Assertions.assertTrue(emptyMusicGroupsMusicTrack.hasMusicAlbum());
        Assertions.assertEquals(emptyMusicGroupsMusicTrack.getMusicAlbum().getTitle(), expectedAlbumTitle);
        Assertions.assertEquals(emptyMusicGroupsMusicTrack.getTrackArtistCount(), 0);
        Assertions.assertEquals(emptyMusicGroupsMusicTrack.getIsExplicit(), false);

        MusicRecording musicRecording = new MusicRecording(expectedISRC, expectedTitle, expectedDurationMillis, musicRelease, List.of(musicGroup), expectedIsExplicit);
        MusicProtocol.MusicTrack musicTrack = AppleMusicPlaylistConverter.convertMusicRecordingToTrackBuilder(musicRecording).build();
        Assertions.assertNotNull(musicTrack);
        Assertions.assertFalse(musicTrack.hasPlaylistId()); // Base builder has no playlist id
        Assertions.assertFalse(musicTrack.hasPlaylistPosition()); // Base builder has no playlist position
        Assertions.assertTrue(musicTrack.hasIsrcCode());
        Assertions.assertEquals(musicTrack.getIsrcCode(), expectedISRC);
        Assertions.assertTrue(musicTrack.hasTitle());
        Assertions.assertEquals(musicTrack.getTitle(), expectedTitle);
        Assertions.assertTrue(musicTrack.hasDurationMillis());
        Assertions.assertEquals(musicTrack.getDurationMillis(), expectedDurationMillis);
        Assertions.assertTrue(musicTrack.hasMusicAlbum());
        Assertions.assertEquals(musicTrack.getMusicAlbum().getTitle(), expectedAlbumTitle);
        Assertions.assertEquals(musicTrack.getTrackArtistCount(), 1);
        Assertions.assertEquals(musicTrack.getIsExplicit(), expectedIsExplicit);
    }

    @Test
    public void testConvertMusicReleaseToMusicAlbum() {
        String expectedArtistName = "Expected Artist Name";
        MusicGroup musicGroup = new MusicGroup(expectedArtistName);
        String expectedICPN = "1234567890abcdefg";
        String expectedTitle = "Expected Album Title";

        MusicProtocol.MusicAlbum nullMusicAlbum = AppleMusicPlaylistConverter.convertMusicReleaseToMusicAlbum(null);
        Assertions.assertNotNull(nullMusicAlbum);
        Assertions.assertFalse(nullMusicAlbum.hasTitle());
        Assertions.assertFalse(nullMusicAlbum.hasIcpnCode());
        Assertions.assertEquals(nullMusicAlbum.getAlbumArtistCount(), 0);

        MusicRelease emptyTitleMusicRelease = new MusicRelease(expectedICPN, null, List.of(musicGroup));
        MusicProtocol.MusicAlbum emptyTitleMusicAlbum = AppleMusicPlaylistConverter.convertMusicReleaseToMusicAlbum(emptyTitleMusicRelease);
        Assertions.assertNotNull(emptyTitleMusicAlbum);
        Assertions.assertTrue(emptyTitleMusicAlbum.hasIcpnCode());
        Assertions.assertEquals(emptyTitleMusicAlbum.getIcpnCode(), expectedICPN);
        Assertions.assertFalse(emptyTitleMusicAlbum.hasTitle());
        Assertions.assertEquals(emptyTitleMusicAlbum.getAlbumArtistCount(), 1);

        MusicRelease emptyICPNMusicRelease = new MusicRelease(null, expectedTitle, List.of(musicGroup));
        MusicProtocol.MusicAlbum emptyICPNMusicAlbum = AppleMusicPlaylistConverter.convertMusicReleaseToMusicAlbum(emptyICPNMusicRelease);
        Assertions.assertNotNull(emptyICPNMusicAlbum);
        Assertions.assertFalse(emptyICPNMusicAlbum.hasIcpnCode());
        Assertions.assertTrue(emptyICPNMusicAlbum.hasTitle());
        Assertions.assertEquals(emptyICPNMusicAlbum.getTitle(), expectedTitle);
        Assertions.assertEquals(emptyICPNMusicAlbum.getAlbumArtistCount(), 1);

        MusicRelease emptyByArtistsMusicRelease = new MusicRelease(expectedICPN, expectedTitle, null);
        MusicProtocol.MusicAlbum emptyByArtistsMusicAlbum = AppleMusicPlaylistConverter.convertMusicReleaseToMusicAlbum(emptyByArtistsMusicRelease);
        Assertions.assertNotNull(emptyByArtistsMusicAlbum);
        Assertions.assertTrue(emptyByArtistsMusicAlbum.hasIcpnCode());
        Assertions.assertEquals(emptyByArtistsMusicAlbum.getIcpnCode(), expectedICPN);
        Assertions.assertTrue(emptyByArtistsMusicAlbum.hasTitle());
        Assertions.assertEquals(emptyByArtistsMusicAlbum.getTitle(), expectedTitle);
        Assertions.assertEquals(emptyByArtistsMusicAlbum.getAlbumArtistCount(), 0);

        MusicRelease musicRelease = new MusicRelease(expectedICPN, expectedTitle, List.of(musicGroup));
        MusicProtocol.MusicAlbum musicAlbum = AppleMusicPlaylistConverter.convertMusicReleaseToMusicAlbum(musicRelease);
        Assertions.assertNotNull(musicAlbum);
        Assertions.assertTrue(musicAlbum.hasIcpnCode());
        Assertions.assertEquals(musicAlbum.getIcpnCode(), expectedICPN);
        Assertions.assertTrue(musicAlbum.hasTitle());
        Assertions.assertEquals(musicAlbum.getTitle(), expectedTitle);
        Assertions.assertEquals(musicAlbum.getAlbumArtistCount(), 1);
    }


    @Test
    public void testConvertMusicGroupsToMusicArtists() {
        String expectedName = "Expected Artist Name";
        String expectedName2 = "Expected Second Artist Name";
        MusicGroup musicGroup = new MusicGroup(expectedName);
        MusicGroup secondMusicGroup = new MusicGroup(expectedName2);

        // Null Collection
        List<MusicProtocol.MusicArtist> nullResult = AppleMusicPlaylistConverter.convertMusicGroupsToMusicArtists(null);
        Assertions.assertTrue(nullResult.isEmpty());

        // Empty Collection
        List<MusicProtocol.MusicArtist> emptyResult = AppleMusicPlaylistConverter.convertMusicGroupsToMusicArtists(List.of());
        Assertions.assertTrue(emptyResult.isEmpty());

        // Single Group
        List<MusicProtocol.MusicArtist> result1 = AppleMusicPlaylistConverter.convertMusicGroupsToMusicArtists(List.of(musicGroup));
        Assertions.assertFalse(result1.isEmpty());
        Assertions.assertEquals(result1.size(), 1);
        Assertions.assertNotNull(result1.get(0));
        Assertions.assertTrue(result1.get(0).hasName());
        Assertions.assertEquals(result1.get(0).getName(), expectedName);

        // Multiple Groups
        List<MusicProtocol.MusicArtist> result2 = AppleMusicPlaylistConverter.convertMusicGroupsToMusicArtists(List.of(musicGroup, secondMusicGroup));
        Assertions.assertFalse(result2.isEmpty());
        Assertions.assertEquals(result2.size(), 2);
        Assertions.assertNotNull(result2.get(0));
        Assertions.assertTrue(result2.get(0).hasName());
        Assertions.assertEquals(result2.get(0).getName(), expectedName);
        Assertions.assertNotNull(result2.get(1));
        Assertions.assertTrue(result2.get(1).hasName());
        Assertions.assertEquals(result2.get(1).getName(), expectedName2);
    }
}
