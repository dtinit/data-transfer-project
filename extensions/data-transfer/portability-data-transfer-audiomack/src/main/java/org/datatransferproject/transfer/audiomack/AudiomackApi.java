/*
 * Copyright 2019 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.audiomack;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.HttpTransport;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.datatransferproject.transfer.audiomack.model.AudiomackResponse;
import org.datatransferproject.transfer.audiomack.model.Playlist;
import org.datatransferproject.transfer.audiomack.model.User;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

public class AudiomackApi {

  private static final ObjectMapper MAPPER = new ObjectMapper().configure(
      DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  private static final String BASE_URL = "https://api.audiomack.com/v1/";

  private final OAuthService oAuthService;
  private final HttpTransport httpTransport;
  private final Token accessToken;

  AudiomackApi(
      HttpTransport httpTransport,
      AppCredentials appCredentials,
      TokenSecretAuthData authData) {
    this.httpTransport = httpTransport;
    this.oAuthService =
        new ServiceBuilder()
            .apiKey(appCredentials.getKey())
            .apiSecret(appCredentials.getSecret())
            .provider(AudiomackOAuthApi.class)
            .build();
    this.accessToken = new Token(authData.getToken(), authData.getSecret());
  }

  public List<Long> getUserPlaylistIds() throws IOException {
    User user = makeRequest("https://api.audiomack.com/v1/user",
        new TypeReference<AudiomackResponse<User>>() {});
    return user.getPlaylists();
  }

  public Playlist getIndividualPlaylist(long id) throws IOException {
    return makeRequest("https://api.audiomack.com/v1/" + id,
        new TypeReference<AudiomackResponse<Playlist>>() {});
  }

  private <T> T makeRequest(String url, TypeReference<AudiomackResponse<T>> typeReference)
      throws IOException {
    String fullUrl;
    if (!url.contains("https://")) {
      fullUrl = BASE_URL + url;
    } else {
      fullUrl = url;
    }
    OAuthRequest request = new OAuthRequest(Verb.GET, fullUrl);
    oAuthService.signRequest(accessToken, request);
    final Response response = request.send();

    if (response.getCode() < 200 || response.getCode() >= 300) {
      throw new IOException(
          String.format("Error occurred in request for %s : %s", url, response.getMessage()));
    }

    String result = response.getBody();
    return MAPPER.readValue(result, typeReference);
  }

  private <T> T postRequest(
      String url,
      Map<String, String> contentParams,
      TypeReference<T> typeReference) throws IOException {

    String fullUrl = url;
    if (!fullUrl.contains("://")) {
      fullUrl = BASE_URL + url;
    }
    OAuthRequest request = new OAuthRequest(Verb.POST, fullUrl);

    for (Entry<String, String> param : contentParams.entrySet()) {
      request.addBodyParameter(param.getKey(), param.getValue());
    }

    oAuthService.signRequest(accessToken, request);

    Response response = request.send();
    if (response.getCode() < 200 || response.getCode() >= 300) {
      throw new IOException(
          String.format("Error occurred in request for %s : %s", fullUrl, response.getMessage()));
    }

    return MAPPER.readValue(response.getBody(), typeReference);
  }
}
