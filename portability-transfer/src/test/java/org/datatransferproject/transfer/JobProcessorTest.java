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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import com.google.inject.Provider;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.datatransferproject.api.launcher.DtpInternalMetricRecorder;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.types.JobAuthorization;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.spi.transfer.hooks.JobHooks;
import org.datatransferproject.spi.transfer.provider.SignalHandler;
import org.datatransferproject.spi.transfer.provider.SignalRequest;
import org.datatransferproject.spi.transfer.security.AuthDataDecryptService;
import org.datatransferproject.spi.transfer.types.CopyException;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle.EndReason;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle.State;
import org.datatransferproject.transfer.copier.InMemoryDataCopier;
import org.datatransferproject.transfer.copier.TelemetryCounters;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.auth.AuthDataPair;
import org.datatransferproject.types.transfer.errors.ErrorDetail;
import org.datatransferproject.types.transfer.retry.RetryException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class JobProcessorTest {

  private UUID jobId;
  private UUID recurringJobId;
  private ExportInformation exportInfo;
  private AuthData exportAuthData;
  private AuthData importAuthData;
  private InMemoryDataCopier copier;
  private Provider<SignalHandler> importSignalHandlerProvider;
  private Provider<SignalHandler> exportSignalHandlerProvider;
  private SignalHandler importSignalHandler;
  private SignalHandler exportSignalHandler;
  private JobAuthorization jobAuthorization;
  private PortabilityJob job;
  private AuthDataDecryptService decryptionService;
  private ObjectMapper objectMapper;
  private JobStore jobStore;

  private static class TestJobProcessor extends JobProcessor {

    public TestJobProcessor(JobStore jobStore,
      InMemoryDataCopier copier,
      ObjectMapper objectMapper,
      AuthDataDecryptService decryptionService,
      Boolean transferSignalEnabled,
      Provider<SignalHandler> importSignalHandler,
      Provider<SignalHandler> exportSignalHandler) {
      this(
          jobStore,
          copier,
          objectMapper,
          decryptionService,
          new TransferSignals(
              exportSignalHandler,
              importSignalHandler,
              transferSignalEnabled,
              false,
              TransferSignals.DEFAULT_HEARTBEAT_INTERVAL,
              Optional.empty(),
              TransferSignals.CANCEL_END_TIMEOUT,
              Mockito.mock(Monitor.class),
              Clock.systemUTC()));
    }

    public TestJobProcessor(JobStore jobStore,
      InMemoryDataCopier copier,
      ObjectMapper objectMapper,
      AuthDataDecryptService decryptionService,
      TransferSignals signals) {
      super(
          jobStore,
          Mockito.mock(JobHooks.class),
          objectMapper,
          copier,
          decryptionService,
          signals,
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
    recurringJobId = UUID.randomUUID();
    exportInfo = Mockito.mock(ExportInformation.class);
    copier = Mockito.mock(InMemoryDataCopier.class);
    importSignalHandlerProvider = (Provider<SignalHandler>) Mockito.mock(Provider.class);
    exportSignalHandlerProvider = (Provider<SignalHandler>) Mockito.mock(Provider.class);

    final String encryptionScheme = "testEncryptionScheme";

    jobAuthorization = Mockito.mock(JobAuthorization.class);
    Mockito.when(jobAuthorization.encryptionScheme()).thenReturn(encryptionScheme);
    Mockito.when(jobAuthorization.encryptedAuthData()).thenReturn("encryptedData");

    job = Mockito.mock(PortabilityJob.class);
    Mockito.when(job.jobAuthorization()).thenReturn(jobAuthorization);

    jobStore = Mockito.mock(JobStore.class);
    Mockito.when(jobStore.findJob(eq(jobId))).thenReturn(job);

    AuthDataPair pair = Mockito.mock(AuthDataPair.class);
    Mockito.when(pair.getExportAuthData()).thenReturn("");
    Mockito.when(pair.getImportAuthData()).thenReturn("");

    decryptionService = Mockito.mock(AuthDataDecryptService.class);
    Mockito.when(decryptionService.decrypt(anyString(), any(byte[].class))).thenReturn(pair);
    Mockito.when(decryptionService.canHandle(eq(encryptionScheme))).thenReturn(true);

    objectMapper = Mockito.mock(ObjectMapper.class);
    Mockito.when(objectMapper.readValue(anyString(), eq(AuthData.class)))
      .thenReturn(exportAuthData, importAuthData);

    processor = Mockito.spy(
      new TestJobProcessor(jobStore,
        copier,
        objectMapper,
        decryptionService,
        Boolean.TRUE,
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

  @Test
  public void processJobCopiesSuccessfullyWithTransferSignalDisabled() throws CopyException,
    IOException, RetryException {
    processor = Mockito.spy(
      new TestJobProcessor(jobStore,
        copier,
        objectMapper,
        decryptionService,
        Boolean.FALSE,
        importSignalHandlerProvider,
        exportSignalHandlerProvider));

    JobMetadata.init(
      jobId,
      "".getBytes(),
      DataVertical.BLOBS,
      "",
      "",
      Stopwatch.createUnstarted());

    assertTrue(JobMetadata.getRecurringJobId().isEmpty());

    Mockito.doThrow(new CopyException("error", new Exception())).when(copier)
      .copy(importAuthData, exportAuthData, jobId, Optional.of(exportInfo));
    processor.processJob();
    Mockito.verify(copier).getErrors(jobId);

    Mockito.verify(importSignalHandlerProvider, Mockito.never()).get();
    Mockito.verify(importSignalHandler, Mockito.never())
      .sendSignal(any(SignalRequest.class), eq(importAuthData), any(Monitor.class));

    Mockito.verify(exportSignalHandlerProvider, Mockito.never()).get();
    Mockito.verify(exportSignalHandler, Mockito.never())
      .sendSignal(any(SignalRequest.class), eq(exportAuthData), any(Monitor.class));
  }

  @Test
  public void testInitJobMetadataWithRecurringId() throws CopyException,
    IOException, RetryException {
    processor = Mockito.spy(
      new TestJobProcessor(jobStore,
        copier,
        objectMapper,
        decryptionService,
        Boolean.FALSE,
        importSignalHandlerProvider,
        exportSignalHandlerProvider));

    JobMetadata.init(
      jobId,
      recurringJobId,
      "".getBytes(),
      DataVertical.BLOBS,
      "",
      "",
      Stopwatch.createUnstarted());

    assertEquals(jobId, JobMetadata.getJobId());
    assertEquals(recurringJobId, JobMetadata.getRecurringJobId().get());
  }

  @Test
  public void runtimeExceptionFromAHandlerAtStart_failsTheJob_afterBothHandlersGetStart()
      throws Exception {
    initJobMetadata();
    Mockito.doThrow(new IllegalStateException("handler bug"))
        .when(exportSignalHandler)
        .sendSignal(withState(State.STARTED), any(), any());

    processor.processJob();

    Mockito.verify(copier, Mockito.never()).copy(any(), any(), any(), any());
    Mockito.verify(importSignalHandler).sendSignal(withState(State.STARTED), any(), any());
    Mockito.verify(importSignalHandler).sendSignal(withState(State.ENDED), any(), any());
    Mockito.verify(exportSignalHandler).sendSignal(withState(State.ENDED), any(), any());
  }

  @Test
  public void runtimeExceptionFromAHandlerAtEnd_stillFinishesTheJob() throws Exception {
    initJobMetadata();
    Mockito.doThrow(new IllegalStateException("handler bug"))
        .when(exportSignalHandler)
        .sendSignal(withState(State.ENDED), any(), any());

    processor.processJob();

    Mockito.verify(importSignalHandler).sendSignal(withState(State.ENDED), any(), any());
    // JobMetadata.reset() is the last step of finally.
    assertFalse(JobMetadata.isInitialized());
  }

  @Test
  public void telemetryOn_optedInHandlerGetsStartAndEndTelemetry_otherHandlerDoesNot()
      throws Exception {
    initJobMetadata();
    TelemetryCounters counters = Mockito.mock(TelemetryCounters.class);
    Mockito.when(counters.itemsFetched()).thenReturn(3L);
    Mockito.when(copier.getTelemetryCounters()).thenReturn(Optional.of(counters));
    Mockito.when(importSignalHandler.supportsTelemetry()).thenReturn(true);
    processor =
        new TestJobProcessor(
            jobStore,
            copier,
            objectMapper,
            decryptionService,
            new TransferSignals(
                exportSignalHandlerProvider,
                importSignalHandlerProvider,
                true,
                true,
                Duration.ofHours(1),
                Optional.of(Duration.ofDays(7)),
                TransferSignals.CANCEL_END_TIMEOUT,
                Mockito.mock(Monitor.class),
                Clock.systemUTC()));

    processor.processJob();

    ArgumentCaptor<SignalRequest> toImport = ArgumentCaptor.forClass(SignalRequest.class);
    Mockito.verify(importSignalHandler, Mockito.times(2))
        .sendSignal(toImport.capture(), any(), any());
    assertNotNull(toImport.getAllValues().get(0).startTelemetry());
    SignalRequest end = toImport.getAllValues().get(1);
    assertEquals(EndReason.SUCCESSFULLY_COMPLETED, end.jobStatus().endReason());
    assertEquals(Long.valueOf(3), end.endTelemetry().totalItemsFetched());

    ArgumentCaptor<SignalRequest> toExport = ArgumentCaptor.forClass(SignalRequest.class);
    Mockito.verify(exportSignalHandler, Mockito.times(2))
        .sendSignal(toExport.capture(), any(), any());
    for (SignalRequest request : toExport.getAllValues()) {
      assertNull(request.startTelemetry());
      assertNull(request.endTelemetry());
    }
  }

  @Test
  public void partiallyCompletedJob_watcherPollsWhenStoreIsMarkedErrored_sendsPartiallyCompleted()
      throws Exception {
    initJobMetadata();
    TelemetryCounters counters = Mockito.mock(TelemetryCounters.class);
    Mockito.when(copier.getTelemetryCounters()).thenReturn(Optional.of(counters));
    Mockito.when(copier.getErrors(jobId))
        .thenReturn(
            List.of(ErrorDetail.builder().setId("item-1").setTitle("t").setException("e").build()));
    Mockito.when(importSignalHandler.supportsTelemetry()).thenReturn(true);
    TransferSignals signals =
        new TransferSignals(
            exportSignalHandlerProvider,
            importSignalHandlerProvider,
            true,
            true,
            Duration.ofHours(1),
            Optional.empty(),
            TransferSignals.CANCEL_END_TIMEOUT,
            Mockito.mock(Monitor.class),
            Clock.systemUTC());
    JobHooks hooks = Mockito.mock(JobHooks.class);
    AtomicBoolean watcherDoneWhenHooksRan = new AtomicBoolean();
    CompletableFuture<Void>[] watcher = new CompletableFuture[1];
    Mockito.doAnswer(
            invocation -> {
              // The watcher polls ERROR right after markJobAsFinished writes it to JobStore.
              watcher[0] =
                  CompletableFuture.runAsync(() -> signals.sendEndOnCancel(EndReason.ERRORED));
              Thread.sleep(100);
              return null;
            })
        .when(jobStore)
        .markJobAsFinished(jobId, PortabilityJob.State.ERROR);
    Mockito.doAnswer(
            invocation -> {
              watcherDoneWhenHooksRan.set(watcher[0].isDone());
              return null;
            })
        .when(hooks)
        .jobFinished(jobId, false);
    JobProcessor jobProcessor =
        new JobProcessor(
            jobStore,
            hooks,
            objectMapper,
            copier,
            decryptionService,
            signals,
            Mockito.mock(Monitor.class),
            Mockito.mock(DtpInternalMetricRecorder.class));

    jobProcessor.processJob();
    watcher[0].get(10, TimeUnit.SECONDS);

    assertFalse(watcherDoneWhenHooksRan.get());
    ArgumentCaptor<SignalRequest> toImport = ArgumentCaptor.forClass(SignalRequest.class);
    Mockito.verify(importSignalHandler, Mockito.times(2))
        .sendSignal(toImport.capture(), any(), any());
    SignalRequest end = toImport.getAllValues().get(1);
    assertEquals(EndReason.PARTIALLY_COMPLETED, end.jobStatus().endReason());
    assertNotNull(end.endTelemetry());
  }

  @Test
  public void hooksThrow_optedInHandlerStillGetsTheEnd_andTheCancelWatcherIsNotLeftWaiting()
      throws Exception {
    initJobMetadata();
    TelemetryCounters counters = Mockito.mock(TelemetryCounters.class);
    Mockito.when(copier.getTelemetryCounters()).thenReturn(Optional.of(counters));
    Mockito.when(importSignalHandler.supportsTelemetry()).thenReturn(true);
    TransferSignals signals = transferSignals(true);
    IllegalStateException failure = new IllegalStateException("hook bug");
    AtomicReference<CompletableFuture<Void>> watcher = new AtomicReference<>();
    JobHooks hooks = Mockito.mock(JobHooks.class);
    Mockito.doAnswer(
            invocation -> {
              // The user cancels while the hooks run, so the cancel watcher waits for the job's
              // End.
              watcher.set(
                  CompletableFuture.runAsync(
                      () -> signals.sendEndOnCancel(EndReason.USER_CANCELLED)));
              Thread.sleep(100);
              throw failure;
            })
        .when(hooks)
        .jobFinished(jobId, true);
    DtpInternalMetricRecorder metrics = Mockito.mock(DtpInternalMetricRecorder.class);
    JobProcessor jobProcessor =
        new JobProcessor(
            jobStore,
            hooks,
            objectMapper,
            copier,
            decryptionService,
            signals,
            Mockito.mock(Monitor.class),
            metrics);

    assertSame(failure, assertThrows(IllegalStateException.class, jobProcessor::processJob));
    watcher.get().get(10, TimeUnit.SECONDS);

    ArgumentCaptor<SignalRequest> toImport = ArgumentCaptor.forClass(SignalRequest.class);
    Mockito.verify(importSignalHandler, Mockito.times(2))
        .sendSignal(toImport.capture(), any(), any());
    SignalRequest end = toImport.getAllValues().get(1);
    assertEquals(EndReason.SUCCESSFULLY_COMPLETED, end.jobStatus().endReason());
    assertNotNull(end.endTelemetry());
    // As before telemetry, the other handler gets no End, and the rest of finally is skipped.
    Mockito.verify(exportSignalHandler, Mockito.never())
        .sendSignal(withState(State.ENDED), any(), any());
    Mockito.verify(metrics, Mockito.never())
        .finishedJob(any(), any(), any(), Mockito.anyBoolean(), any());
    assertTrue(JobMetadata.isInitialized());
  }

  @Test
  public void hooksThrow_withoutTelemetry_noHandlerGetsEnd_asBefore() throws Exception {
    initJobMetadata();
    JobHooks hooks = Mockito.mock(JobHooks.class);
    Mockito.doThrow(new IllegalStateException("hook bug")).when(hooks).jobFinished(jobId, true);
    JobProcessor jobProcessor =
        new JobProcessor(
            jobStore,
            hooks,
            objectMapper,
            copier,
            decryptionService,
            transferSignals(false),
            Mockito.mock(Monitor.class),
            Mockito.mock(DtpInternalMetricRecorder.class));

    assertThrows(IllegalStateException.class, jobProcessor::processJob);

    Mockito.verify(exportSignalHandler).sendSignal(withState(State.STARTED), any(), any());
    Mockito.verify(importSignalHandler).sendSignal(withState(State.STARTED), any(), any());
    Mockito.verify(exportSignalHandler, Mockito.never())
        .sendSignal(withState(State.ENDED), any(), any());
    Mockito.verify(importSignalHandler, Mockito.never())
        .sendSignal(withState(State.ENDED), any(), any());
  }

  private TransferSignals transferSignals(boolean telemetryEnabled) {
    return new TransferSignals(
        exportSignalHandlerProvider,
        importSignalHandlerProvider,
        true,
        telemetryEnabled,
        Duration.ofHours(1),
        Optional.empty(),
        TransferSignals.CANCEL_END_TIMEOUT,
        Mockito.mock(Monitor.class),
        Clock.systemUTC());
  }

  private void initJobMetadata() {
    JobMetadata.init(
        jobId, "".getBytes(), DataVertical.BLOBS, "", "", Stopwatch.createUnstarted());
  }

  private static SignalRequest withState(State state) {
    return argThat(request -> request != null && request.jobStatus().state() == state);
  }
}
