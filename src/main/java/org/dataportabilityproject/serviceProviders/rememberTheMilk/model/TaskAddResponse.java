package org.dataportabilityproject.serviceProviders.rememberTheMilk.model;

import com.google.api.client.util.Key;

public class TaskAddResponse extends RememberTheMilkResponse {
    @Key("list")
    public TaskList taskList;
}
