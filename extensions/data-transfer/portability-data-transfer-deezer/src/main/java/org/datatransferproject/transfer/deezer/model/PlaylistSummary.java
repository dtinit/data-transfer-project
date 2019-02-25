package org.datatransferproject.transfer.deezer.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POJO of: https://developers.deezer.com/api/playlist
 */
public class PlaylistSummary {
  protected long id;
  protected String title;
  protected String description;
  @JsonProperty("pulbic") protected boolean isPublic;
  protected String link;

  public long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public boolean isPublic() {
    return isPublic;
  }

  public String getLink() {
    return link;
  }

  public String toString() {
    return String.format(
        "PlaylistSummary{id=%s, title=\"%s\", description=\"%s\", public=%s, link=%s}",
        id,
        title,
        description,
        isPublic,
        link);
  }
}
