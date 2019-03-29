package org.datatransferproject.api.launcher;

public interface JobAwareMonitor extends Monitor {

  /**
   * Sets the jobId for the monitor, this will be included with all log messages.
   */
  void setJobId(String jobId);
}
