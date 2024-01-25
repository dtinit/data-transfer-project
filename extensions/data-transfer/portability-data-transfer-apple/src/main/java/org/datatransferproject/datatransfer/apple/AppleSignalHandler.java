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

package org.datatransferproject.datatransfer.apple;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.apple.constants.AuditKeys;
import org.datatransferproject.datatransfer.apple.constants.Headers;
import org.datatransferproject.spi.transfer.provider.SignalHandler;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.datatransferproject.spi.transfer.types.UnconfirmedUserException;
import org.datatransferproject.spi.transfer.types.signals.SignalType;
import org.datatransferproject.transfer.JobMetadata;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.transfer.retry.RetryException;
import org.datatransferproject.types.transfer.retry.RetryStrategyLibrary;
import org.datatransferproject.types.transfer.retry.RetryingCallable;
import org.jetbrains.annotations.NotNull;

public class AppleSignalHandler implements SignalHandler<TokensAndUrlAuthData>, AppleBaseInterface {

  protected TokensAndUrlAuthData authData;
  protected AppCredentials appCredentials;
  protected String baseUrl;
  protected String exportingService;
  protected Monitor monitor;
  protected RetryStrategyLibrary retryStrategyLibrary;

  public AppleSignalHandler(
    @NotNull final AppCredentials appCredentials,
    @NotNull final RetryStrategyLibrary retryStrategyLibrary,
    @NotNull final Monitor monitor) {
    this.authData = Objects.requireNonNull(authData, "authData cannot be null");
    this.appCredentials = appCredentials;
    this.baseUrl = "https://datatransfer.apple.com/jobs/%s/status";
    this.exportingService = JobMetadata.getExportService();
    this.retryStrategyLibrary = retryStrategyLibrary;
    this.monitor = monitor;
  }

  @Override
  public void sendSignal(
      UUID jobId, SignalType signalType, TokensAndUrlAuthData authData, Monitor monitor)
    throws RetryException, InvalidTokenException, IOException {
    String url = String.format(baseUrl, jobId.toString());

    HashMap<String, String> requestBody =
        new HashMap<>() {
          {
            put("jobId", jobId.toString());
            put("jobStatus", signalType.name());
          }
        };

    byte[] requestBodyBytes = SerializationUtils.serialize(requestBody);

    Callable<Void> callable = () -> {
      sendPostRequest(url, requestBodyBytes);
      return null;
    };

    RetryingCallable<Void> retryingCallable =
      new RetryingCallable<>(
        callable,
        retryStrategyLibrary,
        Clock.systemUTC(),
        monitor);

    try {
      retryingCallable.call();
    } catch (Throwable e) {
      if (e instanceof UnconfirmedUserException || e instanceof PermissionDeniedException) {
        this.authData = refreshTokens(authData, appCredentials, monitor);
        retryingCallable.call();
      } else {
        throw e;
      }
    }
  }

  public String sendPostRequest(@NotNull String url, @NotNull final byte[] requestData)
      throws IOException, CopyExceptionWithFailureReason {
    final String correlationId = UUID.randomUUID().toString();
    final UUID jobId = JobMetadata.getJobId();
    monitor.info(
        () -> "POST Request from Apple Signal Handler",
        Headers.CORRELATION_ID,
        correlationId,
        AuditKeys.uri,
        url,
        AuditKeys.jobId,
        jobId.toString());

    HttpURLConnection con = null;

    String responseString = null;
    try {
      URL signalUrl = new URL(url);

      con = (HttpURLConnection) signalUrl.openConnection();
      con.setDoOutput(true);
      con.setRequestMethod("POST");
      con.setRequestProperty(Headers.AUTHORIZATION.getValue(), authData.getAccessToken());
      con.setRequestProperty(Headers.CORRELATION_ID.getValue(), correlationId);

      if (url.contains(baseUrl)) {
        // which means we are not sending request to get access token, the
        // contentStream is not filled with params, but with DTP transfer request
        con.setRequestProperty(Headers.CONTENT_TYPE.getValue(), "");
      }
      IOUtils.write(requestData, con.getOutputStream());
      responseString = IOUtils.toString(con.getInputStream(), StandardCharsets.ISO_8859_1);

    } catch (IOException e) {
      try {
        monitor.severe(
            () -> "Exception from POST in AppleMediaInterface",
            Headers.CORRELATION_ID.getValue(),
            correlationId,
            AuditKeys.jobId,
            jobId.toString(),
            AuditKeys.error,
            e.getMessage(),
            AuditKeys.errorCode,
            con.getResponseCode(),
            e);
        convertAndThrowException(e, con);
      } finally {
        con.disconnect();
      }
      return responseString;
    }
    return null;
  }
}
