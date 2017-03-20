package org.dataportabilityproject.dataModels.tasks;

import com.google.common.collect.ImmutableList;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.shared.PortableDataType;

import java.util.List;

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

    @Override public PortableDataType getDataType() {
        return PortableDataType.TASKS;
    }
}
