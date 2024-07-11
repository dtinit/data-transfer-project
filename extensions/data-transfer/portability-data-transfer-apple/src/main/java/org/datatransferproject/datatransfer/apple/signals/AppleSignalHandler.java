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

import com.google.common.annotations.VisibleForTesting;
import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.apple.AppleInterfaceFactory;
import org.datatransferproject.spi.transfer.provider.SignalHandler;
import org.datatransferproject.spi.transfer.provider.SignalRequest;
import org.datatransferproject.transfer.JobMetadata;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.transfer.retry.RetryException;
import org.datatransferproject.types.transfer.retry.RetryStrategyLibrary;
import org.datatransferproject.types.transfer.retry.RetryingCallable;
import org.jetbrains.annotations.NotNull;

public class AppleSignalHandler implements SignalHandler<TokensAndUrlAuthData> {

  protected AppCredentials appCredentials;
  protected String exportingService;
  protected Monitor monitor;
  protected RetryStrategyLibrary retryStrategyLibrary;
  private final AppleInterfaceFactory interfaceFactory;

  public AppleSignalHandler(
      @NotNull final AppCredentials appCredentials,
      @NotNull final RetryStrategyLibrary retryStrategyLibrary,
      @NotNull final Monitor monitor) {
    this(
        appCredentials,
        retryStrategyLibrary,
        monitor,
        JobMetadata.getExportService(),
        new AppleInterfaceFactory());
  }

  @VisibleForTesting
  public AppleSignalHandler(
      @NotNull final AppCredentials appCredentials,
      @NotNull final RetryStrategyLibrary retryStrategyLibrary,
      @NotNull final Monitor monitor,
      @NotNull final String exportingService,
      @NotNull final AppleInterfaceFactory interfaceFactory) {
    this.appCredentials = appCredentials;
    this.retryStrategyLibrary = retryStrategyLibrary;
    this.monitor = monitor;
    this.exportingService = exportingService;
    this.interfaceFactory = interfaceFactory;
  }

  @Override
  public void sendSignal(
    final SignalRequest signalRequest,
    final AuthData authData,
    final Monitor monitor) throws RetryException {
    Objects.requireNonNull(signalRequest, "signalRequest cannot be null");
    Objects.requireNonNull(authData, "authData cannot be null");
    Objects.requireNonNull(monitor, "monitor cannot be null");


    Callable<Void> callable =
        () -> {
          AppleSignalInterface signalInterface =
              interfaceFactory.makeSignalInterface(
                (TokensAndUrlAuthData) authData, appCredentials, exportingService, monitor);
          signalInterface.sendSignal(signalRequest);
          return null;
        };

    RetryingCallable<Void> retryingCallable =
        new RetryingCallable<>(callable, retryStrategyLibrary, Clock.systemUTC(), monitor);

    try {
      retryingCallable.call();
    } catch (Throwable e) {
      throw e;
    }
  }
}
