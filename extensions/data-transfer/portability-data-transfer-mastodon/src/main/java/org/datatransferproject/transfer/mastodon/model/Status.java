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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nullable;
import java.time.Instant;

/**
 * Partial POJO representation of
 * https://github.com/tootsuite/documentation/blob/master/Using-the-API/API.md#status
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Status {
  private String id;
  private String uri;
  @Nullable private String url;
  private Account account;
  @Nullable private String inReplyToId;
  @Nullable private String inReplyToAccountId;
  private String content;
  private String createdAtTime;


  public Status(
      @JsonProperty("id") String id,
      @JsonProperty("uri") String uri,
      @JsonProperty("url") @Nullable String url,
      @JsonProperty("account") Account account,
      @JsonProperty("in_reply_to_id") @Nullable String inReplyToId,
      @JsonProperty("in_reply_to_account_id") @Nullable String inReplyToAccountId,
      @JsonProperty("created_at") String createdAtTime,
      @JsonProperty("content") String content) {
    this.id = id;
    this.uri = uri;
    this.url = url;
    this.account = account;
    this.inReplyToId = inReplyToId;
    this.inReplyToAccountId = inReplyToAccountId;
    this.createdAtTime = createdAtTime;
    this.content = content;
  }

  public String getId() {
    return id;
  }

  public String getUri() {
    return uri;
  }

  @Nullable
  public String getUrl() {
    return url;
  }

  public Account getAccount() {
    return account;
  }

  @Nullable
  public String getInReplyToId() {
    return inReplyToId;
  }

  @Nullable
  public String getInReplyToAccountId() {
    return inReplyToAccountId;
  }

  public String getCreatedAtTime() {
    return createdAtTime;
  }

  public Instant getCreatedAt() {
    return Instant.parse(createdAtTime);
  }

  public String getContent() {
    return content;
  }
}