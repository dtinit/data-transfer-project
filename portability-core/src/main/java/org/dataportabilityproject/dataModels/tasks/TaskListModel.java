package org.dataportabilityproject.dataModels.tasks;

import java.io.Serializable;

public class TaskListModel implements Serializable {
    private final String id;
    private final String name;

    public TaskListModel(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }
}
