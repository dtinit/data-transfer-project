package org.dataportabilityproject.client.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * A request to initiate a data transfer operation.
 */
@ApiModel(description = "A request to initiate a data transfer operation")
public class DataTransferRequest {

    private String source;        // REVIEW: corresponds to the import service
    private String destination;   // REVIEW: corresponds to the import service
    private String contentType;   // REVIEW: replace old PortableDataType since the latter is an enum and not exctensible?

    @JsonCreator
    public DataTransferRequest(
            @JsonProperty(value = "source", required = true) String source,
            @JsonProperty(value = "destination", required = true) String destination,
            @JsonProperty(value = "contentType", required = true) String contentType) {
        this.source = source;
        this.destination = destination;
        this.contentType = contentType;
    }

    @ApiModelProperty(value = "The service to transfer data from", dataType = "string", required = true)
    public String getSource() {
        return source;
    }

    @ApiModelProperty(value = "The service to transfer data to", dataType = "string", required = true)
    public String getDestination() {
        return destination;
    }

    @ApiModelProperty(value = "The type of data to transfer", dataType = "string", required = true)
    public String getContentType() {
        return contentType;
    }

}
