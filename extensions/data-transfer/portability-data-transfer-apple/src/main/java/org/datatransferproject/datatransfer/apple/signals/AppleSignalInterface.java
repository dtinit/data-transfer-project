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

package org.datatransferproject.datatransfer.apple.signals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.apple.AppleBaseInterface;
import org.datatransferproject.datatransfer.apple.AppleInterfaceFactory;
import org.datatransferproject.datatransfer.apple.constants.AuditKeys;
import org.datatransferproject.datatransfer.apple.constants.Headers;
import org.datatransferproject.spi.transfer.provider.SignalRequest;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.datatransferproject.spi.transfer.types.UnconfirmedUserException;
import org.datatransferproject.transfer.JobMetadata;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.transfer.retry.RetryStrategyLibrary;
import org.jetbrains.annotations.NotNull;

/**
 * An Interface to send the Transfer Signals to Apple.  
 */
public class AppleSignalInterface implements AppleBaseInterface {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  protected String baseUrl;
  protected AppCredentials appCredentials;
  protected String exportingService;
  protected Monitor monitor;
  protected TokensAndUrlAuthData authData;

  public AppleSignalInterface(
      @NotNull final TokensAndUrlAuthData authData,
      @NotNull final AppCredentials appCredentials,
      @NotNull final Monitor monitor) {
    this.authData = authData;
    this.appCredentials = appCredentials;
    this.monitor = monitor;
    this.baseUrl = "https://datatransfer.apple.com/jobs/%s/status";
  }

  @Override
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

      IOUtils.write(requestData, con.getOutputStream());
      responseString = IOUtils.toString(con.getInputStream(), StandardCharsets.ISO_8859_1);

    } catch (IOException e) {
      try {
        monitor.severe(
            () -> "Exception from POST in AppleSignalInterface",
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

  public byte[] sendSignal(@NotNull final SignalRequest signalRequest)
      throws IOException, CopyExceptionWithFailureReason {
    byte[] responseData = null;
    final String url = String.format(baseUrl, signalRequest.getJobId());
    final byte[] requestBody = OBJECT_MAPPER.writeValueAsBytes(signalRequest);
    try {
      final String responseString = sendPostRequest(url, requestBody);
      responseData = responseString.getBytes(StandardCharsets.ISO_8859_1);
    } catch (CopyExceptionWithFailureReason e) {
      if (e instanceof UnconfirmedUserException || e instanceof PermissionDeniedException) {
        this.authData = refreshTokens(authData, appCredentials, monitor);
        final String responseString = sendPostRequest(url, requestBody);
        responseData = responseString.getBytes(StandardCharsets.ISO_8859_1);
      } else {
        throw e;
      }
    }
    return responseData;
  }
}
