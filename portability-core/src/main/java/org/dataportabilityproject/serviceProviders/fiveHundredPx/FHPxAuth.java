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

import com.google.api.client.auth.oauth.OAuthAuthorizeTemporaryTokenUrl;
import com.google.api.client.auth.oauth.OAuthCredentialsResponse;
import com.google.api.client.auth.oauth.OAuthGetAccessToken;
import com.google.api.client.auth.oauth.OAuthGetTemporaryToken;
import com.google.api.client.auth.oauth.OAuthHmacSigner;
import com.google.api.client.auth.oauth.OAuthParameters;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.IOException;
import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import oauth.signpost.signature.HmacSha1MessageSigner;
import org.dataportabilityproject.shared.AppCredentials;
import org.dataportabilityproject.shared.IOInterface;
import org.dataportabilityproject.shared.ServiceMode;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.AuthFlowInitiator;
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.TokenSecretAuthData;
import org.dataportabilityproject.shared.signpost.GoogleOAuthConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FHPxAuth implements OfflineAuthDataGenerator, OnlineAuthDataGenerator {

  private final AppCredentials appCredentials;
  private final ServiceMode serviceMode;  // Either import or export

  private final Logger logger = LoggerFactory.getLogger(FHPxAuth.class);

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
    String accessTokenUrl = "https://api.500px.com/v1/oauth/access_token";
    String authWebsiteUrl = "https://api.500px.com/v1/oauth/authorize";
    OAuthProvider provider = new DefaultOAuthProvider(requestTokenUrl, accessTokenUrl,
        authWebsiteUrl);

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

  @Override
  public AuthFlowInitiator generateAuthUrl(String callbackBaseUrl, String id) throws IOException {
    logger.debug("Starting 500px generateAuthUrl");

    OAuthConsumer consumer = new GoogleOAuthConsumer(appCredentials.key(), appCredentials.secret());

    String requestTokenUrl = "https://api.500px.com/v1/oauth/request_token";
    String accessTokenUrl = "https://api.500px.com/v1/oauth/access_token";
    String authWebsiteUrl = "https://api.500px.com/v1/oauth/authorize";
    OAuthProvider provider = new DefaultOAuthProvider(requestTokenUrl, accessTokenUrl,
        authWebsiteUrl);
    String tokenInfo;
    try {
      tokenInfo = provider.retrieveRequestToken(consumer,
          callbackBaseUrl + "/callback1/500px"); // so this is sent to the OAuth handler
    } catch (OAuthMessageSignerException
        | OAuthNotAuthorizedException
        | OAuthExpectationFailedException
        | OAuthCommunicationException e) {
      throw new IOException("Couldn't generate authUrl", e);
    }

    logger.debug("500px auth flow token: {}", tokenInfo);
    TokenSecretAuthData authData = TokenSecretAuthData
        .create(consumer.getToken(), consumer.getTokenSecret());
    logger.debug("500px authData: {}", authData);

    return AuthFlowInitiator
        .create(tokenInfo, authData); // tokenInfo contains a url and all the params
  }

  @Override
  public AuthData generateAuthData(String callbackBaseUrl, String authCode, String id,
      AuthData initialAuthData, String extra) throws IOException {
    logger.debug("Starting 500px generateAuthData");

    Preconditions.checkArgument(Strings.isNullOrEmpty(extra), "Extra data not expected");
    String requestTokenUrl = "https://api.500px.com/v1/oauth/request_token";
    String accessTokenUrl = "https://api.500px.com/v1/oauth/access_token";
    String authWebsiteUrl = "https://api.500px.com/v1/oauth/authorize";

    TokenSecretAuthData initialTokenSecret = (TokenSecretAuthData) initialAuthData;

    OAuthHmacSigner hmacSigner = new OAuthHmacSigner();

    OAuthGetAccessToken accessTokenRequest = new OAuthGetAccessToken(accessTokenUrl);
    accessTokenRequest.signer = hmacSigner;
    accessTokenRequest.temporaryToken = initialTokenSecret.token();
    accessTokenRequest.transport = new NetHttpTransport();
    accessTokenRequest.verifier = authCode;
    accessTokenRequest.consumerKey = appCredentials.key();

    logger.debug("Access token request: {}", accessTokenRequest);

    OAuthCredentialsResponse accessTokenResponse = accessTokenRequest.execute();

    logger.debug("Access token response: {}", accessTokenResponse);

    OAuthParameters params = new OAuthParameters();
    hmacSigner.tokenSharedSecret = initialTokenSecret.secret();
    params.signer = hmacSigner;
    params.consumerKey = appCredentials.key();
    params.token = initialTokenSecret.token();
    params.verifier = authCode;

    logger.debug("Parameters: {}", params);

    HttpRequestFactory reqFactory = new NetHttpTransport().createRequestFactory();
    GenericUrl url = new GenericUrl(authWebsiteUrl);
    HttpResponse response = reqFactory.buildGetRequest(url).execute();
    logger.debug("Response: {}", response);

    return null;
  }
}
