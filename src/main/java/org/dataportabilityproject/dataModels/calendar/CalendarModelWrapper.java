package org.dataportabilityproject.dataModels.calendar;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.dataportabilityproject.dataModels.ContinuationInformation;
import org.dataportabilityproject.dataModels.DataModel;

/**
 * A Wrapper for all the possible objects that can be returned by a calendar exporter.
 */
public class CalendarModelWrapper implements DataModel {
  private final Collection<CalendarModel> calendars;
  private final Collection<CalendarEventModel> events;
  private final ContinuationInformation continuationInformation;

  public CalendarModelWrapper(
      Collection<CalendarModel> calendars,
      Collection<CalendarEventModel> events,
      ContinuationInformation continuationInformation) {
    this.calendars = calendars == null ? ImmutableList.of() : calendars;
    this.events = events == null ? ImmutableList.of() : events;
    this.continuationInformation = continuationInformation;
  }
  public Collection<CalendarModel> getCalendars() {
    return calendars;
  }

  public Collection<CalendarEventModel> getEvents() {
    return events;
  }

  @Override public ContinuationInformation getContinuationInformation() {
    return continuationInformation;
  }
}
