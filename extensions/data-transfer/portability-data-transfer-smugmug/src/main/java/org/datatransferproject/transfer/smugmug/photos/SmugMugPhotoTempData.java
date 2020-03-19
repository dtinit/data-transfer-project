package org.datatransferproject.transfer.smugmug.photos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.MoreObjects;
import java.io.Serializable;
import org.datatransferproject.types.common.models.DataModel;

@JsonTypeName("org.dataportability:SmugMugPhotoTempData")
public class SmugMugPhotoTempData extends DataModel implements Serializable {
  private final String albumId;
  private final String albumName;
  private final String albumDescription;
  private int photoCount;
  private String overflowAlbumId;

  @JsonCreator
  public SmugMugPhotoTempData(
      @JsonProperty("albumId") String albumId,
      @JsonProperty("albumName") String albumName,
      @JsonProperty("albumDescription") String albumDescription) {
    this.albumId = albumId;
    this.albumName = albumName;
    this.albumDescription = albumDescription;
    this.photoCount = 0;
    this.overflowAlbumId = null;
  }

  public String getAlbumId() {
    return this.albumId;
  }

  public int incrementPhotoCount() {
    return this.photoCount++;
  }

  public int getPhotoCount() {
    return this.photoCount;
  }

  public void setOverflowAlbumId(String overflowAlbumId) {
    this.overflowAlbumId = overflowAlbumId;
  }

  public String getOverflowAlbumId() {
    return this.overflowAlbumId;
  }

  public String getAlbumDescription() {
    return albumDescription;
  }

  public String getAlbumName() {
    return albumName;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("albumId", albumId)
        .add("photoCount", photoCount)
        .add("overflowAlbumId", overflowAlbumId)
        .add("albumName", albumName)
        .add("albumDescription", albumDescription)
        .toString();
  }
}
