/*
 * Copyright 2025 The Data Transfer Project Authors.
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
 *
 */

package org.datatransferproject.datatransfer.synology.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.synology.exceptions.SynologyImportException;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SynologyOAuthTokenManagerTest {
  @InjectMocks protected SynologyOAuthTokenManager tokenManager;
  @Mock protected Monitor monitor;
  @Mock protected AppCredentials appCredentials;

  @BeforeEach
  public void setUp() throws Exception {
    lenient().when(appCredentials.getKey()).thenReturn("mockClientId");
    lenient().when(appCredentials.getSecret()).thenReturn("mockClientSecret");
  }

  @Nested
  class OAuthTokenManagerBasicTest {
    @Test
    void shouldGetAccessTokenIfExist() throws SynologyImportException {
      UUID jobId = UUID.randomUUID();
      TokensAndUrlAuthData authData =
          new TokensAndUrlAuthData("accessToken", "refreshToken", "http://mock.token.url");
      tokenManager.addAuthDataIfNotExist(jobId, authData);

      assertEquals("accessToken", tokenManager.getAccessToken(jobId));
    }

    @Test
    void shouldThrowExceptionIfJobIdNotFound() {
      UUID jobId = UUID.randomUUID();

      assertThrows(
          SynologyImportException.class,
          () -> tokenManager.getAccessToken(jobId),
          "No auth data found for job: " + jobId);
    }

    @Test
    void shouldNotReplaceExistingAuthData() throws SynologyImportException {
      UUID jobId = UUID.randomUUID();
      TokensAndUrlAuthData authData1 =
          new TokensAndUrlAuthData("accessToken1", "refreshToken1", "url1");
      TokensAndUrlAuthData authData2 =
          new TokensAndUrlAuthData("accessToken2", "refreshToken2", "url2");

      tokenManager.addAuthDataIfNotExist(jobId, authData1);
      tokenManager.addAuthDataIfNotExist(jobId, authData2);

      String accessToken = tokenManager.getAccessToken(jobId);
      assertEquals("accessToken1", accessToken);
    }
  }

  @Nested
  class RefreshTokenTest {
    @Test
    void shouldReturnFalseIfJobIdNotFound() {
      UUID jobId = UUID.randomUUID();
      OkHttpClient mockClient = mock(OkHttpClient.class);
      ObjectMapper mockObjectMapper = mock(ObjectMapper.class);

      boolean result = tokenManager.refreshToken(jobId, mockClient, mockObjectMapper);

      assertFalse(result);
    }

    @Test
    void shouldReturnFalseIfGotIOExceptionWhenSendingRequest() throws IOException {
      UUID jobId = UUID.randomUUID();
      TokensAndUrlAuthData authData =
          new TokensAndUrlAuthData("oldAccessToken", "oldRefreshToken", "http://mock.token.url");
      tokenManager.addAuthDataIfNotExist(jobId, authData);

      OkHttpClient mockClient = mock(OkHttpClient.class);
      ObjectMapper mockObjectMapper = mock(ObjectMapper.class);
      Response mockResponse = mock(Response.class);
      ResponseBody mockResponseBody = mock(ResponseBody.class);
      Call mockCall = mock(Call.class);

      when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
      doThrow(new IOException("Error when call client.execute")).when(mockCall).execute();

      boolean result = tokenManager.refreshToken(jobId, mockClient, mockObjectMapper);

      assertFalse(result);
    }

    @Test
    void shouldReturnFalseIfRefreshTokenRequestFailed() throws IOException {
      UUID jobId = UUID.randomUUID();
      TokensAndUrlAuthData authData =
          new TokensAndUrlAuthData("oldAccessToken", "oldRefreshToken", "http://mock.token.url");
      tokenManager.addAuthDataIfNotExist(jobId, authData);

      OkHttpClient mockClient = mock(OkHttpClient.class);
      ObjectMapper mockObjectMapper = mock(ObjectMapper.class);
      Response mockResponse = mock(Response.class);
      ResponseBody mockResponseBody = mock(ResponseBody.class);
      Call mockCall = mock(Call.class);

      when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
      doReturn(mockResponse).when(mockCall).execute();
      when(mockResponse.body()).thenReturn(mockResponseBody);
      when(mockResponseBody.string()).thenReturn("{}");
      when(mockResponse.isSuccessful()).thenReturn(false);

      boolean result = tokenManager.refreshToken(jobId, mockClient, mockObjectMapper);

      assertFalse(result);
    }

    @Test
    void shouldReturnFalseIfParseResponseFailed() throws IOException {
      UUID jobId = UUID.randomUUID();
      TokensAndUrlAuthData authData =
          new TokensAndUrlAuthData("oldAccessToken", "oldRefreshToken", "http://mock.token.url");
      tokenManager.addAuthDataIfNotExist(jobId, authData);

      OkHttpClient mockClient = mock(OkHttpClient.class);
      ObjectMapper mockObjectMapper = mock(ObjectMapper.class);
      Response mockResponse = mock(Response.class);
      ResponseBody mockResponseBody = mock(ResponseBody.class);
      Call mockCall = mock(Call.class);

      when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
      doReturn(mockResponse).when(mockCall).execute();
      when(mockResponse.body()).thenReturn(mockResponseBody);
      when(mockResponseBody.string())
          .thenThrow(new IOException("Error when call response.body.string()"));

      boolean result = tokenManager.refreshToken(jobId, mockClient, mockObjectMapper);

      assertFalse(result);
    }

    @Test
    void shouldReturnFalseIfParseJsonFailed() throws IOException, JsonProcessingException {
      UUID jobId = UUID.randomUUID();
      TokensAndUrlAuthData authData =
          new TokensAndUrlAuthData("oldAccessToken", "oldRefreshToken", "http://mock.token.url");
      tokenManager.addAuthDataIfNotExist(jobId, authData);

      OkHttpClient mockClient = mock(OkHttpClient.class);
      ObjectMapper mockObjectMapper = mock(ObjectMapper.class);
      Response mockResponse = mock(Response.class);
      ResponseBody mockResponseBody = mock(ResponseBody.class);
      Call mockCall = mock(Call.class);

      when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
      doReturn(mockResponse).when(mockCall).execute();
      when(mockResponse.isSuccessful()).thenReturn(true);
      when(mockResponse.body()).thenReturn(mockResponseBody);
      when(mockResponseBody.string()).thenReturn("{}");
      when(mockObjectMapper.readValue(anyString(), eq(Map.class)))
          .thenThrow(new JsonProcessingException("Error when call objectMapper.readValue") {});

      boolean result = tokenManager.refreshToken(jobId, mockClient, mockObjectMapper);

      assertFalse(result);
    }

    @Test
    void shouldUpdateTokenAndReturnTrue()
        throws IOException, JsonProcessingException, SynologyImportException {
      UUID jobId = UUID.randomUUID();
      TokensAndUrlAuthData authData =
          new TokensAndUrlAuthData("oldAccessToken", "oldRefreshToken", "http://mock.token.url");
      tokenManager.addAuthDataIfNotExist(jobId, authData);

      OkHttpClient mockClient = mock(OkHttpClient.class);
      ObjectMapper mockObjectMapper = mock(ObjectMapper.class);
      Response mockResponse = mock(Response.class);
      ResponseBody mockResponseBody = mock(ResponseBody.class);
      Call mockCall = mock(Call.class);

      when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
      doReturn(mockResponse).when(mockCall).execute();
      when(mockResponse.isSuccessful()).thenReturn(true);
      when(mockResponse.body()).thenReturn(mockResponseBody);
      when(mockResponseBody.string()).thenReturn("{}");
      when(mockObjectMapper.readValue(anyString(), eq(Map.class)))
          .thenReturn(Map.of("access_token", "newAccessToken", "refresh_token", "newRefreshToken"));

      boolean result = tokenManager.refreshToken(jobId, mockClient, mockObjectMapper);

      assertTrue(result);
      assertEquals("newAccessToken", tokenManager.getAccessToken(jobId));
    }
  }
}
