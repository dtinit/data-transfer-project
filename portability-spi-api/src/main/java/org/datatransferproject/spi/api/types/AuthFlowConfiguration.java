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
  private final AuthProtocol authProtocol;
  private AuthData initialAuthData;

  /**
   * Ctor used when the flow does not require initial authentication data.
   *
   * @param url the initial URL.
   */
  public AuthFlowConfiguration(String url, AuthProtocol authProtocol) {
    this.url = url;
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
          @JsonProperty("authProtocol") AuthProtocol authProtocol,
          @JsonProperty("initialAuthData") AuthData initialAuthData) {
    this.url = url;
    this.authProtocol = authProtocol;
    this.initialAuthData = initialAuthData;
  }

  /** Returns the initial flow URL. */
  public String getUrl() {
    return url;
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
