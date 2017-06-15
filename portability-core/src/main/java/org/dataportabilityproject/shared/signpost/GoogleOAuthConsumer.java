package org.dataportabilityproject.shared.signpost;


import oauth.signpost.AbstractOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.http.HttpRequest;
import oauth.signpost.signature.QueryStringSigningStrategy;

/**
 * Implements a SingPost {@link AbstractOAuthConsumer} that knows how to interact
 * with a a Google {@link com.google.api.client.http.HttpRequest}.
 */
public final class GoogleOAuthConsumer extends AbstractOAuthConsumer {

  public GoogleOAuthConsumer(String consumerKey, String consumerSecret) {
    super(consumerKey, consumerSecret);
  }

  @Override
  protected HttpRequest wrap(Object request) {
    if (!(request instanceof com.google.api.client.http.HttpRequest)) {
      throw new IllegalArgumentException(
          "request needs to be of type: com.google.api.client.http.HttpRequest");
    }
    return new GoogleConnectionRequestAdapter((com.google.api.client.http.HttpRequest) request);
  }

  @Override
  public synchronized HttpRequest sign(Object request) throws OAuthCommunicationException,
      OAuthExpectationFailedException, OAuthMessageSignerException {
    setSigningStrategy(new QueryStringSigningStrategy());
    return super.sign(request);
  }
}
