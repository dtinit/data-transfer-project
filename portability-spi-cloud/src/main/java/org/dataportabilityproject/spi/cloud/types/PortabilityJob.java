package org.dataportabilityproject.spi.cloud.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.dataportabilityproject.types.transfer.EntityType;

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

    @JsonProperty
    private JobAuthorization jobAuthorization; // Review: Job Authorization data

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

    public JobAuthorization getJobAuthorization() {
        return jobAuthorization;
    }

    /**
     * Sets and validates the {@link JobAuthorization} data associated with this job.
     */
    public void setJobAuthorization(JobAuthorization jobAuthorization) {
        switch(jobAuthorization.getState()) {
            case INITIAL:
                validateState(jobAuthorization, true, false, false, false, false);
                break;
            case PENDING_WORKER_ASSIGNMENT:
                validateState(jobAuthorization, true, false, false, false, false);
                break;
            case ASSIGNED_WITHOUT_AUTH_DATA:
                validateState(jobAuthorization, true, false, false, true, true);
                break;
            case ASSIGNED_WITH_AUTH_DATA:
                validateState(jobAuthorization, true, true, true, true, true);
                break;
        }
        this.jobAuthorization = jobAuthorization;
    }

    /**
     * Validate the correct data is present for each state transition.
     */
    private void validateState(JobAuthorization jobAuthorization,
        boolean sessionKeyExists,
        boolean sourceAuthDataExists,
        boolean destinationAuthDataExists,
        boolean publicKeyExists,
        boolean privateKeyExists) {
        Preconditions.checkState(
            Strings.isNullOrEmpty(jobAuthorization.getEncryptedSessionKey()) != sessionKeyExists);
        Preconditions
            .checkState(Strings.isNullOrEmpty(jobAuthorization.getEncryptedSourceAuthData())
                != sourceAuthDataExists);
        Preconditions.checkState(
            Strings.isNullOrEmpty(jobAuthorization.getEncryptedDestinationAuthData())
                != destinationAuthDataExists);
        Preconditions.checkState(
            Strings.isNullOrEmpty(jobAuthorization.getEncryptedPublicKey()) != publicKeyExists);
        Preconditions.checkState(
            Strings.isNullOrEmpty(jobAuthorization.getEncryptedPrivateKey()) != privateKeyExists);
    }
}
