package org.datatransferproject.transfer.smugmug.photos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.MoreObjects;
import org.datatransferproject.types.common.models.DataModel;

@JsonTypeName("org.dataportability:SmugMugPhotoTempData")
public class SmugMugPhotoTempData extends DataModel {
  private final String albumUri;
  private int photoCount;
  private String overflowAlbumUri;

  @JsonCreator
  public SmugMugPhotoTempData(@JsonProperty("albumUri") String albumUri) {
    this.albumUri = albumUri;
    this.photoCount = 0;
    this.overflowAlbumUri = null;
  }

  public String getAlbumUri() {
    return this.albumUri;
  }

  public int incrementPhotoCount() {
    return this.photoCount++;
  }

  public int getPhotoCount() {
    return this.photoCount;
  }

  public void setOverflowAlbumUri(String overflowAlbumUri) {
    this.overflowAlbumUri = overflowAlbumUri;
  }

  public String getOverflowAlbumUri() {
    return this.overflowAlbumUri;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("albumUri", albumUri)
        .add("photoCount", photoCount)
        .add("overflowAlbumUri", overflowAlbumUri)
        .toString();
  }
}
