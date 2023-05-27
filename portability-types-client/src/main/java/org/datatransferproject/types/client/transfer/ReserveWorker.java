package org.datatransferproject.types.client.transfer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

/**
 * Request to reserve a worker to process a transfer job.
 */
public class ReserveWorker {

    private final String id;

    @JsonCreator
    public ReserveWorker(@JsonProperty(value = "id", required = true) String id) {
        this.id = id;
    }

    @ApiModelProperty(value = "The id of the job for which we are requesting a worker", dataType = "string", required = true)
    public String getId() {
        return id;
    }
}
