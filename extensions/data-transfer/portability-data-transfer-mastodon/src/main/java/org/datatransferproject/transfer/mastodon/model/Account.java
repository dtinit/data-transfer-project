/*
 * Copyright 2019 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.mastodon.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Partial POJO representation of
 * https://github.com/tootsuite/documentation/blob/master/Using-the-API/API.md#account
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Account {
  private String id;
  private String username;
  private String acct;
  private String displayName;
  private int statusesCount;
  private String url;
  private boolean moved;

  @JsonCreator
  public Account(
      @JsonProperty("id") String id,
      @JsonProperty("username") String username,
      @JsonProperty("acct") String acct,
      @JsonProperty("display_name") String displayName,
      @JsonProperty("statuses_count") int statusesCount,
      @JsonProperty("url") String url,
      @JsonProperty("moved") boolean moved) {
    this.id = id;
    this.username = username;
    this.acct = acct;
    this.displayName = displayName;
    this.statusesCount = statusesCount;
    this.url = url;
    this.moved = moved;
  }

  public String getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getAcct() {
    return acct;
  }

  public String getDisplayName() {
    return displayName;
  }

  public int getStatusesCount() {
    return statusesCount;
  }

  public String getUrl() {
    return url;
  }

  public boolean isMoved() {
    return moved;
  }
}
