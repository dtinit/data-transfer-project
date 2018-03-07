package org.dataportabilityproject.worker;

import com.google.inject.Inject;

/**
 * Polls a {@code PortabilityJob} for this worker to process.
 *
 * <p>Lightweight wrapper around an {@code AbstractScheduledService} so as to not expose its
 * implementation details.
 */
final class JobPoller {
  private final JobPollingService jobPollingService;

  @Inject
  JobPoller(JobPollingService jobPollingService) {
    this.jobPollingService = jobPollingService;
  }

  void pollJob() {
    jobPollingService.startAsync();
    jobPollingService.awaitTerminated();
  }
}
