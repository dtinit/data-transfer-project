package org.datatransferproject.launcher.metrics;

import org.datatransferproject.api.launcher.DtpInternalMetricRecorder;
import org.datatransferproject.api.launcher.MetricRecorder;

import java.time.Duration;

public class ServiceAwareMetricRecorder implements MetricRecorder {
  private final String service;
  private final DtpInternalMetricRecorder metricRecorder;

  public ServiceAwareMetricRecorder(
      String service,
      DtpInternalMetricRecorder metricRecorder) {
    this.service = service;
    this.metricRecorder = metricRecorder;
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
