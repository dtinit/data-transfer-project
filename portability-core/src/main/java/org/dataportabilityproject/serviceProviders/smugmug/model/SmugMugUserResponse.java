package org.dataportabilityproject.serviceProviders.smugmug.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Results from https://api.smugmug.com/api/v2!authuser
 */
public class SmugMugUserResponse {
    @JsonProperty("Url")
    private String uri;

    @JsonProperty("Locator")
    private String locator;

    @JsonProperty("LocatorType")
    private String locatorType;

    @JsonProperty("User")
    private SmugMugUser user;

    public String getUri() {
        return uri;
    }

    public String getLocator() {
        return locator;
    }

    public String getLocatorType() {
        return locatorType;
    }

    public SmugMugUser getUser() {
        return user;
    }
}
