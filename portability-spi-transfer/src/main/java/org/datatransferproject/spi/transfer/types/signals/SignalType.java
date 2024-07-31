package org.datatransferproject.spi.transfer.types.signals;

/**
 * A list of States to represent the life cycle of a transfer Job. See the list of monitoring Event
 * codes ({@code org.datatransferproject.launcher.monitor.events.EventCode}) that represents more
 * granular states + reasons.
 */
public enum SignalType {
    JOB_BEGIN,
    JOB_ERRORED,
    JOB_COMPLETED
}
