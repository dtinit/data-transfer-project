package org.datatransferproject.launcher.metrics;

import org.datatransferproject.api.launcher.DtpInternalMetricRecorder;
import org.datatransferproject.api.launcher.Monitor;

import java.time.Duration;

public class LoggingDtpInternalMetricRecorder implements DtpInternalMetricRecorder {
  private final Monitor monitor;

  public LoggingDtpInternalMetricRecorder(Monitor monitor) {
    this.monitor = monitor;
  }
  @Override
  public void startedJob(String dataType, String exportService, String importService) {
    monitor.debug(() -> "Metric: StartedJob, data type: %s, from: %s, to: %s",
        dataType, exportService, importService);
  }

  @Override
  public void exportPageAttemptFinished(
      String dataType,
      String service,
      boolean success,
      Duration duration) {
    monitor.debug(
        () -> "Metric: exportPageAttemptFinished, data type: %s, service: %s, success: %s, duration: %s",
        dataType,
        service,
        success,
        duration);
  }

  @Override
  public void exportPageFinished(String dataType, String service, boolean success, Duration duration) {
    monitor.debug(
        () -> "Metric: exportPageFinished, data type: %s, service: %s, success: %s, duration: %s",
        dataType,
        service,
        success,
        duration);
  }

  @Override
  public void exportFinished(String dataType, String service, boolean success, Duration duration) {
    monitor.debug(
        () -> "Metric: exportFinished, data type: %s, service: %s, success: %s, duration: %s",
        dataType,
        service,
        success,
        duration);
  }

  @Override
  public void importPageAttemptFinished(String dataType, String service, boolean success, Duration duration) {
    monitor.debug(
        () -> "Metric: importPageAttemptFinished, data type: %s, service: %s, success: %s, duration: %s",
        dataType,
        service,
        success,
        duration);
  }

  @Override
  public void importPageFinished(String dataType, String service, boolean success, Duration duration) {
    monitor.debug(
        () -> "Metric: importPageFinished, data type: %s, service: %s, success: %s, duration: %s",
        dataType,
        service,
        success,
        duration);
  }

  @Override
  public void importFinished(String dataType, String service, boolean success, Duration duration) {
    monitor.debug(
        () -> "Metric: importFinished, data type: %s, service: %s, success: %s, duration: %s",
        dataType,
        service,
        success,
        duration);
  }

  @Override
  public void finishedJob(String dataType, String exportService, String importService) {
    monitor.debug(() -> "Metric: finishedJob, data type: %s, from: %s, to: %s",
        dataType, exportService, importService);
  }

  @Override
  public void recordGenericMetric(String dataType, String service, String tag) {
    monitor.debug(() -> "Metric: Generic, data type: %s, service: %s, tag: %s",
        dataType, service, tag);
  }

  @Override
  public void recordGenericMetric(String dataType, String service, String tag, boolean bool) {
    monitor.debug(() -> "Metric: Generic, data type: %s, service: %s, tag: %s, value: %s",
        dataType, service, tag, bool);
  }

  @Override
  public void recordGenericMetric(String dataType, String service, String tag, Duration duration) {
    monitor.debug(() -> "Metric: Generic, data type: %s, service: %s, tag: %s, durration: %s",
        dataType, service, tag, duration);
  }

  @Override
  public void recordGenericMetric(String dataType, String service, String tag, int value) {
    monitor.debug(() -> "Metric: Generic, data type: %s, service: %s, tag: %s, value: %s",
        dataType, service, tag, value);
  }
}
