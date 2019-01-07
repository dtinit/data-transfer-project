package org.datatransferproject.transfer.mastodon.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nullable;

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


  public Status(
      @JsonProperty("id") String id,
      @JsonProperty("uri") String uri,
      @JsonProperty("url") @Nullable String url,
      @JsonProperty("account") Account account,
      @JsonProperty("in_reply_to_id") @Nullable String inReplyToId,
      @JsonProperty("in_reply_to_account_id") @Nullable String inReplyToAccountId,
      @JsonProperty("content") String content) {
    this.id = id;
    this.uri = uri;
    this.url = url;
    this.account = account;
    this.inReplyToId = inReplyToId;
    this.inReplyToAccountId = inReplyToAccountId;
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

  public String getContent() {
    return content;
  }
}
