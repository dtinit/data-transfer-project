package org.dataportabilityproject.dataModels;

import org.dataportabilityproject.dataModels.calendar.CalendarModel;
import org.dataportabilityproject.dataModels.photos.Photo;
import org.dataportabilityproject.dataModels.tasks.TaskList;
import org.dataportabilityproject.shared.PortableDataType;

public final class DataModelRegistry {
    public static Class<? extends DataModel> getClass(PortableDataType portableDataType) {
        switch (portableDataType) {
            case CALENDAR:
                return CalendarModel.class;
            case PHOTOS:
                return Photo.class;
            case TASKS:
                return TaskList.class;
            default:
                throw new IllegalArgumentException("Don't know what class to use for: " + portableDataType);
        }
    }
}
