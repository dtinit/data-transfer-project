package org.datatransferproject.transfer.deezer.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POJO of track: https://developers.deezer.com/api/track
 */
public class Track {
  private long id;
  private boolean readable;
  private String title;
  @JsonProperty("short_title") private String shortTitle;
  @JsonProperty("short_version") private String titleVersion;
  private String isrc;
  private String link;
  private int duration;
  @JsonProperty("track_position") private int trackPosition;
  @JsonProperty("disk_position") private int diskPosition;
  private Artist artist;
  private Album album;

  public long getId() {
    return id;
  }

  public boolean isReadable() {
    return readable;
  }

  public String getTitle() {
    return title;
  }

  public String getShortTitle() {
    return shortTitle;
  }

  public String getTitleVersion() {
    return titleVersion;
  }

  public String getIsrc() {
    return isrc;
  }

  public String getLink() {
    return link;
  }

  public int getDuration() {
    return duration;
  }

  public int getTrackPosition() {
    return trackPosition;
  }

  public int getDiskPosition() {
    return diskPosition;
  }

  public Artist getArtist() {
    return artist;
  }

  public Album getAlbum() {
    return album;
  }

  @Override
  public String toString() {
    return String.format("Track{id=%s, title=\"%s\", isrc=%s}",
        id,
        title,
        isrc);
  }
}
