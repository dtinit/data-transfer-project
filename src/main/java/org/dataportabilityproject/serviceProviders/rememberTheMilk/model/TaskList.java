package org.dataportabilityproject.serviceProviders.rememberTheMilk.model;

import com.google.api.client.util.Key;
import com.google.common.base.Joiner;

import java.util.List;

/**
 * A list of one or more {@link Task} contained in a {@link TaskSeries}.
 */
public class TaskList {
    @Key("@id")
    public int id;

    @Key("taskseries")
    public List<TaskSeries> taskSeriesList;

    @Override
    public String toString() {
        return String.format("(list id=%d children:[%s])", id,
                (taskSeriesList == null || taskSeriesList.isEmpty()) ? ""
                    : Joiner.on("\n").join(taskSeriesList));
    }
}
