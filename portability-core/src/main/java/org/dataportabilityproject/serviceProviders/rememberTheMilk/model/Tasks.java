package org.dataportabilityproject.serviceProviders.rememberTheMilk.model;

import com.google.api.client.util.Key;
import com.google.common.base.Joiner;

import java.util.List;

/**
 * A set of task lists.
 */
public class Tasks {
    @Key("@rev")
    public String rev;

    @Key("list")
    public List<TaskList> list;

    @Override
    public String toString() {
        return String.format("Tasks(rev=%s List=%s)",
                rev,
                (null == list || list.isEmpty()) ? "" : Joiner.on("\n").join(list));
    }
}
