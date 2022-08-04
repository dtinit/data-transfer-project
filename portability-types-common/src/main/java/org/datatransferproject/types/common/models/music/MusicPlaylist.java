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
import java.time.Instant;
import java.util.Objects;

/** POJO for https://schema.org/MusicPlaylist */
final class MusicPlaylist extends CreativeWork {
    private final String id;
    private final String description;
    private final String originalPlatform;
    private final Instant createTime;
    private final Instant updateTime;

    @JsonCreator
    public MusicPlaylist(
            @JsonProperty("headline") String headline,
            @JsonProperty("id") String id,
            @JsonProperty("description") String description,
            @JsonProperty("originalPlatform") String originalPlatform,
            @JsonProperty("createTime") Instant createTime,
            @JsonProperty("updateTime") Instant updateTime) {
        super(headline);
        if (isNullOrEmpty(this.headline)) {
            throw new IllegalArgumentException("headline must be set for MusicPlaylist");
        }
        if (isNullOrEmpty(id)) {
            throw new IllegalArgumentException("id must be set for MusicPlaylist");
        }
        this.id = id;
        if (isNullOrEmpty(description)) {
            throw new IllegalArgumentException("description must be set for MusicPlaylist");
        }
        this.description = description;
        if (isNullOrEmpty(description)) {
            throw new IllegalArgumentException("originalPlatform must be set for MusicPlaylist");
        }
        this.originalPlatform = originalPlatform;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getOriginalPlatform() {
        return originalPlatform;
    }

    public Instant getCreateTime() {
        return createTime;
    }

    public Instant getUpdateTime() {
        return updateTime;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("headline", getHeadline())
                .add("id", getId())
                .add("description", getDescription())
                .add("originalPlatform", getOriginalPlatform())
                .add("createTime", getCreateTime())
                .add("updateTime", getUpdateTime())
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MusicPlaylist that = (MusicPlaylist) o;
        return Objects.equals(headline, that.headline)
                && Objects.equals(id, that.id)
                && Objects.equals(description, that.description)
                && Objects.equals(originalPlatform, that.originalPlatform);
    }

    @Override
    public int hashCode() {
        return Objects.hash(headline, id, description, originalPlatform);
    }
}
