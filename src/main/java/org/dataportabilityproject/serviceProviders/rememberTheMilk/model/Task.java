package org.dataportabilityproject.serviceProviders.rememberTheMilk.model;

import com.google.api.client.util.Key;

/**
 * Represents a single Task
 */
public class Task {
    @Key("@id")
    public int id;

    @Key("@due")
    public String due;

    @Key("@has_due_time")
    public boolean has_due_time;

    @Key("@added")
    public String added;

    @Key("@completed")
    public String completed;

    @Key("@deleted")
    public String deleted;

    @Key("@priority")
    public String priority;

    @Key("@postponed")
    public boolean postponed;

    @Key("@estimate")
    public String estimate;

    @Override
    public String toString() {
        return String.format("Task(id=%d due=%s has_due_time=%s added=%s completed=%s deleted=%s priority=%s postponed=%s",
                id, due, has_due_time, added, completed, deleted, priority, postponed);
    }
}
