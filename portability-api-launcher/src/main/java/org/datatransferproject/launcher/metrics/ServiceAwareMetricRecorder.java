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
package org.datatransferproject.launcher.metrics;

import org.datatransferproject.api.launcher.DtpInternalMetricRecorder;
import org.datatransferproject.api.launcher.MetricRecorder;

import java.time.Duration;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link MetricRecorder} that is aware of the service it is being used in
 * that forwards metric events on to a {@link DtpInternalMetricRecorder}.
 **/
public class ServiceAwareMetricRecorder implements MetricRecorder {
  private final String service;
  private final DtpInternalMetricRecorder metricRecorder;

  public ServiceAwareMetricRecorder(
      String service,
      DtpInternalMetricRecorder metricRecorder) {
    this.service = service;
    this.metricRecorder = checkNotNull(metricRecorder, "metricRecorder can't be null");
  }
  @Override
  public void recordMetric(String dataType, String tag) {
    metricRecorder.recordGenericMetric(dataType, service, tag);
  }

  @Override
  public void recordMetric(String dataType, String tag, boolean bool) {
    metricRecorder.recordGenericMetric(dataType, service, tag, bool);
  }

  @Override
  public void recordMetric(String dataType, String tag, Duration duration) {
    metricRecorder.recordGenericMetric(dataType, service, tag, duration);
  }

  @Override
  public void recordMetric(String dataType, String tag, int value) {
    metricRecorder.recordGenericMetric(dataType, service, tag, value);
  }
}
