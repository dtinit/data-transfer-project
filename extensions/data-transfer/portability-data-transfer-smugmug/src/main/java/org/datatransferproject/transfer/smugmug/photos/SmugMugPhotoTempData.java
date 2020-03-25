package org.datatransferproject.transfer.smugmug.photos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.MoreObjects;
import java.io.Serializable;
import org.datatransferproject.types.common.models.DataModel;

@JsonTypeName("org.dataportability:SmugMugPhotoTempData")
public class SmugMugPhotoTempData extends DataModel implements Serializable {
  private final String albumExportId;
  private final String albumName;
  private final String albumDescription;
  private final String albumUri;
  private int photoCount;
  private String overflowAlbumExportId;

  @JsonCreator
  public SmugMugPhotoTempData(
      @JsonProperty("albumExportId") String albumExportId,
      @JsonProperty("albumName") String albumName,
      @JsonProperty("albumDescription") String albumDescription,
      @JsonProperty("albumUri") String albumUri) {
    this.albumExportId = albumExportId;
    this.albumName = albumName;
    this.albumDescription = albumDescription;
    this.albumUri = albumUri;
    this.photoCount = 0;
    this.overflowAlbumExportId = null;
  }

  public String getAlbumExportId() {
    return this.albumExportId;
  }

  public int incrementPhotoCount() {
    return this.photoCount++;
  }

  public int getPhotoCount() {
    return this.photoCount;
  }

  public void setOverflowAlbumExportId(String overflowAlbumExportId) {
    this.overflowAlbumExportId = overflowAlbumExportId;
  }

  public String getOverflowAlbumExportId() {
    return this.overflowAlbumExportId;
  }

  public String getAlbumDescription() {
    return albumDescription;
  }

  public String getAlbumName() {
    return albumName;
  }

  public String getAlbumUri() {
    return albumUri;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("albumExportId", albumExportId)
        .add("photoCount", photoCount)
        .add("overflowAlbumExportId", overflowAlbumExportId)
        .add("albumName", albumName)
        .add("albumDescription", albumDescription)
        .toString();
  }
}
