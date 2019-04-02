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
package org.datatransferproject.launcher.metrics;

import org.datatransferproject.api.launcher.DtpInternalMetricRecorder;

import java.time.Duration;

/** Forwards monitor events to a set of delegates. */
public class MultiplexMetricRecorder implements DtpInternalMetricRecorder {
  private DtpInternalMetricRecorder[] delegates;

  public MultiplexMetricRecorder(DtpInternalMetricRecorder... delegates) {
    this.delegates = delegates != null ? delegates : new DtpInternalMetricRecorder[0];
  }

  @Override
  public void startedJob(String dataType, String exportService, String importService) {
    for (DtpInternalMetricRecorder metricRecorder : delegates) {
      metricRecorder.startedJob(dataType, exportService, importService);
    }
  }

  @Override
  public void exportPageAttemptFinished(String dataType, String service, boolean success, Duration duration) {
    for (DtpInternalMetricRecorder metricRecorder : delegates) {
      metricRecorder.exportPageAttemptFinished(dataType, service, success, duration);
    }
  }

  @Override
  public void exportPageFinished(String dataType, String service, boolean success, Duration duration) {
    for (DtpInternalMetricRecorder metricRecorder : delegates) {
      metricRecorder.exportPageFinished(dataType, service, success, duration);
    }
  }

  @Override
  public void exportFinished(String dataType, String service, boolean success, Duration duration) {
    for (DtpInternalMetricRecorder metricRecorder : delegates) {
      metricRecorder.exportPageFinished(dataType, service, success, duration);
    }
  }

  @Override
  public void importPageAttemptFinished(String dataType, String service, boolean success, Duration duration) {
    for (DtpInternalMetricRecorder metricRecorder : delegates) {
      metricRecorder.importPageAttemptFinished(dataType, service, success, duration);
    }
  }

  @Override
  public void importPageFinished(String dataType, String service, boolean success, Duration duration) {
    for (DtpInternalMetricRecorder metricRecorder : delegates) {
      metricRecorder.importPageFinished(dataType, service, success, duration);
    }
  }

  @Override
  public void importFinished(String dataType, String service, boolean success, Duration duration) {
    for (DtpInternalMetricRecorder metricRecorder : delegates) {
      metricRecorder.importFinished(dataType, service, success, duration);
    }
  }

  @Override
  public void finishedJob(String dataType, String exportService, String importService) {
    for (DtpInternalMetricRecorder metricRecorder : delegates) {
      metricRecorder.finishedJob(dataType, exportService, importService);
    }
  }

  @Override
  public void recordGenericMetric(String dataType, String service, String tag) {
    for (DtpInternalMetricRecorder metricRecorder : delegates) {
      metricRecorder.recordGenericMetric(dataType, service, tag);
    }
  }

  @Override
  public void recordGenericMetric(String dataType, String service, String tag, boolean bool) {
    for (DtpInternalMetricRecorder metricRecorder : delegates) {
      metricRecorder.recordGenericMetric(dataType, service, tag, bool);
    }
  }

  @Override
  public void recordGenericMetric(String dataType, String service, String tag, Duration duration) {
    for (DtpInternalMetricRecorder metricRecorder : delegates) {
      metricRecorder.recordGenericMetric(dataType, service, tag, duration);
    }
  }

  @Override
  public void recordGenericMetric(String dataType, String service, String tag, int value) {
    for (DtpInternalMetricRecorder metricRecorder : delegates) {
      metricRecorder.recordGenericMetric(dataType, service, tag, value);
    }
  }
}
