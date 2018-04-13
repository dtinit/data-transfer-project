/*
 * Copyright 2018 The Data Transfer Project Authors.
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

package org.dataportabilityproject.auth.smugmug.SmugMugAuth;

import com.google.common.base.Strings;
import java.io.IOException;
import org.dataportabilityproject.types.transfer.auth.TokenSecretAuthData;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

public class AuthInterface {

  private final String apiKey;
  private final String apiSecret;

  public AuthInterface(String apiKey, String apiSecret) {
    this.apiKey = apiKey;
    this.apiSecret = apiSecret;
  }

  public TokenSecretAuthData getRequestToken(String callbackUrl) throws IOException {
    String callback = (Strings.isNullOrEmpty(callbackUrl)) ? "oob" : callbackUrl;
    OAuthService service =
        new ServiceBuilder()
            .provider(SmugmugApi.class)
            .apiKey(apiKey)
            .apiSecret(apiSecret)
            .callback(callback)
            .build();
    Token token = service.getRequestToken();
    return new TokenSecretAuthData(token.getToken(), token.getSecret());
  }

  public String getAuthorizationUrl(TokenSecretAuthData token, String permission) {
    OAuthService service =
        new ServiceBuilder()
            .provider(SmugmugApi.class)
            .apiKey(apiKey)
            .apiSecret(apiSecret)
            .build();
    String authUrl = service.getAuthorizationUrl(new Token(token.getToken(), token.getSecret()));
    return String.format("%s&perms=%s", authUrl, permission);
  }

  public TokenSecretAuthData getAccessToken(TokenSecretAuthData requestToken, Verifier verifier) {
    OAuthService service =
        new ServiceBuilder()
            .provider(SmugmugApi.class)
            .apiKey(apiKey)
            .apiSecret(apiSecret)
            .build();
    Token accessToken =
        service.getAccessToken(
            new Token(requestToken.getToken(), requestToken.getSecret()), verifier);
    return new TokenSecretAuthData(accessToken.getToken(), accessToken.getSecret());
  }
}
