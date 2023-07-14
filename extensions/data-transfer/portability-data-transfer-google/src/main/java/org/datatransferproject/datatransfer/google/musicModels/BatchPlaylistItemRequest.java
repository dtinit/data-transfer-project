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

package org.datatransferproject.datatransfer.google.musicModels;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

/**
 * Class containing all the information necessary to create new {@link CreatePlaylistItemRequest}s
 * in the Google Music API.
 */
@JsonInclude(Include.NON_NULL)
public class BatchPlaylistItemRequest {

  @JsonProperty("requests")
  private List<CreatePlaylistItemRequest> requests;

  @JsonProperty("parent")
  private String parent;

  public BatchPlaylistItemRequest(List<CreatePlaylistItemRequest> requests, String parent) {
    this.requests = requests;
    this.parent = parent;
  }

  public List<CreatePlaylistItemRequest> getRequests() {
    return requests;
  }

  public String getParent() {
    return parent;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BatchPlaylistItemRequest)) {
      return false;
    }
    BatchPlaylistItemRequest that = (BatchPlaylistItemRequest) o;
    return Objects.equals(requests, that.requests) && Objects.equals(parent, that.parent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getRequests(), getParent());
  }
}
