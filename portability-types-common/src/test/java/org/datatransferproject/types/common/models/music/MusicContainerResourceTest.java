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

package org.datatransferproject.types.common.models.music;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import org.datatransferproject.types.common.models.ContainerResource;
import org.junit.jupiter.api.Test;

public final class MusicContainerResourceTest {

  @Test
  public void verifySerializeDeserialize() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerSubtypes(MusicContainerResource.class);

    ImmutableList<MusicPlaylist> playlists =
        ImmutableList.of(
            new MusicPlaylist(
                "p1_id",
                "p1_title",
                "p1_description",
                null,
                null));

    ImmutableList<MusicPlaylistItem> playlistItems =
        ImmutableList.of(
            new MusicPlaylistItem(
                new MusicRecording("item1_isrc", null, 180000L,
                    new MusicRelease("r1_icpn", null, null), null, false),
                "p1_id",
                1),
            new MusicPlaylistItem(
                new MusicRecording("item2_isrc", null, 180000L,
                    new MusicRelease("r1_icpn", null, null), null, false),
                "p1_id",
                2));

    ImmutableList<MusicRecording> tracks =
        ImmutableList.of(
            new MusicRecording("t1_isrc", null, 180000L,
                new MusicRelease("r2_icpn", null, null),
                null, false),
            new MusicRecording("t2_isrc", null, 180000L,
                new MusicRelease("r3_icpn", null, null),
                null, false));

    ImmutableList<MusicRelease> releases = ImmutableList.of(
        new MusicRelease("r4_icpn", null, null));

    ContainerResource data = new MusicContainerResource(playlists,
        playlistItems, tracks, releases);

    String serialized = objectMapper.writeValueAsString(data);

    ContainerResource deserializedModel =
        objectMapper.readValue(serialized, ContainerResource.class);

    Truth.assertThat(deserializedModel).isNotNull();
    Truth.assertThat(deserializedModel).isInstanceOf(MusicContainerResource.class);
    MusicContainerResource deserialized = (MusicContainerResource) deserializedModel;
    Truth.assertThat(deserialized.getPlaylists()).hasSize(1);
    Truth.assertThat(deserialized.getPlaylistItems()).hasSize(2);
    Truth.assertThat(deserialized.getTracks()).hasSize(2);
    Truth.assertThat(deserialized.getReleases()).hasSize(1);
    Truth.assertThat(deserialized).isEqualTo(data);
  }
}
