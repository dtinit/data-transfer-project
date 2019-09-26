package org.datatransferproject.datatransfer.flickr.photos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.datatransferproject.types.common.models.DataModel;

@JsonTypeName("org.dataportability:FlickrTempPhotoData")
public class FlickrTempPhotoData extends DataModel {
  private final String name;
  private final String description;

  @JsonCreator
  public FlickrTempPhotoData(
      @JsonProperty("name") String name, @JsonProperty("description") String description) {
    this.name = name;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }
}
