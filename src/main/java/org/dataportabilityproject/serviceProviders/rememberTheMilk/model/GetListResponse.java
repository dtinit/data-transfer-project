package org.dataportabilityproject.serviceProviders.rememberTheMilk.model;

import com.google.api.client.util.Key;


public class GetListResponse extends RememberTheMilkResponse {
    @Key("tasks")
    public Tasks tasks;

    @Override
    public String toString() {
        return null == tasks ? stat : tasks.toString();
    }
}
