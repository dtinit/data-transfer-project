package org.dataportabilityproject.client.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModelProperty;

/**
 * A request to initiate a data transfer operation.
 */
public abstract class AbstractDataTransfer {

    private String source;        // REVIEW: corresponds to the import service
    private String destination;   // REVIEW: corresponds to the export service
    private String contentType;   // REVIEW: replace old PortableDataType since the latter is an enum and not exctensible?

    @JsonCreator
    public AbstractDataTransfer(String source, String destination, String contentType) {
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
