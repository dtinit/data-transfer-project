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

package org.datatransferproject.auth.twitter;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.datatransferproject.spi.api.auth.AuthDataGenerator;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.datatransferproject.spi.api.types.AuthFlowConfiguration;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

import static org.datatransferproject.types.common.PortabilityCommon.AuthProtocol;
import static org.datatransferproject.types.common.PortabilityCommon.AuthProtocol.OAUTH_2;

/*
 * {@link AuthDataGenerator} to obtain auth credentials for the Twitter API.
 *
 * Note: this is in the process of being deprecated in favor of OAuth2DataGenerator.
 * <p>TODO(#553): Remove code/token exchange as this will be handled by frontends.
 */
final class TwitterAuthDataGenerator implements AuthDataGenerator {
  private static final Logger logger = LoggerFactory.getLogger(TwitterAuthDataGenerator.class);
  private static final AuthProtocol AUTH_PROTOCOL = OAUTH_2;
  private final String perms;
  private final Twitter twitterApi;

  public TwitterAuthDataGenerator(AppCredentials appCredentials, AuthMode authMode) {
    this.perms = authMode == AuthMode.IMPORT ? "write" : "read";
    ConfigurationBuilder cb = new ConfigurationBuilder();
    cb.setDebugEnabled(true)
        .setOAuthConsumerKey(appCredentials.getKey())
        .setOAuthConsumerSecret(appCredentials.getSecret());
    TwitterFactory tf = new TwitterFactory(cb.build());
    twitterApi = tf.getInstance();
  }

  @Override
  public AuthFlowConfiguration generateConfiguration(String callbackUrl, String id) {
    // Generate a request token and include that as initial auth data
    RequestToken requestToken;
    try {
       requestToken =
          twitterApi.getOAuthRequestToken(callbackUrl, perms);
    } catch (TwitterException e) {
      logger.warn("Couldn't get authData", e);
      return null;
    }

    return new AuthFlowConfiguration(
        requestToken.getAuthorizationURL(),
        getTokenUrl(),
        AUTH_PROTOCOL,
        new TokenSecretAuthData(requestToken.getToken(), requestToken.getTokenSecret()));
  }

  @Override
  public AuthData generateAuthData(
      String callbackUrl, String authCode, String id, AuthData initialAuthData, String extra) {
    Preconditions.checkArgument(Strings.isNullOrEmpty(extra), "Extra data not expected");
    Preconditions.checkNotNull(
        initialAuthData,
        "Earlier auth data expected for Twitter flow"); // Turn initial auth data into a Token
    TokenSecretAuthData requestData = (TokenSecretAuthData) initialAuthData;
    try {
      AccessToken accessToken =
          twitterApi.getOAuthAccessToken(
              new RequestToken(requestData.getToken(), requestData.getSecret()),
              authCode);
      return new TokenSecretAuthData(accessToken.getToken(), accessToken.getTokenSecret());
    } catch (TwitterException e) {
      logger.warn("Couldn't generate Twitter AccessToken", e);
      return null;
    }
  }
}
