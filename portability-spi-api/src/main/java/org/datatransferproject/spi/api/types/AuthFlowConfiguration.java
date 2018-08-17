package org.datatransferproject.spi.api.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.datatransferproject.types.transfer.PortableType;
import org.datatransferproject.types.transfer.auth.AuthData;

import static org.datatransferproject.types.common.PortabilityCommon.AuthProtocol;

/**
 * Configuration for an authorization flow. A flow has an initial URL and optional initial
 * authentication data.
 */
@JsonTypeName("org.dataportability:AuthFlowConfiguration")
public class AuthFlowConfiguration extends PortableType {
  private final String url;
  private final String tokenUrl;
  private final AuthProtocol authProtocol;
  private AuthData initialAuthData;

  /**
   * Ctor used when the flow does not require initial authentication data.
   *
   * @param url the initial URL.
   */
  public AuthFlowConfiguration(String url, String tokenUrl, AuthProtocol authProtocol) {
    this.url = url;
    this.tokenUrl = tokenUrl;
    this.authProtocol = authProtocol;
  }

  /**
   * Ctor.
   *  @param url the initial URL.
   * @param authProtocol the protocol used for authentication.
   * @param initialAuthData the initial authentication data
   */
  @JsonCreator
  public AuthFlowConfiguration(
          @JsonProperty("url") String url,
          @JsonProperty("tokenUrl") String tokenUrl,
          @JsonProperty("authProtocol") AuthProtocol authProtocol,
          @JsonProperty("initialAuthData") AuthData initialAuthData) {
    this.url = url;
    this.tokenUrl = tokenUrl;
    this.authProtocol = authProtocol;
    this.initialAuthData = initialAuthData;
  }

  /** Returns the initial flow URL. */
  public String getUrl() {
    return url;
  }

  /** Returns the access token URL. */
  public String getTokenUrl() {
    return tokenUrl;
  }

  /** Returns the initial authentication data or null if the flow does not use it. */
  public AuthData getInitialAuthData() {
    return initialAuthData;
  }

  /** Returns the authentication protocol. */
  public AuthProtocol getAuthProtocol() {
    return authProtocol;
  }
}
