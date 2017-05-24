package org.dataportabilityproject.serviceProviders.rememberTheMilk.model;

import com.google.api.client.util.Key;

import java.util.List;

/**
 * Resonse to rtm.lists.getList. See:
 * https://www.rememberthemilk.com/services/api/methods/rtm.lists.getList.rtm
 */
public class ListInfoList {
    @Key("list")
    public List<ListInfo> lists;

}
