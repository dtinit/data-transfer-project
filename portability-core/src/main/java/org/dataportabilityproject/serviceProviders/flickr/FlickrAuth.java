package org.dataportabilityproject.serviceProviders.flickr;

import static com.google.common.base.Preconditions.checkArgument;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.auth.AuthInterface;
import com.flickr4java.flickr.auth.Permission;
import com.google.common.base.Preconditions;
import java.io.IOException;
import org.dataportabilityproject.shared.IOInterface;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.AuthRequest;
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.TokenSecretAuthData;
import org.scribe.model.Token;
import org.scribe.model.Verifier;

final class FlickrAuth implements OfflineAuthDataGenerator, OnlineAuthDataGenerator {
  private final Flickr flickr;

  FlickrAuth(String apiKey, String apiSecret) {
    this.flickr = new Flickr(apiKey, apiSecret, new REST());
  }

  @Override
  public AuthData generateAuthData(IOInterface ioInterface) throws IOException {
    AuthInterface authInterface = flickr.getAuthInterface();
    Token token = authInterface.getRequestToken();
    String url = authInterface.getAuthorizationUrl(token, Permission.WRITE);
    String tokenKey = ioInterface.ask("Please enter the code from this url: " + url);
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
  public AuthRequest generateAuthUrl(String id) throws IOException {
    AuthInterface authInterface = flickr.getAuthInterface();
    Token token = authInterface.getRequestToken("http://localhost:8080/callback1/flickr");
    String url = authInterface.getAuthorizationUrl(token, Permission.WRITE);
    return AuthRequest.create(url, toAuthData(token));
  }

  @Override
  public AuthData generateAuthData(String authCode, String id, AuthData initialAuthData) throws IOException {
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

  private static TokenSecretAuthData toAuthData(Token token) {
    return TokenSecretAuthData.create(token.getToken(), token.getSecret());
  }

  private static Token fromAuthData(AuthData authData) {
    TokenSecretAuthData data = (TokenSecretAuthData) authData;
    return new Token(data.token(), data.secret());
  }
}
