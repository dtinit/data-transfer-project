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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.apple.AppleInterfaceFactory;
import org.datatransferproject.spi.transfer.provider.SignalRequest;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.spi.transfer.types.signals.SignalType;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.transfer.retry.RetryException;
import org.datatransferproject.types.transfer.retry.RetryStrategyLibrary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AppleSignalHandlerTest {

  private AppCredentials appCredentials;
  private TokensAndUrlAuthData authData;
  private RetryStrategyLibrary retryStrategyLibrary;
  private Monitor monitor;
  private String jobId;
  private AppleInterfaceFactory interfaceFactory;
  private AppleSignalHandler signalHandler;
  private AppleSignalInterface signalInterface;
  private final String exportingService = "exportingService";

  @BeforeEach
  public void setUp() throws CopyExceptionWithFailureReason, IOException {
    jobId = UUID.randomUUID().toString();

    appCredentials = mock(AppCredentials.class);
    authData = mock(TokensAndUrlAuthData.class);
    retryStrategyLibrary = mock(RetryStrategyLibrary.class);
    monitor = mock(Monitor.class);
    interfaceFactory = mock(AppleInterfaceFactory.class);
    signalInterface = mock(AppleSignalInterface.class);

    when(interfaceFactory.makeSignalInterface(
            eq(authData), eq(appCredentials), eq(exportingService), eq(monitor)))
        .thenReturn(signalInterface);

    when(signalInterface.sendSignal(any(SignalRequest.class)))
        .thenReturn("response".getBytes());

    signalHandler =
        new AppleSignalHandler(
            appCredentials, retryStrategyLibrary, monitor, exportingService, interfaceFactory);
  }

  @Test
  public void testSendSignal() throws RetryException, CopyExceptionWithFailureReason, IOException {
    SignalRequest signalRequest =
        SignalRequest.newBuilder()
          .withJobId(jobId.toString())
          .withJobStatus(SignalType.JOB_COMPLETED.name())
          .withExportingService("EXPORT_SERVICE")
          .withImportingService("IMPORT_SERVICE")
          .withDataType(DataVertical.MAIL.getDataType())
          .build();

    signalHandler.sendSignal(signalRequest, authData, monitor);

    verify(interfaceFactory, times(1))
      .makeSignalInterface(eq(authData), eq(appCredentials), eq(exportingService), eq(monitor));
    verify(signalInterface, times(1))
      .sendSignal(eq(signalRequest));
  }
}
