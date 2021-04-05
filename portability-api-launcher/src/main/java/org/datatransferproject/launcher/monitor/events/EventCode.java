package org.datatransferproject.launcher.monitor.events;

public class EventCode {
  public static final EventCode API_GENERATED_AUTH_DATA = new EventCode("API_GENERATED_AUTH_DATA");
  public static final EventCode API_GOT_RESERVED_WORKER = new EventCode("API_GOT_RESERVED_WORKER");
  public static final EventCode API_GOT_TRANSFER_JOB = new EventCode("API_GOT_TRANSFER_JOB");
  public static final EventCode API_JOB_CREATED = new EventCode("API_JOB_CREATED");
  public static final EventCode API_JOB_CREDS_AVAILABLE = new EventCode("API_JOB_CREDS_AVAILABLE");
  public static final EventCode API_JOB_CREDS_STORED = new EventCode("API_JOB_CREDS_STORED");

  public static final EventCode WORKER_CREDS_STORED = new EventCode("WORKER_CREDS_STORED");
  public static final EventCode WORKER_CREDS_TIMEOUT = new EventCode("WORKER_CREDS_TIMEOUT");
  public static final EventCode WORKER_JOB_CANCELED = new EventCode("WORKER_JOB_CANCELED");
  public static final EventCode WORKER_JOB_ERRORED = new EventCode("WORKER_JOB_ERRORED");
  public static final EventCode WORKER_JOB_FINISHED = new EventCode("WORKER_JOB_FINISHED");
  public static final EventCode WORKER_JOB_STARTED = new EventCode("WORKER_JOB_STARTED");

  public static final EventCode WATCHING_SERVICE_JOB_ERRORED = new EventCode("WATCHING_SERVICE_JOB_ERRORED");
  public static final EventCode WATCHING_SERVICE_JOB_PREEMPTED = new EventCode("WATCHING_SERVICE_JOB_PREEMPTED");

  public static final EventCode COPIER_FINISHED_EXPORT = new EventCode("COPIER_FINISHED_EXPORT");
  public static final EventCode COPIER_FINISHED_IMPORT = new EventCode("COPIER_FINISHED_IMPORT");
  public static final EventCode COPIER_STARTED_EXPORT = new EventCode("COPIER_STARTED_EXPORT");
  public static final EventCode COPIER_STARTED_IMPORT = new EventCode("COPIER_STARTED_IMPORT");

  private final String code;

  protected EventCode(String code) {
    this.code = code;
  }

  @Override
  public String toString() {
    return code;
  }
}
