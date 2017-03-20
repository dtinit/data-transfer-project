package org.dataportabilityproject.serviceProviders.rememberTheMilk.model;

import com.google.api.client.util.Key;

public class ListAddResponse extends RememberTheMilkResponse {
    @Key("list")
    public ListInfo listInfo;
}
