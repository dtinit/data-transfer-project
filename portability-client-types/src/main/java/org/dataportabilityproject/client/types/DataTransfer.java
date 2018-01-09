package org.dataportabilityproject.client.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * A transfer operation in the system.
 */
@ApiModel(description = "A transfer operation in the system")
public class DataTransfer extends AbstractDataTransfer {

    @ApiModel
    public enum Status {
        INPROCESS, COMPLETE, ERROR
    }

    private Status status;

    @JsonCreator
    public DataTransfer(
            @JsonProperty(value = "source", required = true) String source,
            @JsonProperty(value = "destination", required = true) String destination,
            @JsonProperty(value = "contentType", required = true) String contentType,
            @JsonProperty(value = "status", required = true) Status status) {
        super(source, destination, contentType);
        this.status = status;
    }

    @ApiModelProperty
    public Status getStatus() {
        return status;
    }
}
