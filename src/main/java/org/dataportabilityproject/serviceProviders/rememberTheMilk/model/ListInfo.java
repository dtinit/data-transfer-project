package org.dataportabilityproject.serviceProviders.rememberTheMilk.model;

import com.google.api.client.util.Key;

public class ListInfo {
    @Key("@id")
    public int id;

    @Key("@name")
    public String name;

    @Key("@deleted")
    public boolean deleted;

    @Key("@locked")
    public boolean locked;

    @Key("@archived")
    public boolean archived;

    @Key("@position")
    public int position;

    @Key("@smart")
    public boolean smart;

    @Override
    public String toString() {
        return String.format("List(id=%d, name=%s)", id, name);
    }
}

