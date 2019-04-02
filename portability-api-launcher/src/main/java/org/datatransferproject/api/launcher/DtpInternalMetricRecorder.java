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
package org.datatransferproject.api.launcher;

import java.time.Duration;

public interface DtpInternalMetricRecorder {
  // Metrics related to DTP internals
  void startedJob(String dataType, String exportService, String importService);

  void exportPageAttemptFinished(String dataType, String service, boolean success, Duration duration);
  void exportPageFinished(String dataType, String service, boolean success, Duration duration);
  void exportFinished(String dataType, String service, boolean success, Duration duration);

  void importPageAttemptFinished(String dataType, String service, boolean success, Duration duration);
  void importPageFinished(String dataType, String service, boolean success, Duration duration);
  void importFinished(String dataType, String service, boolean success, Duration duration);

  void finishedJob(String dataType, String exportService, String importService);

  // Metrics from {@link MetricRecorder}
  void recordGenericMetric(String dataType, String service, String tag);
  void recordGenericMetric(String dataType, String service, String tag, boolean bool);
  void recordGenericMetric(String dataType, String service, String tag, Duration duration);
  void recordGenericMetric(String dataType, String service, String tag, int value);
}
