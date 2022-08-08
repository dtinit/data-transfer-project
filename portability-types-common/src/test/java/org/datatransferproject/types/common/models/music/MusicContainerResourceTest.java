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
                                "p1_title",
                                "p1_id",
                                "p1_description",
                               "Google",
                                null,
                                null));

        ImmutableList<MusicPlaylistItem> playlistItems =
                ImmutableList.of(
                        new MusicPlaylistItem(
                                new MusicRecording(null, "item1_isrc", new MusicRelease(null, "r1_icpn"), null),
                                "p1_id",
                                "Google",
                                1),
                        new MusicPlaylistItem(
                                new MusicRecording(null, "item2_isrc", new MusicRelease(null, "r1_icpn"), null),
                                "p1_id",
                                "Google",
                                2));

        ImmutableList<MusicRecording> tracks =
                ImmutableList.of(
                        new MusicRecording(null, "t1_isrc", new MusicRelease(null, "r2_icpn"), null),
                        new MusicRecording(null, "t2_isrc", new MusicRelease(null, "r3_icpn"), null));

        ImmutableList<MusicRelease> releases = ImmutableList.of(new MusicRelease(null, "r4_icpn"));

        ContainerResource data = new MusicContainerResource(playlists, playlistItems, tracks, releases);

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
