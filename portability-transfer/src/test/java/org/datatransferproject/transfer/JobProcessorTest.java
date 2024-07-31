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

package org.datatransferproject.transfer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import com.google.inject.Provider;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.datatransferproject.api.launcher.DtpInternalMetricRecorder;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.types.JobAuthorization;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.spi.transfer.hooks.JobHooks;
import org.datatransferproject.spi.transfer.provider.SignalHandler;
import org.datatransferproject.spi.transfer.provider.SignalRequest;
import org.datatransferproject.spi.transfer.security.AuthDataDecryptService;
import org.datatransferproject.spi.transfer.types.CopyException;
import org.datatransferproject.spi.transfer.types.signals.SignalType;
import org.datatransferproject.transfer.copier.InMemoryDataCopier;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.auth.AuthDataPair;
import org.datatransferproject.types.transfer.retry.RetryException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class JobProcessorTest {

  private UUID jobId;
  private ExportInformation exportInfo;
  private AuthData exportAuthData;
  private AuthData importAuthData;
  private InMemoryDataCopier copier;
  private Provider<SignalHandler> importSignalHandlerProvider;
  private Provider<SignalHandler> exportSignalHandlerProvider;
  private SignalHandler importSignalHandler;
  private SignalHandler exportSignalHandler;

  private static class TestJobProcessor extends JobProcessor {

    public TestJobProcessor(JobStore jobStore,
      InMemoryDataCopier copier,
      ObjectMapper objectMapper,
      AuthDataDecryptService decryptionService,
      Provider<SignalHandler> importSignalHandler,
      Provider<SignalHandler> exportSignalHandler) {
      super(
          jobStore,
          Mockito.mock(JobHooks.class),
          objectMapper,
          copier,
          decryptionService,
          importSignalHandler,
          exportSignalHandler,
          Mockito.mock(Monitor.class),
          Mockito.mock(DtpInternalMetricRecorder.class)
      );
    }
  }

  private TestJobProcessor processor;

  @Before
  public void setUp() throws JsonProcessingException {
    importAuthData = exportAuthData = Mockito.mock(AuthData.class);
    jobId = UUID.randomUUID();
    exportInfo = Mockito.mock(ExportInformation.class);
    copier = Mockito.mock(InMemoryDataCopier.class);
    importSignalHandlerProvider = (Provider<SignalHandler>) Mockito.mock(Provider.class);
    exportSignalHandlerProvider = (Provider<SignalHandler>) Mockito.mock(Provider.class);

    final String encryptionScheme = "testEncryptionScheme";
    JobAuthorization jobAuthorization = Mockito.mock(JobAuthorization.class);
    Mockito.when(jobAuthorization.encryptionScheme()).thenReturn(encryptionScheme);
    Mockito.when(jobAuthorization.encryptedAuthData()).thenReturn("encryptedData");

    PortabilityJob job = Mockito.mock(PortabilityJob.class);
    Mockito.when(job.jobAuthorization()).thenReturn(jobAuthorization);

    JobStore jobStore = Mockito.mock(JobStore.class);
    Mockito.when(jobStore.findJob(eq(jobId))).thenReturn(job);

    AuthDataPair pair = Mockito.mock(AuthDataPair.class);
    Mockito.when(pair.getExportAuthData()).thenReturn("");
    Mockito.when(pair.getImportAuthData()).thenReturn("");

    AuthDataDecryptService decryptionService = Mockito.mock(AuthDataDecryptService.class);
    Mockito.when(decryptionService.decrypt(anyString(), any(byte[].class))).thenReturn(pair);
    Mockito.when(decryptionService.canHandle(eq(encryptionScheme))).thenReturn(true);

    ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);
    Mockito.when(objectMapper.readValue(anyString(), eq(AuthData.class)))
      .thenReturn(exportAuthData, importAuthData);

    processor = Mockito.spy(
      new TestJobProcessor(jobStore,
        copier,
        objectMapper,
        decryptionService,
        importSignalHandlerProvider,
        exportSignalHandlerProvider));

    importSignalHandler = Mockito.mock(SignalHandler.class);
    exportSignalHandler = Mockito.mock(SignalHandler.class);

    Mockito.when(importSignalHandlerProvider.get()).thenReturn(importSignalHandler);
    Mockito.when(exportSignalHandlerProvider.get()).thenReturn(exportSignalHandler);

    JobMetadata.reset();
  }

  @After
  public void cleanUp() {
    JobMetadata.reset();
  }

  @Test
  public void processJobGetsErrorsEvenWhenCopyThrows() throws CopyException, IOException, RetryException {
    JobMetadata.init(
      jobId,
      "".getBytes(),
      DataVertical.BLOBS,
      "",
      "",
      Stopwatch.createStarted());
    Mockito.doThrow(new CopyException("error", new Exception())).when(copier)
        .copy(importAuthData, exportAuthData, jobId, Optional.of(exportInfo));
    processor.processJob();
    Mockito.verify(copier).getErrors(jobId);

    Mockito.verify(importSignalHandlerProvider, Mockito.times(1)).get();
    Mockito.verify(importSignalHandler, Mockito.times(1))
      .sendSignal(any(SignalRequest.class), eq(importAuthData), any(Monitor.class));

    Mockito.verify(exportSignalHandlerProvider, Mockito.times(1)).get();
    Mockito.verify(exportSignalHandler, Mockito.times(1))
      .sendSignal(any(SignalRequest.class), eq(exportAuthData), any(Monitor.class));
  }

  @Test
  public void processJobCopiesSuccessfully() throws CopyException, IOException, RetryException {
    JobMetadata.init(
      jobId,
      "".getBytes(),
      DataVertical.BLOBS,
      "",
      "",
      Stopwatch.createUnstarted());
    Mockito.doThrow(new CopyException("error", new Exception())).when(copier)
      .copy(importAuthData, exportAuthData, jobId, Optional.of(exportInfo));
    processor.processJob();
    Mockito.verify(copier).getErrors(jobId);

    Mockito.verify(importSignalHandlerProvider, Mockito.times(2)).get();
    Mockito.verify(importSignalHandler, Mockito.times(2))
      .sendSignal(any(SignalRequest.class), eq(importAuthData), any(Monitor.class));

    Mockito.verify(exportSignalHandlerProvider, Mockito.times(2)).get();
    Mockito.verify(exportSignalHandler, Mockito.times(2))
      .sendSignal(any(SignalRequest.class), eq(exportAuthData), any(Monitor.class));
  }
}
