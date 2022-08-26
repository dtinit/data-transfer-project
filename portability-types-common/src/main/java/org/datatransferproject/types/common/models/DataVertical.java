package org.datatransferproject.types.common.models;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.HashMap;
import java.util.Map;


/**
 * Represents the data type of a transfer job
 */
public enum DataVertical {
  // TODO consider if we can reference data models directly rather than enums; for now beware
  //  that each item in this enum is represented by one and only one common data model.
  BLOBS("BLOBS"),
  CALENDAR("CALENDAR"),
  CONTACTS("CONTACTS"),
  MAIL("MAIL"),
  MEDIA("MEDIA"),
  NOTES("NOTES"),
  OFFLINE_DATA("OFFLINE-DATA"),
  PHOTOS("PHOTOS"),
  PLAYLISTS("PLAYLISTS"),
  SOCIAL_POSTS("SOCIAL-POSTS"),
  TASKS("TASKS"),
  VIDEOS("VIDEOS");

  private final String dataType;

  DataVertical(String dataType) {
    this.dataType = dataType;
  }

  @JsonValue
  public String getDataType() {
    return dataType;
  }

  private static final Map<String, DataVertical> BY_TYPE = new HashMap<>();

  static {
    for (DataVertical e : values()) {
      BY_TYPE.put(e.dataType, e);
    }
  }

  public static DataVertical fromDataType(String dataType) {
    return BY_TYPE.get(dataType);
  }
}
