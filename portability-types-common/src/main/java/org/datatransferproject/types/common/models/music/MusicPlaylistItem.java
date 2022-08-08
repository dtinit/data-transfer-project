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

import static com.google.common.base.Strings.isNullOrEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.util.Objects;

/** POJO for MusicPlaylistItem */
public class MusicPlaylistItem {
    private final MusicRecording track;
    private final String playlistId;
    private final String originalPlatform;
    private final int order;

    @JsonCreator
    public MusicPlaylistItem(
            @JsonProperty("track") MusicRecording track,
            @JsonProperty("playlistId") String playlistId,
            @JsonProperty("originalPlatform") String originalPlatform,
            @JsonProperty("order") int order) {
        Preconditions.checkNotNull(track, "track must be set for MusicPlaylistItem");
        this.track = track;
        if (isNullOrEmpty(playlistId)) {
            throw new IllegalArgumentException("playlistId must be set for MusicPlaylistItem");
        }
        this.playlistId = playlistId;
        if (isNullOrEmpty(originalPlatform)) {
            throw new IllegalArgumentException("originalPlatform must be set for MusicPlaylistItem");
        }
        this.originalPlatform = originalPlatform;
        this.order = order;
    }

    public MusicRecording getTrack() {
        return track;
    }

    public String getPlaylistId() {
        return playlistId;
    }

    public String getOriginalPlatform() {
        return originalPlatform;
    }

    public int getOrder() {
        return order;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("track", getTrack())
                .add("playlistId", getPlaylistId())
                .add("originalPlatform", getOriginalPlatform())
                .add("order", getOrder())
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MusicPlaylistItem)) {
            return false;
        }
        MusicPlaylistItem that = (MusicPlaylistItem) o;
        return Objects.equals(track, that.track)
                && Objects.equals(playlistId, that.playlistId)
                && Objects.equals(originalPlatform, that.originalPlatform)
                && order == that.order;
    }

    @Override
    public int hashCode() {
        return Objects.hash(track, playlistId, originalPlatform, order);
    }
}
