package org.datatransferproject.transfer.photobucket.data.response.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserStatsResponse {
    @JsonProperty("username")
    String username;

    @JsonProperty("totalUserUsedSpace")
    long totalUserUsedSpace;

    @JsonProperty("totalImagesCount")
    long totalImagesCount;

    @JsonProperty("maxSpace")
    long maxSpace;

    @JsonProperty("planId")
    String planId;

}
