package org.datatransferproject.types.transfer.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;

/** Auth data based on a set of cookies and a URL, currently only used for Solid. */
@JsonTypeName("org.dataportability:CookiesAndUrlAuthData")
public class CookiesAndUrlAuthData extends AuthData {
  private final List<String> cookies;
  private final String url;

  @JsonCreator
  public CookiesAndUrlAuthData(
      @JsonProperty("cookies") List<String> cookies,
      @JsonProperty("url") String url) {
    this.cookies = cookies;
    this.url = url;
  }

  public String getUrl() {
    return url;
  }

  public List<String> getCookies() {
    return cookies;
  }

  @Override
  public String getToken() {
    // CookiesAndUrlAuthData is the only class not to have a token.
    return "";
  }
}
