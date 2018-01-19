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
package org.dataportabilityproject.serviceProviders.flickr;

import static com.google.common.base.Preconditions.checkArgument;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.auth.AuthInterface;
import com.flickr4java.flickr.auth.Permission;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.IOException;
import javax.annotation.Nullable;
import org.dataportabilityproject.shared.AppCredentials;
import org.dataportabilityproject.shared.IOInterface;
import org.dataportabilityproject.shared.ServiceMode;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.AuthFlowInitiator;
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.TokenSecretAuthData;
import org.scribe.model.Token;
import org.scribe.model.Verifier;

final class FlickrAuth implements OfflineAuthDataGenerator, OnlineAuthDataGenerator {

  private final Flickr flickr;
  private final ServiceMode serviceMode;

  FlickrAuth(AppCredentials appCredentials, ServiceMode serviceMode) {
    Preconditions.checkNotNull(appCredentials);
    this.flickr = new Flickr(appCredentials.key(), appCredentials.secret(), new REST());
    this.serviceMode = serviceMode;
  }

  private static TokenSecretAuthData toAuthData(Token token) {
    return TokenSecretAuthData.create(token.getToken(), token.getSecret());
  }

  private static Token fromAuthData(AuthData authData) {
    TokenSecretAuthData data = (TokenSecretAuthData) authData;
    return new Token(data.token(), data.secret());
  }

  @Override
  public AuthData generateAuthData(IOInterface ioInterface) throws IOException {
    AuthInterface authInterface = flickr.getAuthInterface();
    Token token = authInterface.getRequestToken();
    String url = authInterface.getAuthorizationUrl(token, Permission.WRITE);
    String tokenKey = ioInterface.ask("Please enter the code from this authUrl: " + url);
    Token requestToken = authInterface.getAccessToken(token, new Verifier(tokenKey));
    try {
      Auth auth = authInterface.checkToken(requestToken);
      return toAuthData(requestToken);
    } catch (FlickrException e) {
      throw new IOException("Problem verifying auth token", e);
    }
  }

  public Auth getAuth(AuthData authData) throws IOException {
    checkArgument(authData instanceof TokenSecretAuthData,
        "authData expected to be TokenSecretAuthData not %s",
        authData.getClass().getCanonicalName());
    Token requestToken = fromAuthData(authData);
    try {
      Auth auth = flickr.getAuthInterface().checkToken(requestToken);
      return auth;
    } catch (FlickrException e) {
      throw new IOException("Problem verifying auth token", e);
    }
  }

  @Override // online case
  public AuthFlowInitiator generateAuthUrl(String callbackBaseUrl, String id) throws IOException {
    AuthInterface authInterface = flickr.getAuthInterface();
    Token token = authInterface.getRequestToken(
        callbackBaseUrl + "/callback1/flickr");
    String url = authInterface.getAuthorizationUrl(token,
        serviceMode == ServiceMode.IMPORT ? Permission.WRITE : Permission.READ);
    return AuthFlowInitiator.create(url, toAuthData(token));
  }

  @Override
  public AuthData generateAuthData(String callbackBaseUrl, String authCode, String id,
      AuthData initialAuthData, @Nullable String extra) throws IOException {
    Preconditions.checkArgument(Strings.isNullOrEmpty(extra), "Extra data not expected");
    Preconditions
        .checkNotNull(initialAuthData, "Earlier auth data not expected for Google flow");
    AuthInterface authInterface = flickr.getAuthInterface();
    Token token = fromAuthData(initialAuthData);
    Token requestToken = authInterface.getAccessToken(token, new Verifier(authCode));
    try {
      Auth auth = authInterface.checkToken(requestToken);
      return TokenSecretAuthData.create(requestToken.getToken(), requestToken.getSecret());
    } catch (FlickrException e) {
      throw new IOException("Problem verifying auth token", e);
    }
  }
}
