package org.dataportabilityproject.client.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * A transfer operation in the system.
 */
@ApiModel(description = "A transfer operation in the system")
public class DataTransfer {

    @ApiModel
    public enum Status {
        INPROCESS, COMPLETE, ERROR
    }

    private Status status;

    @JsonCreator
    public DataTransfer(@JsonProperty(value = "status", required = true) Status status) {
        this.status = status;
    }

    @ApiModelProperty
    public Status getStatus() {
        return status;
    }
}
