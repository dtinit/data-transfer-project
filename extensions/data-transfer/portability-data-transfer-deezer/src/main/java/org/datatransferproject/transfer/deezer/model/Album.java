package org.datatransferproject.transfer.deezer.model;

/**
 * POJO of: https://developers.deezer.com/api/album
 */
public class Album {
  private long id;
  private String title;
  private String upc;
  private String link;

  public long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getUpc() {
    return upc;
  }

  public String getLink() {
    return link;
  }

  @Override
  public String toString() {
    return String.format("Album{id=%s, title=%s}", id, title);
  }
}
