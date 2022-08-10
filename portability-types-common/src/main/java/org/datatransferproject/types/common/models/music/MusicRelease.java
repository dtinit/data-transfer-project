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
import java.util.Objects;
import org.datatransferproject.types.common.models.CreativeWork;

/** POJO for https://schema.org/MusicRelease */
public class MusicRelease extends CreativeWork {
    private final String icpnCode;

    @JsonCreator
    public MusicRelease(
            @JsonProperty("identifier") String identifier,
            @JsonProperty("headline") String headline,
            @JsonProperty("icpnCode") String icpnCode) {
        super(identifier);
        setHeadline(headline);
        if (isNullOrEmpty(icpnCode)) {
            throw new IllegalArgumentException("icpnCode must be set for MusicRelease");
        }
        this.icpnCode = icpnCode;
    }

    public String getIcpnCode() {
        return icpnCode;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("identifier", getIdentifier())
                .add("headline", getHeadline())
                .add("icpnCode", getIcpnCode())
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MusicRelease)) {
            return false;
        }
        MusicRelease that = (MusicRelease) o;
        return Objects.equals(icpnCode, that.icpnCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(icpnCode);
    }
}
