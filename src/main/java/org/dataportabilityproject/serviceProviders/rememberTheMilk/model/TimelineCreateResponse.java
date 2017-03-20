package org.dataportabilityproject.serviceProviders.rememberTheMilk.model;

import com.google.api.client.util.Key;

public class TimelineCreateResponse extends RememberTheMilkResponse {
    @Key("timeline")
    public String timeline;
}
