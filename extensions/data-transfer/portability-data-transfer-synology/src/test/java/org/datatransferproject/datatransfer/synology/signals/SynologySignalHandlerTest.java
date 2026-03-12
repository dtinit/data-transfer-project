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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.synology.service.SynologyDTPService;
import org.datatransferproject.datatransfer.synology.service.SynologyOAuthTokenManager;
import org.datatransferproject.spi.transfer.provider.SignalRequest;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle.EndReason;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle.State;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.transfer.retry.RetryException;
import org.datatransferproject.types.transfer.retry.RetryMapping;
import org.datatransferproject.types.transfer.retry.RetryStrategy;
import org.datatransferproject.types.transfer.retry.RetryStrategyLibrary;
import org.datatransferproject.types.transfer.retry.UniformRetryStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SynologySignalHandlerTest {

  private SynologyOAuthTokenManager tokenManager;
  private RetryStrategyLibrary retryStrategyLibrary;
  private Monitor monitor;
  private String jobId;
  private SynologySignalHandler signalHandler;
  private SynologyDTPService synologyDTPService;
  private TokensAndUrlAuthData authData;

  @BeforeEach
  public void setUp() {
    jobId = UUID.randomUUID().toString();

    tokenManager = mock(SynologyOAuthTokenManager.class);
    RetryStrategy retryStrategy = new UniformRetryStrategy(3, 1, "test");
    RetryMapping retryMapping =
        new RetryMapping(
            new String[] {RuntimeException.class.getName()}, new String[] {}, retryStrategy);
    retryStrategyLibrary =
        new RetryStrategyLibrary(
            Collections.singletonList(retryMapping), new UniformRetryStrategy(1, 1, "default"));
    monitor = mock(Monitor.class);
    synologyDTPService = mock(SynologyDTPService.class);
    authData = mock(TokensAndUrlAuthData.class);

    signalHandler =
        new SynologySignalHandler(tokenManager, synologyDTPService, retryStrategyLibrary);
  }

  @Test
  public void testSendSignal() throws RetryException, CopyExceptionWithFailureReason {
    JobLifeCycle jobStatus =
        JobLifeCycle.builder()
            .setState(State.ENDED)
            .setEndReason(EndReason.SUCCESSFULLY_COMPLETED)
            .build();

    SignalRequest signalRequest =
        SignalRequest.builder()
            .setJobId(jobId)
            .setJobStatus(jobStatus)
            .setExportingService("EXPORT_SERVICE")
            .setImportingService("IMPORT_SERVICE")
            .setDataType(DataVertical.MAIL.getDataType())
            .build();

    when(synologyDTPService.sendJobSignal(any(JobLifeCycle.class), any(UUID.class)))
        .thenReturn(Map.of("success", true));

    signalHandler.sendSignal(signalRequest, authData, monitor);

    verify(synologyDTPService, times(1)).sendJobSignal(any(JobLifeCycle.class), any(UUID.class));
  }

  @Test
  public void testSendSignalRetry() throws RetryException, CopyExceptionWithFailureReason {
    JobLifeCycle jobStatus =
        JobLifeCycle.builder()
            .setState(State.ENDED)
            .setEndReason(EndReason.SUCCESSFULLY_COMPLETED)
            .build();

    SignalRequest signalRequest =
        SignalRequest.builder()
            .setJobId(jobId)
            .setJobStatus(jobStatus)
            .setExportingService("EXPORT_SERVICE")
            .setImportingService("IMPORT_SERVICE")
            .setDataType(DataVertical.MAIL.getDataType())
            .build();

    when(synologyDTPService.sendJobSignal(any(JobLifeCycle.class), any(UUID.class)))
        .thenThrow(new RuntimeException("Failed to send signal"))
        .thenReturn(Map.of("success", true));

    signalHandler.sendSignal(signalRequest, authData, monitor);

    verify(synologyDTPService, times(2)).sendJobSignal(any(JobLifeCycle.class), any(UUID.class));
  }
}
