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
 */
package org.datatransferproject.transfer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.AbstractScheduledService.Scheduler;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.datatransferproject.api.launcher.DtpInternalMetricRecorder;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle.EndReason;
import org.datatransferproject.types.common.models.DataVertical;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class JobCancelWatchingServiceTest {

  private final UUID jobId = UUID.randomUUID();
  private final JobStore store = mock(JobStore.class);
  private final TransferSignals signals = mock(TransferSignals.class);
  private final Runnable exit = mock(Runnable.class);
  private JobCancelWatchingService service;

  @BeforeEach
  void setUp() {
    JobMetadata.reset();
    JobMetadata.init(
        jobId, new byte[0], DataVertical.PHOTOS, "Apple", "Google", Stopwatch.createStarted());
    service =
        new JobCancelWatchingService(
            store,
            Scheduler.newFixedDelaySchedule(0, 1, TimeUnit.HOURS),
            mock(Monitor.class),
            mock(DtpInternalMetricRecorder.class),
            signals,
            exit);
  }

  @AfterEach
  void tearDown() {
    JobMetadata.reset();
  }

  @Test
  void canceledJob_sendsUserCancelledEnd_thenExits() {
    jobIs(PortabilityJob.State.CANCELED);

    service.runOneIteration();

    InOrder order = inOrder(store, signals, exit);
    order.verify(store).abandonJob(jobId, PortabilityJob.State.CANCELED.toString());
    order.verify(signals).sendEndOnCancel(EndReason.USER_CANCELLED);
    order.verify(exit).run();
  }

  @Test
  void erroredJob_sendsErroredEnd_thenExits() {
    jobIs(PortabilityJob.State.ERROR);

    service.runOneIteration();

    InOrder order = inOrder(store, signals, exit);
    order.verify(store).abandonJob(jobId, PortabilityJob.State.ERROR.toString());
    order.verify(signals).sendEndOnCancel(EndReason.ERRORED);
    order.verify(exit).run();
  }

  @Test
  void preemptedJob_sendsNoEnd_andExits() {
    jobIs(PortabilityJob.State.PREEMPTED);

    service.runOneIteration();

    verify(signals, never()).sendEndOnCancel(any());
    verify(exit).run();
  }

  @Test
  void canceledJob_exitsEvenIfSendingEndThrows() {
    jobIs(PortabilityJob.State.CANCELED);
    doThrow(new IllegalStateException("bug")).when(signals).sendEndOnCancel(any());

    assertThrows(IllegalStateException.class, () -> service.runOneIteration());

    verify(exit).run();
  }

  @Test
  void runningJob_sendsNothing_andKeepsRunning() {
    jobIs(PortabilityJob.State.IN_PROGRESS);

    service.runOneIteration();

    verifyNoInteractions(signals, exit);
  }

  @Test
  void completedJob_sendsNothing_andKeepsRunning() {
    // The job's own finally wrote COMPLETE and sends End itself.
    jobIs(PortabilityJob.State.COMPLETE);

    service.runOneIteration();

    verifyNoInteractions(signals, exit);
  }

  @Test
  void beforeAJobIsClaimed_doesNothing() {
    JobMetadata.reset();

    service.runOneIteration();

    verifyNoInteractions(store, signals, exit);
  }

  private void jobIs(PortabilityJob.State state) {
    PortabilityJob job = mock(PortabilityJob.class);
    when(job.state()).thenReturn(state);
    when(store.findJob(jobId)).thenReturn(job);
  }
}
