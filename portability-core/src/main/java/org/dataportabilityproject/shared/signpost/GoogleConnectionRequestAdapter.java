/*
 * Copyright 2018 The Data Transfer Project Authors.
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

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Implements a SingPost {@link oauth.signpost.http.HttpRequest} that knows how to interact with a a
 * Google {@link HttpRequest}.
 */
final class GoogleConnectionRequestAdapter implements oauth.signpost.http.HttpRequest {
  private final HttpRequest httpRequest;

  GoogleConnectionRequestAdapter(HttpRequest httpRequest) {
    this.httpRequest = httpRequest;
  }

  @Override
  public String getMethod() {
    return httpRequest.getRequestMethod();
  }

  @Override
  public String getRequestUrl() {
    return httpRequest.getUrl().toString();
  }

  @Override
  public void setRequestUrl(String url) {
    httpRequest.setUrl(new GenericUrl(url));
  }

  @Override
  public void setHeader(String name, String value) {
    HttpHeaders headers = httpRequest.getHeaders();
    try {
      if ("Authorization".equals(name)) {
        headers.setAuthenticate(value);
      } else {
        headers.set(name, value);
      }
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("Couldn't set: " + name + " to " + value, e);
    }
  }

  @Override
  public String getHeader(String name) {
    return (String) httpRequest.getHeaders().get(name);
  }

  @Override
  public Map<String, String> getAllHeaders() {
    ImmutableMap.Builder<String, String> headers = ImmutableMap.builder();
    for (String key : httpRequest.getHeaders().keySet()) {
      Object o = httpRequest.getHeaders().get(key);
      if (o instanceof String) {
        headers.put(key, (String) o);
      } else if (o instanceof List) {
        headers.put(key, (String) ((List) o).get(0));
      }
    }
    return headers.build();
  }

  @Override
  public InputStream getMessagePayload() throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    httpRequest.getContent().writeTo(outputStream);
    return new ByteArrayInputStream(outputStream.toByteArray());
  }

  @Override
  public String getContentType() {
    return httpRequest.getContent().getType();
  }

  @Override
  public Object unwrap() {
    return httpRequest;
  }
}
