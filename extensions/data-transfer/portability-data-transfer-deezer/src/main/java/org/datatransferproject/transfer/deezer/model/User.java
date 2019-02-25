package org.datatransferproject.transfer.deezer.model;

/**
 * POJO of https://developers.deezer.com/api/user
 */
public class User {
  private long id;
  private String name;
  private String tracklist;

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getTracklist() {
    return tracklist;
  }

  @Override
  public String toString() {
    return String.format("User{id=%s, name=%s, tracklist=%s}", id, name, tracklist);
  }
}
