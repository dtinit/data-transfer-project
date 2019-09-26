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

package org.datatransferproject.transfer.twitter;

import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

final class TwitterApiWrapper {
  static Twitter getInstance(AppCredentials appCredentials, TokenSecretAuthData authData) {

    ConfigurationBuilder cb = new ConfigurationBuilder();
    cb.setDebugEnabled(false)
        .setOAuthConsumerKey(appCredentials.getKey())
        .setOAuthConsumerSecret(appCredentials.getSecret())
        // TODO: I think the token/secret expire, we need to check into refreshing them
        .setOAuthAccessToken(authData.getToken())
        .setOAuthAccessTokenSecret(authData.getSecret());
    TwitterFactory tf = new TwitterFactory(cb.build());
    return tf.getInstance();
  }
}
