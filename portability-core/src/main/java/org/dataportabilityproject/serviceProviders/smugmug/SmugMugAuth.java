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
package org.dataportabilityproject.serviceProviders.smugmug;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Preconditions;
import java.io.IOException;
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
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.TokenSecretAuthData;
import org.dataportabilityproject.shared.signpost.GoogleOAuthConsumer;

final class SmugMugAuth implements OfflineAuthDataGenerator {

  private final AppCredentials appCredentials;
  private final ServiceMode serviceMode;

  SmugMugAuth(AppCredentials appCredentials, ServiceMode serviceMode) {
    this.appCredentials = Preconditions.checkNotNull(appCredentials);
    this.serviceMode = serviceMode;
  }

  @Override
  public AuthData generateAuthData(IOInterface ioInterface) throws IOException {
    // As per details: https://api.smugmug.com/api/v2/doc/tutorial/authorization.html
    // and example: http://stackoverflow.com/questions/15194182/examples-for-oauth1-using-google-api-java-oauth
    // Google library puts signature in header and not in request, see https://oauth.net/1/
    OAuthConsumer consumer = new GoogleOAuthConsumer(appCredentials.key(), appCredentials.secret());

    String permissions = (serviceMode == ServiceMode.EXPORT) ? "Read" : "Add";

    OAuthProvider provider = new DefaultOAuthProvider(
        "https://secure.smugmug.com/services/oauth/1.0a/getRequestToken",
        "https://secure.smugmug.com/services/oauth/1.0a/getAccessToken",
        "https://secure.smugmug.com/services/oauth/1.0a/authorize?Access=Full&Permissions="
            + permissions);

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
