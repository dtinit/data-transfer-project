package org.dataportabilityproject.dataModels.tasks;

public class Task {
    private final String text;
    private final String notes;

    public Task(String text, String notes) {
        this.text = text;
        this.notes = notes;
    }

    public String getText() {
        return text;
    }

    public String getNotes() {
        return notes;
    }
}
