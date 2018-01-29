/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.types.transfer.models.photos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PhotoModel {
    private final String title;
    private final String fetchableUrl;
    private final String description;
    private final String mediaType;
    private final String albumId;

    @JsonCreator
    public PhotoModel(
        @JsonProperty("title") String title,
        @JsonProperty("fetchableUrl") String fetchableUrl,
        @JsonProperty("description") String description,
        @JsonProperty("mediaType") String mediaType,
        @JsonProperty("albumId") String albumId) {
        this.title = title;
        this.fetchableUrl = fetchableUrl;
        this.description = description;
        this.mediaType = mediaType;
        this.albumId = albumId;
    }

    public String getTitle() {
        return title;
    }

    public String getFetchableUrl() {
        return fetchableUrl;
    }

    public String getDescription() {
        return description;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getAlbumId() {
        return albumId;
    }
}
