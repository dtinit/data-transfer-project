package org.datatransferproject.cloud.google;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.api.launcher.MonitorExtension;
import org.datatransferproject.launcher.monitor.ConsoleMonitor;
import org.datatransferproject.launcher.monitor.MultiplexMonitor;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkState;

/**
 * A {@link MonitorExtension} that provisions a {@link Monitor} that logs to
 * Google Stackdriver.
 */
public class StackdriverMonitorExtension implements MonitorExtension {
  Logging logging;

  @Override
  public Monitor getMonitor() {
    checkState(logging != null, "logging must be initialized first");
    // Include the console monitor for extra debugging.
    //return new ConsoleMonitor(ConsoleMonitor.Level.DEBUG);
    return new MultiplexMonitor(new StackdriverMonitor(
        this.logging,
        GoogleCloudUtils.getProjectId()),
        new ConsoleMonitor(ConsoleMonitor.Level.INFO));
  }

  @Override
  public void initialize() {
    try {
      this.logging = LoggingOptions
        .newBuilder()
        .setProjectId(GoogleCloudUtils.getProjectId())
        .setCredentials(GoogleCredentials.getApplicationDefault())
        .build()
        .getService();
    } catch (IOException e) {
      throw new RuntimeException("Couldn't initialize StackdriverMonitorExtension", e);
    }
  }
}
