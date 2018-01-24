/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.serviceProviders.fiveHundredPx;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import jdk.nashorn.internal.parser.Token;
import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import org.dataportabilityproject.shared.AppCredentials;
import org.dataportabilityproject.shared.IOInterface;
import org.dataportabilityproject.shared.ServiceMode;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.AuthFlowInitiator;
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.TokenSecretAuthData;
import org.dataportabilityproject.shared.signpost.GoogleOAuthConsumer;

public class FHPxAuth implements OfflineAuthDataGenerator {

  private final AppCredentials appCredentials;
  private final ServiceMode serviceMode;  // Either import or export

  FHPxAuth(AppCredentials appCredentials, ServiceMode serviceMode) {
    this.appCredentials = appCredentials;
    this.serviceMode = serviceMode;
  }

  @Override
  public AuthData generateAuthData(IOInterface ioInterface) throws IOException {
    // Shamelessly copied from SmugMugAuth
    OAuthConsumer consumer = new GoogleOAuthConsumer(appCredentials.key(), appCredentials.secret());

    // TODO(olsona): figure out if we need permissions, as in SmugMugAuth
    String requestTokenUrl = "https://api.500px.com/v1/oauth/request_token";
    String accessTokenUrl = ""; // TODO(olsona)
    String authWebsiteUrl = ""; // TODO(olsona)
    OAuthProvider provider = new DefaultOAuthProvider(requestTokenUrl, accessTokenUrl, authWebsiteUrl);

    String authUrl;
    try {
      authUrl = provider.retrieveRequestToken(consumer, OAuth.OUT_OF_BAND);
    } catch (OAuthMessageSignerException
        | OAuthNotAuthorizedException
        | OAuthExpectationFailedException
        | OAuthCommunicationException e) {
      throw new IOException("Couldn't generate authUrl", e);
    }

    String code = ioInterface.ask("Please visit: " + authUrl + " and enter code:");
    try {
      provider.retrieveAccessToken(consumer, code.trim());
    } catch (OAuthMessageSignerException
        | OAuthNotAuthorizedException
        | OAuthExpectationFailedException
        | OAuthCommunicationException e) {
      throw new IOException("Couldn't authorize", e);
    }

    return TokenSecretAuthData.create(consumer.getToken(), consumer.getTokenSecret());
  }

  // This should be in a utility class
  OAuthConsumer generateConsumer(AuthData authData) {
    checkArgument(authData instanceof TokenSecretAuthData,
        "authData expected to be TokenSecretAuthData not %s",
        authData.getClass().getCanonicalName());
    TokenSecretAuthData tokenSecretAuthData = (TokenSecretAuthData) authData;
    OAuthConsumer consumer = new GoogleOAuthConsumer(appCredentials.key(), appCredentials.secret());
    consumer.setTokenWithSecret(tokenSecretAuthData.token(), tokenSecretAuthData.secret());
    return consumer;
  }
}
