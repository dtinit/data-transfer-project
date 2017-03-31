package org.dataportabilityproject.dataModels.calendar;

import java.util.Collection;
import org.dataportabilityproject.dataModels.DataModel;

public class CalendarModel implements DataModel {
    private final String name;
    private final String description;
    private final Collection<CalendarEventModel> events;

    public CalendarModel(String name, String description, Collection<CalendarEventModel> events) {
        this.name = name;
        this.description = description;
        this.events = events;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Collection<CalendarEventModel> getEvents() {
        return events;
    }
}
