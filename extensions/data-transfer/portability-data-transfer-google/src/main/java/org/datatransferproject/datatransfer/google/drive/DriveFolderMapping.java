package org.datatransferproject.datatransfer.google.drive;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.datatransferproject.types.common.models.DataModel;

@JsonTypeName("org.dataportability:GoogleDriveFolderMapping")
class DriveFolderMapping extends DataModel {

  private final String oldId;
  private final String newId;

  @JsonCreator
  DriveFolderMapping(
      @JsonProperty("oldId") String oldId,
      @JsonProperty("newId") String newId) {

    this.oldId = oldId;
    this.newId = newId;
  }

  public String getNewId() {
    return newId;
  }

  public String getOldId() {
    return oldId;
  }
}
