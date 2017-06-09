package org.dataportabilityproject.shared;

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
 * Implements a SingPost {@link oauth.signpost.http.HttpRequest} that knows how to interact
 * with a a Google {@link HttpRequest}.
 */
public final class GoogleConnectionRequestAdapter implements oauth.signpost.http.HttpRequest {
  private final HttpRequest httpRequest;

  public GoogleConnectionRequestAdapter(HttpRequest httpRequest) {
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
