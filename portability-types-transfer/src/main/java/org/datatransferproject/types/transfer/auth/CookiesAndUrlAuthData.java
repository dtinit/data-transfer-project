package org.datatransferproject.types.transfer.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;

/** Auth data based on a set of cookies and a URL, currently only used for Solid and Mastodon. */
@JsonTypeName("org.dataportability:CookiesAndUrlAuthData")
public class CookiesAndUrlAuthData extends AuthData {
  private final List<String> cookies;
  private final String url;

  @JsonCreator
  public CookiesAndUrlAuthData(
      @JsonProperty("cookies") List<String> cookies, @JsonProperty("url") String url) {
    this.cookies = cookies;
    this.url = url;
  }

  public String getUrl() {
    return url;
  }

  public List<String> getCookies() {
    return cookies;
  }

  @JsonIgnore
  @Override
  public String getToken() {
    // CookiesAndUrlAuthData is the only class not to have a token.
    throw new UnsupportedOperationException("CookiesAndUrlAuthData does not have a token.");
  }
}
