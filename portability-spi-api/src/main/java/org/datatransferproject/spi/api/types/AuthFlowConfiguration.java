package org.datatransferproject.spi.api.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.datatransferproject.types.common.PortableType;
import org.datatransferproject.types.transfer.auth.AuthData;

import static org.datatransferproject.types.common.PortabilityCommon.AuthProtocol;

/**
 * Configuration for an authorization flow. A flow has an initial URL and optional initial
 * authentication data.
 */
@JsonTypeName("org.dataportability:AuthFlowConfiguration")
public class AuthFlowConfiguration extends PortableType {
  private final String authUrl;
  private final String tokenUrl;
  private final AuthProtocol authProtocol;
  private AuthData initialAuthData;

  /**
   * Ctor used when the flow does not require initial authentication data.
   *
   * @param authUrl the initial URL.
   */
  public AuthFlowConfiguration(String authUrl, AuthProtocol authProtocol, String tokenUrl) {
    this.authUrl = authUrl;
    this.tokenUrl = tokenUrl;
    this.authProtocol = authProtocol;
  }

  /**
   * Ctor.
   * @param authUrl the initial URL.
   * @param tokenUrl the URL to exchange an access code for token.
   * @param authProtocol the protocol used for authentication.
   * @param initialAuthData the initial authentication data
   */
  @JsonCreator
  public AuthFlowConfiguration(
          @JsonProperty("authUrl") String authUrl,
          @JsonProperty("tokenUrl") String tokenUrl,
          @JsonProperty("authProtocol") AuthProtocol authProtocol,
          @JsonProperty("initialAuthData") AuthData initialAuthData) {
    this.authUrl = authUrl;
    this.tokenUrl = tokenUrl;
    this.authProtocol = authProtocol;
    this.initialAuthData = initialAuthData;
  }

  /** Returns the initial flow URL. */
  public String getAuthUrl() {
    return authUrl;
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
