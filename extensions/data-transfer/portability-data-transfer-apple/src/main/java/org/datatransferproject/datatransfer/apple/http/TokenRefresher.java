/*
 * Copyright 2024 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.datatransferproject.datatransfer.apple.http;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class TokenRefresher {
  private TokenRefresher() {} // prevent instantiation

  // TODO(jzacsh, hgandhi90) refactor refresh-token flow in AppleMediaInterface to use this codepath
  // instead of what it has now.
  public static HttpRequest.Builder buildRefreshRequestUrlForAccessToken(
      TokensAndUrlAuthData authData, AppCredentials appCredentials) throws IllegalStateException {
    BodyPublisher postBody = buildRefreshRequestPostBody(authData, appCredentials);
    URI refreshUri = authData.getTokenServerEncodedUri();
    return HttpRequest.newBuilder()
      .uri(refreshUri)
      .POST(postBody)
      .header("content-type", "application/x-www-form-urlencoded");
  }

  private static BodyPublisher buildRefreshRequestPostBody(
      TokensAndUrlAuthData authData, AppCredentials appCredentials) throws IllegalStateException {
    final Map<String, String> parameters = new HashMap<>();
    parameters.put("client_id", checkString(appCredentials.getKey(), "AppCredentials#getKey"));
    parameters.put(
        "client_secret", checkString(appCredentials.getSecret(), "AppCredentials#getSecret"));
    parameters.put("grant_type", "refresh_token");
    parameters.put(
        "refresh_token",
        checkString(authData.getRefreshToken(), "TokensAndUrlAuthData#getRefreshToken"));

    StringJoiner sj = new StringJoiner("&");
    for (Map.Entry<String, String> entry : parameters.entrySet()) {
      sj.add(entry.getKey() + "=" + entry.getValue());
    }
    return HttpRequest.BodyPublishers.ofString(sj.toString());
  }

  /** Checks a {@code content} is not null or empty and passes it through. */
  private static String checkString(String content, String erroMessageTitle)
      throws IllegalStateException {
    checkState(
        !isNullOrEmpty(content), "require non-empty %s, but got \"%s\"", erroMessageTitle, content);
    return content;
  }
}
