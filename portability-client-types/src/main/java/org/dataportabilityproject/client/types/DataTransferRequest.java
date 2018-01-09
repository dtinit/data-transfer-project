package org.dataportabilityproject.client.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A request to initiate a data transfer.
 */
public class DataTransferRequest {

    private String contentType;   // REVIEW: replace old PortableDataType since the latter is an enum and not exctensible?

    @JsonCreator
    public DataTransferRequest(@JsonProperty(value = "contentType", required = true) String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }
}
