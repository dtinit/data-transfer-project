package org.dataportabilityproject.spi.transfer.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.dataportabilityproject.types.transfer.models.DataModel;
import org.dataportabilityproject.types.transfer.models.photos.PhotoAlbum;

import java.util.HashMap;
import java.util.Map;

@JsonTypeName("org.dataportability:TempPhotosData")
public class TempPhotosData extends DataModel {

  @JsonProperty("jobId")
  private final String jobId;

  @JsonProperty("album")
  private final Map<String, PhotoAlbum> photoAlbums;

  @JsonProperty("newAlbumIds")
  private final Map<String, String> newAlbumIds;

  public TempPhotosData(
      @JsonProperty("jobId") String jobId,
      @JsonProperty("albums") Map<String, PhotoAlbum> photoAlbums,
      @JsonProperty("newAlbumIds") Map<String, String> newAlbumIds) {
    this.jobId = jobId;
    this.photoAlbums = photoAlbums;
    this.newAlbumIds = newAlbumIds;
  }

  public TempPhotosData(@JsonProperty("jobId") String jobId) {
    this.jobId = jobId;
    this.photoAlbums = new HashMap<>();
    this.newAlbumIds = new HashMap<>();
  }

  public void addAlbum(String key, PhotoAlbum album) {
    photoAlbums.put(key, album);
  }

  public PhotoAlbum lookupAlbum(String key) {
    return photoAlbums.getOrDefault(key, null);
  }

  public void addAlbumId(String oldAlbumId, String newAlbumId){
    newAlbumIds.put(oldAlbumId, newAlbumId);
  }

  public String lookupNewAlbumId(String oldAlbumId){
    return newAlbumIds.getOrDefault(oldAlbumId, "");
  }
}
