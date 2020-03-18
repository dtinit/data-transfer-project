package org.datatransferproject.transfer.smugmug.photos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.MoreObjects;
import org.datatransferproject.transfer.smugmug.photos.model.SmugMugAlbum;
import org.datatransferproject.types.common.models.DataModel;
import java.io.Serializable;

@JsonTypeName("org.dataportability:SmugMugPhotoTempData")
public class SmugMugPhotoTempData extends DataModel implements Serializable {
  private final String albumId;
  private final SmugMugAlbum apiAlbum;
  private int photoCount;
  private String overflowAlbumId;

  @JsonCreator
  public SmugMugPhotoTempData(@JsonProperty("albumId") String albumId, @JsonProperty("album") SmugMugAlbum apiAlbum) {
    this.albumId = albumId;
    this.apiAlbum = apiAlbum;
    this.photoCount = 0;
    this.overflowAlbumId = null;
  }

  public String getAlbumUri() {
    return this.apiAlbum.getUri();
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

  public SmugMugAlbum getApiAlbum() {
    return this.apiAlbum;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("albumId", albumId)
        .add("photoCount", photoCount)
        .add("overflowAlbumId", overflowAlbumId)
        .add("apiAlbum", apiAlbum)
        .toString();
  }
}
