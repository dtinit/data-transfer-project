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
package org.dataportabilityproject.shared.signpost;

import oauth.signpost.AbstractOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.http.HttpRequest;
import oauth.signpost.signature.QueryStringSigningStrategy;

/**
 * Implements a SingPost {@link AbstractOAuthConsumer} that knows how to interact with a a Google
 * {@link com.google.api.client.http.HttpRequest}.
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
  public synchronized HttpRequest sign(Object request)
      throws OAuthCommunicationException, OAuthExpectationFailedException,
          OAuthMessageSignerException {
    setSigningStrategy(new QueryStringSigningStrategy());
    return super.sign(request);
  }
}
