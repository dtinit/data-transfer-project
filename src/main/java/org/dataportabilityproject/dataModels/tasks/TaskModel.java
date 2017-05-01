package org.dataportabilityproject.dataModels.tasks;

public class TaskModel {
    private final String taskListId;
    private final String text;
    private final String notes;

    public TaskModel(String taskListId, String text, String notes) {
        this.taskListId = taskListId;
        this.text = text;
        this.notes = notes;
    }

    public String getText() {
        return text;
    }

    public String getNotes() {
        return notes;
    }

    public String getTaskListId() {
        return taskListId;
    }
}
