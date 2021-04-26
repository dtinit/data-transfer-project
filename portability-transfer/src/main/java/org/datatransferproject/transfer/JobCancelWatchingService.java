/*
 * Copyright 2019 The Data Transfer Project Authors.
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

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import org.datatransferproject.api.launcher.DtpInternalMetricRecorder;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.launcher.monitor.events.EventCode;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.types.PortabilityJob;

/** A service that polls storage to see if a job is canceled, if it is it kills the binary. */
class JobCancelWatchingService extends AbstractScheduledService {
  private final JobStore store;
  private final Scheduler scheduler;
  private final Monitor monitor;
  private final DtpInternalMetricRecorder dtpInternalMetricRecorder;

  @Inject
  JobCancelWatchingService(
          JobStore store, @Annotations.CancelScheduler Scheduler scheduler,
          Monitor monitor, DtpInternalMetricRecorder dtpInternalMetricRecorder) {
    this.store = store;
    this.scheduler = scheduler;
    this.monitor = monitor;
    this.dtpInternalMetricRecorder = dtpInternalMetricRecorder;
  }

  @Override
  protected void runOneIteration() {
    if (!JobMetadata.isInitialized()) {
      return;
    }
    monitor.debug(() -> "polling for job to check cancellation");
    PortabilityJob currentJob = store.findJob(JobMetadata.getJobId());
    switch (currentJob.state()) {
      case CANCELED:
        monitor.info(
            () -> String.format("Job %s is canceled", JobMetadata.getJobId()),
            EventCode.WORKER_JOB_CANCELED);
        dtpInternalMetricRecorder.cancelledJob(
            JobMetadata.getDataType(),
            JobMetadata.getExportService(),
            JobMetadata.getImportService(),
            JobMetadata.getStopWatch().elapsed());
        monitor.flushLogs();
        System.exit(0);
        break;
      case ERROR:
        monitor.severe(
            () -> String.format("Job %s is errored", JobMetadata.getJobId()),
            EventCode.WATCHING_SERVICE_JOB_ERRORED);
        recordGeneralMetric(PortabilityJob.State.ERROR.toString());
        monitor.flushLogs();
        System.exit(0);
        break;
      case PREEMPTED:
        monitor.info(
            () -> String.format("Job %s is preempted", JobMetadata.getJobId()),
            EventCode.WATCHING_SERVICE_JOB_PREEMPTED);
        recordGeneralMetric(PortabilityJob.State.PREEMPTED.toString());
        monitor.flushLogs();
        System.exit(0);
        break;
      default:
        monitor.debug(
            () ->
                String.format(
                    "Job %s is not canceled or errored or preempted", JobMetadata.getJobId()));
    }
  }

  @Override
  protected Scheduler scheduler() {
    return scheduler;
  }

    private void recordGeneralMetric(String jobState) {
      dtpInternalMetricRecorder.recordGenericMetric(
              JobMetadata.getDataType(),
              JobMetadata.getExportService(),
              jobState,
              JobMetadata.getStopWatch().elapsed());
    }
}
