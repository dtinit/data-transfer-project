package org.datatransferproject.datatransfer.generic;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.datatransferproject.types.common.models.calendar.CalendarAttendeeModel;
import org.datatransferproject.types.common.models.calendar.CalendarContainerResource;
import org.datatransferproject.types.common.models.calendar.CalendarEventModel;
import org.datatransferproject.types.common.models.calendar.CalendarModel;
import org.datatransferproject.types.common.models.calendar.RecurrenceRule;

class CalendarExportData extends CalendarModel implements CalendarSerializer.ExportData {
  private CalendarExportData(String id, String name, String description) {
    super(id, name, description);
  }

  public static CalendarExportData fromModel(CalendarModel model) {
    return new CalendarExportData(model.getId(), model.getName(), model.getDescription());
  }
}

class CalendarEventExportData extends CalendarEventModel implements CalendarSerializer.ExportData {

  private CalendarEventExportData(
      String calendarId,
      String title,
      String notes,
      List<CalendarAttendeeModel> attendees,
      String location,
      CalendarEventTime startTime,
      CalendarEventTime endTime,
      RecurrenceRule recurrenceRule) {
    super(calendarId, title, notes, attendees, location, startTime, endTime, recurrenceRule);
  }

  public static CalendarEventExportData fromModel(CalendarEventModel model) {
    return new CalendarEventExportData(
        model.getCalendarId(),
        model.getTitle(),
        model.getNotes(),
        model.getAttendees(),
        model.getLocation(),
        model.getStartTime(),
        model.getEndTime(),
        model.getRecurrenceRule());
  }
}

public class CalendarSerializer {

  @JsonSubTypes({
    @JsonSubTypes.Type(value = CalendarExportData.class, name = "Calendar"),
    @JsonSubTypes.Type(value = CalendarEventExportData.class, name = "CalendarEvent"),
  })
  public interface ExportData {}

  static final String SCHEMA_SOURCE_CALENDAR =
      GenericTransferConstants.SCHEMA_SOURCE_BASE
          + "/portability-types-common/src/main/java/org/datatransferproject/types/common/models/calendar/CalendarModel.java";
  static final String SCHEMA_SOURCE_EVENT =
      GenericTransferConstants.SCHEMA_SOURCE_BASE
          + "/portability-types-common/src/main/java/org/datatransferproject/types/common/models/calendar/CalendarEventModel.java";

  public static Iterable<ImportableData<ExportData>> serialize(
      CalendarContainerResource container) {
    return Stream.concat(
            container.getCalendars().stream()
                .map(
                    calendar ->
                        new ImportableData<>(
                            new GenericPayload<ExportData>(
                                CalendarExportData.fromModel(calendar), SCHEMA_SOURCE_CALENDAR),
                            calendar.getId(),
                            calendar.getName())),
            container.getEvents().stream()
                .map(
                    event ->
                        new ImportableData<>(
                            new GenericPayload<ExportData>(
                                CalendarEventExportData.fromModel(event), SCHEMA_SOURCE_EVENT),
                            String.valueOf(event.hashCode()),
                            event.getTitle())))
        .collect(Collectors.toList());
  }
}
