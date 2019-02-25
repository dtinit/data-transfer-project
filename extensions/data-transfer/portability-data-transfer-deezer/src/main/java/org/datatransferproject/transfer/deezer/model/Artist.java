package org.datatransferproject.transfer.deezer.model;

/**
 * POJO of: https://developers.deezer.com/api/artist
 */
public class Artist {
  private long id;
  private String name;
  private String link;

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getLink() {
    return link;
  }

  @Override
  public String toString(){
    return String.format("Artist{id=%s, name=%s, link=%s}",
        id, name, link);
  }
}
