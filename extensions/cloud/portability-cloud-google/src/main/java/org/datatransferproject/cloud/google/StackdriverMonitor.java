package org.datatransferproject.cloud.google;

import com.google.cloud.MonitoredResource;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.Payload;
import com.google.cloud.logging.Severity;
import com.google.common.base.Throwables;
import org.datatransferproject.api.launcher.JobAwareMonitor;
import org.datatransferproject.launcher.monitor.events.EventCode;

import java.net.InetAddress;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

class StackdriverMonitor implements JobAwareMonitor {

  private static final String LOG_NAME = "worker-instance-log";
  private final Logging logging;
  private final String projectId;
  private String jobId;

  public StackdriverMonitor(Logging logging, String projectId) {
    this.logging = logging;
    this.projectId = projectId;
  }

  @Override
  public void severe(Supplier<String> supplier, Object... data) {
    log(Severity.ERROR, supplier, data);
  }

  @Override
  public void info(Supplier<String> supplier, Object... data) {
    log(Severity.INFO, supplier, data);
  }

  @Override
  public void debug(Supplier<String> supplier, Object... data) {
    log(Severity.NOTICE, supplier, data);
  }

  private void log(Severity severity, Supplier<String> supplier, Object... data) {
    MonitoredResource.Builder resourceBuilder = MonitoredResource.newBuilder("generic_task")
        .addLabel("project_id", projectId)
        // This is slightly backwards as in GCP a job can have many tasks
        // but to line up with the DTP terminology around a job we'll use
        // GCP's job to line up with DTP's job.
        .addLabel("task_id", getHostName());

    if (null != jobId) {
      resourceBuilder.addLabel("job", jobId);
    }

    StringBuilder logMessage = new StringBuilder();
    logMessage.append(supplier.get());

    if (data != null) {
      for (Object datum : data) {
        if (datum instanceof Throwable) {
          logMessage.append(
              format("\n%s", Throwables.getStackTraceAsString(((Throwable) datum))));
        } else if (datum instanceof UUID) {
          logMessage.append(format("\nJobId: %s", ((UUID) datum)));
        } else if (datum instanceof EventCode) {
          logMessage.append(format("\nEventCode: %s", (EventCode) datum));
        } else if (datum != null) {
          logMessage.append(format("\n%s", datum));
        }
      }
    }

    LogEntry entry = LogEntry.newBuilder(Payload.StringPayload.of(logMessage.toString()))
        .setSeverity(severity)
        .setLogName(LOG_NAME)
        .setResource(resourceBuilder.build())
        .build();

    try {
      // Writes the log entry asynchronously
      logging.write(Collections.singleton(entry));
    } catch (Throwable t) {
      System.out.println("Problem logging: " + t.getMessage());
      t.printStackTrace(System.out);
    }
  }

  private static String getHostName() {
    try {
      return InetAddress.getLocalHost().getCanonicalHostName();
    } catch (Throwable t) {
      return "Unknown";
    }
  }

  @Override
  public void setJobId(String jobId) {
    checkState(this.jobId == null, "JobId can only be set once.");
    this.jobId = jobId;
    debug(() -> format("Set job id to: %s", jobId));
  }

  @Override
  public void flushLogs() {
    logging.flush();
  }
}
