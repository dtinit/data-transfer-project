package org.dataportabilityproject.serviceProviders.rememberTheMilk.model;

import com.google.api.client.util.Key;


public class GetListsResponse extends RememberTheMilkResponse {
    @Key("lists")
    public ListInfoList listInfoList;
}
