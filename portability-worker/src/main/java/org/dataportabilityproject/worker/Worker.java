package org.dataportabilityproject.worker;

import com.google.inject.Inject;

/**
 * Polls and processes a {@code PortabilityJob}.
 */
final class Worker {
  private final JobPoller jobPoller;
  private final JobProcessor jobProcessor;

  @Inject
  Worker(JobPoller jobPoller, JobProcessor jobProcessor) {
    this.jobPoller = jobPoller;
    this.jobProcessor = jobProcessor;
  }

  void doWork() {
    jobPoller.pollJob();
    jobProcessor.processJob();
  }
}
