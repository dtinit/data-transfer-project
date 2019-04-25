/*
 * Copyright 2018 The Data Transfer Project Authors.
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

import com.google.inject.Inject;

/** Polls and processes a {@code PortabilityJob}. */
final class Worker {
  private final JobPoller jobPoller;
  private final JobCancelWatchingService jobCancelWatchingService;
  private final JobProcessor jobProcessor;

  @Inject
  Worker(
      JobPoller jobPoller,
      JobCancelWatchingService jobCancelWatchingService,
      JobProcessor jobProcessor) {
    this.jobPoller = jobPoller;
    this.jobCancelWatchingService = jobCancelWatchingService;
    this.jobProcessor = jobProcessor;
  }

  void doWork() {
    jobPoller.pollJob();
    jobCancelWatchingService.startAsync();
    jobProcessor.processJob();
  }
}
