package org.dataportabilityproject.serviceProviders.flickr;

import static com.google.common.base.Preconditions.checkArgument;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.auth.AuthInterface;
import com.flickr4java.flickr.auth.Permission;
import java.io.IOException;
import org.dataportabilityproject.shared.IOInterface;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.TokenSecretAuthData;
import org.scribe.model.Token;
import org.scribe.model.Verifier;

final class FlickrAuth implements OfflineAuthDataGenerator {
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
      return TokenSecretAuthData.create(requestToken.getToken(), requestToken.getSecret());
    } catch (FlickrException e) {
      throw new IOException("Problem verifying auth token", e);
    }
  }

  public Auth getAuth(AuthData authData) throws IOException {
    checkArgument(authData instanceof TokenSecretAuthData,
        "authData expected to be TokenSecretAuthData not %s",
        authData.getClass().getCanonicalName());
    TokenSecretAuthData data = (TokenSecretAuthData) authData;
    Token requestToken = new Token(data.token(), data.secret());
    try {
    Auth auth = flickr.getAuthInterface().checkToken(requestToken);
      return auth;
    } catch (FlickrException e) {
      throw new IOException("Problem verifying auth token", e);
    }
  }
}
