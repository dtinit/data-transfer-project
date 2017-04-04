package org.dataportabilityproject.dataModels.calendar;

public class CalendarModel {
    private final String id;
    private final String name;
    private final String description;

    public CalendarModel(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getId() {
        return id;
    }
}
