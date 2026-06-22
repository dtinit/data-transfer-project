/*
 * Copyright 2026 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.synology.signals;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.synology.service.SynologyDTPService;
import org.datatransferproject.datatransfer.synology.service.SynologyOAuthTokenManager;
import org.datatransferproject.spi.transfer.provider.SignalHandler;
import org.datatransferproject.spi.transfer.provider.SignalRequest;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.transfer.retry.RetryException;
import org.datatransferproject.types.transfer.retry.RetryStrategyLibrary;
import org.datatransferproject.types.transfer.retry.RetryingCallable;

/** A {@link SignalHandler} for Synology. */
public class SynologySignalHandler implements SignalHandler<TokensAndUrlAuthData> {
  private final SynologyOAuthTokenManager tokenManager;
  private final SynologyDTPService synologyDTPService;
  private final RetryStrategyLibrary retryStrategyLibrary;

  /**
   * Constructs a new {@code SynologySignalHandler} instance.
   *
   * @param synologyDTPService the Synology DTP service
   * @param tokenManager the Synology OAuth token manager
   * @param retryStrategyLibrary the retry strategy library for handling transient failures
   */
  public SynologySignalHandler(
      SynologyOAuthTokenManager tokenManager,
      SynologyDTPService synologyDTPService,
      RetryStrategyLibrary retryStrategyLibrary) {
    this.tokenManager = tokenManager;
    this.synologyDTPService = synologyDTPService;
    this.retryStrategyLibrary = retryStrategyLibrary;
  }

  @Override
  public void sendSignal(SignalRequest signalRequest, AuthData authData, Monitor monitor)
      throws RetryException {
    Objects.requireNonNull(signalRequest, "signalRequest cannot be null");
    Objects.requireNonNull(authData, "authData cannot be null");
    Objects.requireNonNull(monitor, "monitor cannot be null");

    UUID uuidJobId = UUID.fromString(signalRequest.jobId());
    tokenManager.addAuthDataIfNotExist(uuidJobId, (TokensAndUrlAuthData) authData);

    monitor.info(
        () ->
            String.format(
                "[SynologySignalHandler] Received signal for jobId: %s, jobStatus.state: %s,"
                    + " jobStatus.endReason: %s",
                signalRequest.jobId(),
                signalRequest.jobStatus().state(),
                signalRequest.jobStatus().endReason()));

    Callable<Void> sendJobSignalCallable =
        () -> {
          Boolean success =
              (Boolean)
                  this.synologyDTPService
                      .sendJobSignal(signalRequest.jobStatus(), uuidJobId)
                      .get("success");

          if (success) {
            monitor.debug(
                () ->
                    String.format(
                        "[SynologySignalHandler] Successfully sent signal for jobId: %s,"
                            + " jobStatus.state: %s, jobStatus.endReason: %s",
                        signalRequest.jobId(),
                        signalRequest.jobStatus().state(),
                        signalRequest.jobStatus().endReason()));
            return null;
          }

          throw new RuntimeException(
              String.format(
                  "Failed to send signal for jobId: %s. Response with success=false.",
                  signalRequest.jobId()));
        };

    RetryingCallable<Void> retryingSendJobSignalCallable =
        new RetryingCallable<>(
            sendJobSignalCallable, retryStrategyLibrary, Clock.systemUTC(), monitor);

    try {
      retryingSendJobSignalCallable.call();
    } catch (Throwable e) {
      throw e;
    }
  }
}
