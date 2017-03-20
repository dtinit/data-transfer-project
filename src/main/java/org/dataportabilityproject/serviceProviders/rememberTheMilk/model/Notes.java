package org.dataportabilityproject.serviceProviders.rememberTheMilk.model;

import com.google.api.client.util.Key;
import com.google.common.base.Joiner;

import java.util.List;

/**
 * A collection of notes that exist on a task/taskseries
 */
public class Notes {
    @Key("note")
    public List<String> notes;

    @Override
    public String toString() {
        return (notes == null || notes.isEmpty()) ? "" : Joiner.on("; ").join(notes);
    }
}
