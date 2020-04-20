package org.datatransferproject.transfer.smugmug.photos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.MoreObjects;
import com.google.common.annotations.VisibleForTesting;
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

  public SmugMugPhotoTempData(
      @JsonProperty("albumExportId") String albumExportId,
      @JsonProperty("albumName") String albumName,
      @JsonProperty("albumDescription") String albumDescription,
      @JsonProperty("albumUri") String albumUri) {
    this(albumExportId, albumName, albumDescription, albumUri, 0, null);
  }

  @VisibleForTesting
  @JsonCreator
  public SmugMugPhotoTempData(
      @JsonProperty("albumExportId") String albumExportId,
      @JsonProperty("albumName") String albumName,
      @JsonProperty("albumDescription") String albumDescription,
      @JsonProperty("albumUri") String albumUri,
      @JsonProperty("photoCount") int photoCount,
      @JsonProperty("overflowAlbumExportId") String overflowAlbumExportId) {
    this.albumExportId = albumExportId;
    this.albumName = albumName;
    this.albumDescription = albumDescription;
    this.albumUri = albumUri;
    this.photoCount = photoCount;
    this.overflowAlbumExportId = overflowAlbumExportId;
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
        .add("albumUri", albumUri)
        .toString();
  }
}
