package org.datatransferproject.datatransfer.generic;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.datatransferproject.types.common.models.calendar.CalendarContainerResource;

public class CalendarSerializer {
  static final String SCHEMA_SOURCE_CALENDAR =
      GenericTransferConstants.SCHEMA_SOURCE_BASE
          + "/portability-types-common/src/main/java/org/datatransferproject/types/common/models/calendar/CalendarModel.java";
  static final String SCHEMA_SOURCE_EVENT =
      GenericTransferConstants.SCHEMA_SOURCE_BASE
          + "/portability-types-common/src/main/java/org/datatransferproject/types/common/models/calendar/CalendarEventModel.java";

  public static Iterable<ImportableData> serialize(
      CalendarContainerResource container, ObjectMapper objectMapper) {
    return Stream.concat(
            container.getCalendars().stream()
                .map(
                    calendar ->
                        new ImportableData(
                            objectMapper.valueToTree(
                                new GenericPayload<>(calendar, SCHEMA_SOURCE_CALENDAR)),
                            calendar.getId(),
                            calendar.getName())),
            container.getEvents().stream()
                .map(
                    event ->
                        new ImportableData(
                            objectMapper.valueToTree(
                                new GenericPayload<>(event, SCHEMA_SOURCE_EVENT)),
                            String.valueOf(event.hashCode()),
                            event.getTitle())))
        .collect(Collectors.toList());
  }
}
