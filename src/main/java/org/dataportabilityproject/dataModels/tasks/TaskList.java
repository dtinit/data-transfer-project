package org.dataportabilityproject.dataModels.tasks;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.dataportabilityproject.dataModels.DataModel;

public class TaskList implements DataModel {
    private final String name;
    private final ImmutableList<Task> tasks;

    public TaskList(String name, ImmutableList<Task> tasks) {
        this.name = name;
        this.tasks = tasks;
    }

    public String getName() {
        return name;
    }


    public List<Task> getTasks() {
        return tasks;
    }
}
