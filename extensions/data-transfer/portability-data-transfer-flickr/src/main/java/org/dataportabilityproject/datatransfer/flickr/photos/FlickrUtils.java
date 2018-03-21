package org.dataportabilityproject.datatransfer.flickr.photos;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.auth.Auth;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.auth.TokenSecretAuthData;
import org.scribe.model.Token;

import static com.google.common.base.Preconditions.checkArgument;

public class FlickrUtils {
  public static Auth getAuth(AuthData authData, Flickr flickr) throws FlickrException {
    checkArgument(
            authData instanceof TokenSecretAuthData,
            "authData expected to be TokenSecretAuthData not %s",
            authData.getClass().getCanonicalName());
    TokenSecretAuthData tokenAuthData = (TokenSecretAuthData) authData;
    Token requestToken = new Token(tokenAuthData.getToken(), tokenAuthData.getSecret());
    return flickr.getAuthInterface().checkToken(requestToken);
  }
}
