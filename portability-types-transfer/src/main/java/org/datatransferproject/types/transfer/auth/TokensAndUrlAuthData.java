package org.datatransferproject.types.transfer.auth;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.net.URI;
import java.net.URISyntaxException;

/** Token-based authentication data. */
@JsonTypeName("org.dataportability:TokensAndUrlAuthData")
public class TokensAndUrlAuthData extends AuthData {
  private final String accessToken;
  private final String refreshToken;
  private final String tokenServerEncodedUrl;

  @JsonCreator
  public TokensAndUrlAuthData(
      @JsonProperty("accessToken") String accessToken,
      @JsonProperty("refreshToken") String refreshToken,
      @JsonProperty("tokenServerEncodedUrl") String tokenServerEncodedUrl) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.tokenServerEncodedUrl = tokenServerEncodedUrl;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public String getTokenServerEncodedUrl() {
    return tokenServerEncodedUrl;
  }

  @JsonIgnore
  public URI getTokenServerEncodedUri() throws IllegalStateException {
    final String urlString = getTokenServerEncodedUrl();
    // TODO(jzacsh, hgandhi90) this should probably happen at construction time; maybe there's a jackson annotation for this?
    checkState(
        !isNullOrEmpty(urlString),
        "malformed construction TokensAndUrlAuthData getTokenServerEncodedUrl() should be non-empty, but got \"%s\"",
        urlString);
    try {
      return new URI(urlString);
    } catch (URISyntaxException e) {
      throw new IllegalStateException(
          String.format("TokensAndUrlAuthData built with a malformed token-refresh URI (\"%s\")", urlString),
          e);
    }
  }

  @JsonIgnore
  @Override
  public String getToken() {
    return getAccessToken();
  }

  @JsonIgnore
  public TokensAndUrlAuthData rebuildWithRefresh(String fresherAccessToken) {
    return new TokensAndUrlAuthData(fresherAccessToken, getRefreshToken(), getTokenServerEncodedUrl());
  }
}
