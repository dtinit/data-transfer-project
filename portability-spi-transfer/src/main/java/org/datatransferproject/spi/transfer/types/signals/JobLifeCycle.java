package org.datatransferproject.spi.transfer.types.signals;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import javax.annotation.Nullable;

import org.datatransferproject.spi.transfer.provider.SignalRequest;
import org.datatransferproject.spi.transfer.types.FailureReasons;

/**
 * A list of States to represent the life cycle of a transfer Job. See the list of monitoring Event
 * codes ({@code org.datatransferproject.launcher.monitor.events.EventCode}) that represents more
 * granular states + reasons.
 */
@AutoValue
@JsonDeserialize(builder = JobLifeCycle.Builder.class)
public abstract class JobLifeCycle {
  public static Builder builder() {
    return new org.datatransferproject.spi.transfer.types.signals.AutoValue_JobLifeCycle.Builder()
        .setState(State.UNKNOWN_STATE);
  }

  @JsonProperty("state")
  public abstract State state();

  @Nullable
  @JsonProperty("failureReason")
  public abstract FailureReasons failureReason();

  @Nullable
  @JsonProperty("endReason")
  public abstract EndReason endReason();

  public static JobLifeCycle JOB_STARTED() {
    return JobLifeCycle.builder()
        .setState(JobLifeCycle.State.STARTED)
        .build();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setState(JobLifeCycle.State state);
    public abstract Builder setFailureReason(FailureReasons failureReason);
    public abstract Builder setEndReason(JobLifeCycle.EndReason endReason);
    abstract JobLifeCycle autoBuild();

    public final JobLifeCycle build() {
      JobLifeCycle jobLifeCycle = autoBuild();
      if(jobLifeCycle.state().equals(State.ENDED)) {
        Preconditions.checkArgument(jobLifeCycle.endReason() != null, "End reason required when JobState is ENDED");
      }
      return jobLifeCycle;
    }
  }

  public enum State {
    STARTED,
    IN_PROGRESS,
    ENDED,
    UNKNOWN_STATE;
  }

  public enum EndReason {
    /**
     * Job currently in a paused state. (TODO: Zacsh & Sundeep - need to define this accurately.)
     * Intended to resume the Job from where it was left off.
     */
    PAUSED,

    /**
     * Less than 100% of the Data that Exporter could export was imported into Destination.
     * No failures observed so far in the process of transferring the data.
     * There may or may not have any associated Failure Reason.
     */
    INTERRUPTED,

    /**
     * 100% of the Data that Exporter could export was imported into Destination.
     * No failures observed in the process of transferring the data.
     */
    SUCCESSFULLY_COMPLETED,

    /**
     * Less than 100% of the Data that Exporter could export was imported into Destination.
     * Some failures observed in the process of transferring the data.
     * There may or may not have any associated Failure Reason.
     */
    PARTIALLY_COMPLETED,

    /**
     * 0% of the Data that Exporter could export was imported into Destination.
     * Failures observed in the process of transferring the data.
     * There may or may not have any associated Failure Reason.
     */
    ERRORED,
    ;
  }
}
