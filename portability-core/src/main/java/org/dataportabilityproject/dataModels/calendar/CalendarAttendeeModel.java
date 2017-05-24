package org.dataportabilityproject.dataModels.calendar;


public class CalendarAttendeeModel {
    private final String displayName;
    private final String email;
    private final boolean optional;

    public CalendarAttendeeModel(String displayName, String email, boolean optional) {
        this.displayName = displayName;
        this.email = email;
        this.optional = optional;
    }

    public boolean isOptional() {
        return optional;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }
}
