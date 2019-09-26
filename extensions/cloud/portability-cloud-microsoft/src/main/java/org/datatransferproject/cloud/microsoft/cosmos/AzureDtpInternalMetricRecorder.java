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
package org.datatransferproject.cloud.microsoft.cosmos;

import static java.lang.String.format;

import java.time.Duration;
import org.datatransferproject.api.launcher.DtpInternalMetricRecorder;
import org.datatransferproject.api.launcher.Monitor;

/**
 * A placeholder {@link DtpInternalMetricRecorder} that simply logs metrics to the default monitor.
 * *
 */
class AzureDtpInternalMetricRecorder implements DtpInternalMetricRecorder {
  private final Monitor monitor;

  // TODO: Replace with a Azure Monitor based implementation.
  AzureDtpInternalMetricRecorder(Monitor monitor) {
    this.monitor = monitor;
  }

  @Override
  public void startedJob(String dataType, String exportService, String importService) {
    monitor.debug(
        () ->
            format(
                "Metric: StartedJob, data type: %s, from: %s, to: %s",
                dataType, exportService, importService));
  }

  @Override
  public void exportPageAttemptFinished(
      String dataType, String service, boolean success, Duration duration) {
    monitor.debug(
        () ->
            format(
                "Metric: exportPageAttemptFinished, data type: %s, service: %s, "
                    + "success: %s, duration: %s",
                dataType, service, success, duration));
  }

  @Override
  public void exportPageFinished(
      String dataType, String service, boolean success, Duration duration) {
    monitor.debug(
        () ->
            format(
                "Metric: exportPageFinished, data type: %s, service: %s, success: %s, duration: %s",
                dataType, service, success, duration));
  }

  @Override
  public void importPageAttemptFinished(
      String dataType, String service, boolean success, Duration duration) {
    monitor.debug(
        () ->
            format(
                "Metric: importPageAttemptFinished, data type: %s, service: %s,"
                    + "success: %s, duration: %s",
                dataType, service, success, duration));
  }

  @Override
  public void importPageFinished(
      String dataType, String service, boolean success, Duration duration) {
    monitor.debug(
        () ->
            format(
                "Metric: importPageFinished, data type: %s, service: %s, success: %s, duration: %s",
                dataType, service, success, duration));
  }

  @Override
  public void finishedJob(
      String dataType,
      String exportService,
      String importService,
      boolean success,
      Duration duration) {
    monitor.debug(
        () ->
            format(
                "Metric: finishedJob, data type: %s, from: %s, to: %s, success: %s, duration: %s",
                dataType, exportService, importService, success, duration));
  }

  @Override
  public void recordGenericMetric(String dataType, String service, String tag) {
    monitor.debug(
        () ->
            format("Metric: Generic, data type: %s, service: %s, tag: %s", dataType, service, tag));
  }

  @Override
  public void recordGenericMetric(String dataType, String service, String tag, boolean bool) {
    monitor.debug(
        () ->
            format(
                "Metric: Generic, data type: %s, service: %s, tag: %s, value: %s",
                dataType, service, tag, bool));
  }

  @Override
  public void recordGenericMetric(String dataType, String service, String tag, Duration duration) {
    monitor.debug(
        () ->
            format(
                "Metric: Generic, data type: %s, service: %s, tag: %s, duration: %s",
                dataType, service, tag, duration));
  }

  @Override
  public void recordGenericMetric(String dataType, String service, String tag, int value) {
    monitor.debug(
        () ->
            format(
                "Metric: Generic, data type: %s, service: %s, tag: %s, value: %s",
                dataType, service, tag, value));
  }
}
