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

    private String contentType;   // REVIEW: replace old PortableDataType since the latter is an enum and not exctensible?

    @JsonCreator
    public DataTransferRequest(@JsonProperty(value = "contentType", required = true) String contentType) {
        this.contentType = contentType;
    }

    @ApiModelProperty(dataType = "string", required = true)
    public String getContentType() {
        return contentType;
    }
}
