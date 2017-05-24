package org.dataportabilityproject.serviceProviders.rememberTheMilk.model;

import com.google.api.client.util.Key;

/**
 * A generic response from the remember the milk service.
 */
public abstract class RememberTheMilkResponse {
    @Key("@stat")
    public String stat;

    @Key("err")
    public Error error;
}
