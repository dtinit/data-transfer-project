package org.dataportabilityproject.datatransfer.types.job;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dataportabilityproject.datatransfer.types.EntityType;

import java.time.LocalDateTime;

/**
 * A job that will fulfill a transfer request.
 *
 * TODO: Consider having the concepts of an data "owner".
 */
public class PortabilityJob extends EntityType {

    /**
     * The job states.
     */
    public enum State {
        NEW, COMPLETE, ERROR
    }

    @JsonProperty
    private State state = State.NEW;

    @JsonProperty
    private String source;        // REVIEW: corresponds to the import service

    @JsonProperty
    private String destination;   // REVIEW: corresponds to the export service

    @JsonProperty
    private String transferDataType;   // REVIEW: replace old PortableDataType since the latter is an enum and not extensible?

    @JsonProperty
    private LocalDateTime createdTimestamp; // ISO 8601 timestamp

    @JsonProperty
    private LocalDateTime lastUpdateTimestamp; // ISO 8601 timestamp

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getTransferDataType() {
        return transferDataType;
    }

    public void setTransferDataType(String transferDataType) {
        this.transferDataType = transferDataType;
    }

    public LocalDateTime getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(LocalDateTime createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public LocalDateTime getLastUpdateTimestamp() {
        return lastUpdateTimestamp;
    }

    public void setLastUpdateTimestamp(LocalDateTime lastUpdateTimestamp) {
        this.lastUpdateTimestamp = lastUpdateTimestamp;
    }
}
