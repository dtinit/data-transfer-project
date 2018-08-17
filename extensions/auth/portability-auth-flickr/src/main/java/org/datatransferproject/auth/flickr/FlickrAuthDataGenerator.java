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

package org.datatransferproject.auth.flickr;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.auth.AuthInterface;
import com.flickr4java.flickr.auth.Permission;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.datatransferproject.spi.api.auth.AuthDataGenerator;
import org.datatransferproject.spi.api.types.AuthFlowConfiguration;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.datatransferproject.spi.api.types.AuthFlowConfiguration.AuthProtocol;
import static org.datatransferproject.spi.api.types.AuthFlowConfiguration.AuthProtocol.OAUTH_1;

public class FlickrAuthDataGenerator implements AuthDataGenerator {
  private static final Logger logger = LoggerFactory.getLogger(FlickrAuthDataGenerator.class);
  private static final AuthProtocol AUTH_PROTOCOL = OAUTH_1;

  private final Flickr flickr;

  FlickrAuthDataGenerator(AppCredentials appCredentials){
    flickr = new Flickr(appCredentials.getKey(), appCredentials.getSecret(), new REST());
  }

  @Override
  public AuthFlowConfiguration generateConfiguration(String callbackBaseUrl, String id) {
    AuthInterface authInterface = flickr.getAuthInterface();
    Token token = authInterface.getRequestToken(callbackBaseUrl + "/callback/flickr");
    String url =
            authInterface.getAuthorizationUrl(
                    token, Permission.WRITE);
    return new AuthFlowConfiguration(url, AUTH_PROTOCOL, toAuthData(token));
  }

  @Override
  public AuthData generateAuthData(
      String callbackBaseUrl, String authCode, String id, AuthData initialAuthData, String extra) {
    Preconditions.checkArgument(Strings.isNullOrEmpty(extra), "Extra data not expected");
    Preconditions.checkNotNull(initialAuthData, "Earlier auth data not expected for Flickr flow");
    AuthInterface authInterface = flickr.getAuthInterface();
    Token token = fromAuthData(initialAuthData);
    Token requestToken = authInterface.getAccessToken(token, new Verifier(authCode));

    try {
      authInterface.checkToken(requestToken);
    } catch (FlickrException e) {
      logger.warn("Problem verifying auth token {}", e);
      return null;
    }
    return new TokenSecretAuthData(requestToken.getToken(), requestToken.getSecret());
  }

  private static Token fromAuthData(AuthData authData) {
    TokenSecretAuthData data = (TokenSecretAuthData) authData;
    return new Token(data.getToken(), data.getSecret());
  }

  private static TokenSecretAuthData toAuthData(Token token){
    return new TokenSecretAuthData(token.getToken(), token.getSecret());
  }
}
