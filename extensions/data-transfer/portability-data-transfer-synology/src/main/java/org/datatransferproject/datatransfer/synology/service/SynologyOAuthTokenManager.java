/*
 * Copyright 2023 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.synology.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.synology.exceptions.SynologyImportException;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

/** Manages the OAuth tokens from Synology Account. */
public class SynologyOAuthTokenManager {
  private final Map<UUID, TokensAndUrlAuthData> authMap = new HashMap<>();
  private final AppCredentials appCredentials;
  private final Monitor monitor;

  public SynologyOAuthTokenManager(AppCredentials appCredentials, Monitor monitor) {
    this.appCredentials = appCredentials;
    this.monitor = monitor;
  }

  public String getAccessToken(UUID jobId) {
    if (!authMap.containsKey(jobId)) {
      throw new SynologyImportException("No auth data found for job: " + jobId);
    }
    return authMap.get(jobId).getAccessToken();
  }

  public void addAuthDataIfNotExist(UUID jobId, TokensAndUrlAuthData authData) {
    authMap.putIfAbsent(jobId, authData);
  }

  public boolean refreshToken(UUID jobId, OkHttpClient client, ObjectMapper objectMapper) {
    monitor.info(
        () -> "=== [SynologyImporter] Refresh Synology Account token for job:", jobId, "===");
    if (!authMap.containsKey(jobId)) {
      monitor.severe(() -> "[SynologyImporter] No auth data found for job:", jobId);
      return false;
    }

    TokensAndUrlAuthData authData = authMap.get(jobId);
    Request request =
        new Request.Builder()
            .url(authData.getTokenServerEncodedUrl())
            .addHeader("Accept", "application/json")
            .post(
                new FormBody.Builder()
                    .add("client_id", appCredentials.getKey())
                    .add("client_secret", appCredentials.getSecret())
                    .add("refresh_token", authData.getRefreshToken())
                    .add("grant_type", "refresh_token")
                    .build())
            .build();
    Response response;
    try {
      response = client.newCall(request).execute();
    } catch (IOException e) {
      monitor.severe(
          () -> "[SynologyImporter] Failed to send refresh token request for job:",
          jobId,
          "with exception:",
          e);
      return false;
    }

    String responseBody;
    try {
      responseBody = response.body().string();
      if (!response.isSuccessful()) {
        monitor.severe(
            () -> "[SynologyImporter] Failed to refresh token for job:",
            jobId,
            "with failed response:",
            responseBody);
        return false;
      }
    } catch (IOException e) {
      monitor.severe(
          () -> "[SynologyImporter] Failed to stringify response body, jobId:",
          jobId,
          "with exception:",
          e);
      return false;
    }

    try {
      Map<String, Object> responseData = objectMapper.readValue(responseBody, Map.class);
      String accessToken = (String) responseData.get("access_token");
      String refreshToken = (String) responseData.get("refresh_token");
      authData =
          new TokensAndUrlAuthData(accessToken, refreshToken, authData.getTokenServerEncodedUrl());
      authMap.replace(jobId, authData);
    } catch (JsonProcessingException e) {
      monitor.severe(
          () -> "[SynologyImporter] Failed to parse response for job:",
          jobId,
          "with exception:",
          e);
      return false;
    }

    monitor.info(
        () -> "=== [SynologyImporter] Successfully refreshed token for job:", jobId, "===");
    return true;
  }
}
