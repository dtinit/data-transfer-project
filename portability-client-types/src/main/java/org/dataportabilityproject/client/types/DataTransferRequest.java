package org.dataportabilityproject.client.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;

/**
 * A request to initiate a data transfer operation.
 */
@ApiModel(description = "A request to initiate a data transfer operation")
public class DataTransferRequest extends AbstractDataTransfer {

    @JsonCreator
    public DataTransferRequest(
            @JsonProperty(value = "source", required = true) String source,
            @JsonProperty(value = "destination", required = true) String destination,
            @JsonProperty(value = "contentType", required = true) String contentType) {
        super(source, destination, contentType);
    }


}
