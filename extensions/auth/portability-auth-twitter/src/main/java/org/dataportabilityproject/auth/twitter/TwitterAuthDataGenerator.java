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

package org.dataportabilityproject.auth.twitter;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.dataportabilityproject.spi.api.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.dataportabilityproject.spi.api.types.AuthFlowConfiguration;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.auth.TokenSecretAuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

/* TwitterAuthDataGenerator used for obtaining auth credentials for the Twitter API*/
final class TwitterAuthDataGenerator implements AuthDataGenerator {
  private final Logger logger = LoggerFactory.getLogger(TwitterAuthDataGenerator.class);
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
  public AuthFlowConfiguration generateConfiguration(String callbackBaseUrl, String id) {
    // Generate a request token and include that as initial auth data
    RequestToken requestToken;
    try {
       requestToken =
          twitterApi.getOAuthRequestToken(callbackBaseUrl + "/callback1/twitter", perms);
    } catch (TwitterException e) {
      logger.warn("Couldn't get authData", e);
      return null;
    }

    return new AuthFlowConfiguration(
        requestToken.getAuthorizationURL(),
        new TokenSecretAuthData(requestToken.getToken(), requestToken.getTokenSecret()));
  }

  @Override
  public AuthData generateAuthData(
      String callbackBaseUrl, String authCode, String id, AuthData initialAuthData, String extra) {
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
