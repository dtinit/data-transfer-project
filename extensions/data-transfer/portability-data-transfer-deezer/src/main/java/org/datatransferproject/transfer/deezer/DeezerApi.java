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
package org.datatransferproject.transfer.deezer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Map;
import org.datatransferproject.transfer.deezer.model.Error;
import org.datatransferproject.transfer.deezer.model.InsertResponse;
import org.datatransferproject.transfer.deezer.model.PlaylistDetails;
import org.datatransferproject.transfer.deezer.model.PlaylistSummary;
import org.datatransferproject.transfer.deezer.model.PlaylistsResponse;
import org.datatransferproject.transfer.deezer.model.Track;
import org.datatransferproject.transfer.deezer.model.User;

/**
 * A utility wrapper for interacting with the Deezer Api.
 *
 * <p>See: https://developers.deezer.com/api/explorer
 */
public class DeezerApi {
  private static final ObjectMapper MAPPER =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  private static final String BASE_URL = "https://api.deezer.com";

  private final String accessToken;
  private final HttpTransport httpTransport;

  public DeezerApi(String accessToken, HttpTransport httpTransport) {
    this.accessToken = accessToken;
    this.httpTransport = httpTransport;
  }

  public User getUser() throws IOException {
    return makeRequest(BASE_URL + "/user/me", User.class);
  }

  public Collection<PlaylistSummary> getPlaylists() throws IOException {
    return ImmutableList.copyOf(
        makeRequest(BASE_URL + "/user/me/playlists", PlaylistsResponse.class)
            .getData());
  }

  public PlaylistDetails getPlaylistDetails(long playlistId) throws IOException {
    return makeRequest(BASE_URL + "/playlist/" + playlistId, PlaylistDetails.class);
  }

  public Track getTrack(long trackId) throws IOException {
    return makeRequest(BASE_URL + "/track/" + trackId, Track.class);
  }

  public InsertResponse createPlaylist(String title) throws IOException {
    String result = makePostRequest(
        BASE_URL + "/user/me/playlists",
        ImmutableMap.of("title", title));
    return MAPPER.readValue(result, InsertResponse.class);
  }

  public Error insertTracksInPlaylist(long playlist, Collection<Long> tracks)
      throws IOException {
    if (tracks.isEmpty()) {
      return null;
    }
    // Track inserts return true if successful and an Error json object on error....
    String result = makePostRequest(
        BASE_URL + "/playlist/" + playlist + "/tracks",
        ImmutableMap.of("songs", Joiner.on(",").join(tracks)));
    if ("true".equalsIgnoreCase(result)) {
      return null;
    }
    return MAPPER.readValue(result, Error.class);
  }

  public Track lookupTrackByIsrc(String isrc) throws IOException {
    // This is undocumented in their official API, but was recommended here:
    // https://stackoverflow.com/questions/21484347/query-track-by-isrc
    return makeRequest(BASE_URL + "/2.0/track/isrc:" + isrc, Track.class);
  }

  private String makePostRequest(String url, Map<String, String> params) throws IOException {
    HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
    StringBuilder extraArgs = new StringBuilder();
    params.entrySet().forEach(entry -> {
      try {
        extraArgs
            .append("&")
            .append(entry.getKey())
            .append("=")
            .append(URLEncoder.encode(entry.getValue(), "UTF8"));
      } catch (UnsupportedEncodingException e) {
        throw new IllegalArgumentException(e);
      }
    });
    HttpRequest getRequest =
        requestFactory.buildGetRequest(
            new GenericUrl(url
                + "?output=json&request_method=post&access_token=" + accessToken
                + extraArgs));
    HttpResponse response = getRequest.execute();
    int statusCode = response.getStatusCode();
    if (statusCode != 200) {
      throw new IOException(
          "Bad status code: " + statusCode + " error: " + response.getStatusMessage());
    }
    String result =
        CharStreams.toString(new InputStreamReader(response.getContent(), Charsets.UTF_8));
    return result;
  }

  private <T> T makeRequest(String url, Class<T> clazz)
      throws IOException {
    HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
    HttpRequest getRequest =
        requestFactory.buildGetRequest(
            new GenericUrl(url + "?output=json&access_token=" + accessToken));
    HttpResponse response = getRequest.execute();
    int statusCode = response.getStatusCode();
    if (statusCode != 200) {
      throw new IOException(
          "Bad status code: " + statusCode + " error: " + response.getStatusMessage());
    }
    String result =
        CharStreams.toString(new InputStreamReader(response.getContent(), Charsets.UTF_8));
    return MAPPER.readValue(result, clazz);
  }
}